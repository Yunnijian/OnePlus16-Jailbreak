# 修改说明

上游：[`inforcqb/susfs4ksu-lkm`](https://github.com/inforcqb/susfs4ksu-lkm)（GPL-2.0，见 `LICENSE`）。基线 `b69dee4`，2026-10-27 已 rebase 到 `28b2e85`（`sus_map: expose rules through /proc/susfs_map`）。

PYB110 内核开启 `CONFIG_ARM64_BTI_KERNEL=y`。kprobe 会把函数入口的 `bti c` 替换成断点，而 `invoke_syscall` / `seq_read` / `show_map` 通过函数指针间接跳转时会先做 BTI 检查，结果就是 `Oops - BTI`。本目录改了三个文件：

## kernel/susfs_supercall.c

- 去掉 `__arm64_sys_reboot` 上的 kprobe。
- 改用 `sys_call_table[__NR_reboot]` 的模块 wrapper：直接读 `regs->regs[0..3]`（syscall 表 wrapper 拿到的就是 `struct pt_regs *`，不需要 kprobe 的 `real_regs` 间接层），匹配 magic 后把命令交给 task_work 并返回 0；其余情况调用保存的原 syscall 函数。
- 卸载时通过 `susfs_fp_remove()` + `susfs_fp_drain()` 恢复表项。

## kernel/susfs_hide_syms.c

- 去掉 `s_show` / `m_show` 上的 kprobe。
- 解析 `kallsyms_op.show` / `modules_op.show` 后，用 `ksu_patch_text()` 换成模块 wrapper；wrapper 先调用原 show，命中隐藏规则时把 `seq_file->count` 回滚到调用前，达到“这一行不输出”的效果。
- 旧的 kprobe 结构体保留为 `__maybe_unused`，仅作为参考，不再注册。

## kernel/sus_map.c

- 去掉 `show_map` / `show_smap` 上的 kprobe（它们是 `/proc/<pid>/maps` / `smaps` 的 `seq_operations.show`，被 `seq_read` 间接调用）。
- 解析 `proc_pid_maps_op` / `proc_pid_smaps_op`，用 `ksu_patch_text()` 换成模块 wrapper；wrapper 先调用原 show，命中规则时回滚 `seq_file->count`。
- `show_map_vma` 是 `show_map`/`show_smap` 直接调用的内层函数，保留 kprobe，BTI 检查不经过它。
- 与上游 `28b2e85` 的 `/proc/susfs_map` 合并：两处外层 kprobe 仍跳过，wrapper 改在 `sus_map_register_probes()` 里与新的 `/proc` 节点一起 arm；`map_registered` 与 smaps 告警改看 wrapper 的 arm 状态。

## 验证

- 6.18.21 编译：vermagic `6.18.21-4k SMP preempt mod_unload modversions aarch64`；合并版 3,373,024 B，sha256 `c28d54130041a255d56e68fa2c02d2750ed25feb390b3b95fabe3334f1eada9f`。
- 真机：SUSFS 版本 `v2.3.0`，`susfs supercall: armed via sys_call_table`；`/proc/kallsyms` / `/proc/modules` 的 susfs/ksu 行被隐藏；`/proc/susfs_*` 规则节点正常；普通 `reboot(2)` 走原函数返回 `EINVAL`，无 BTI。
- sus_map：app 进程映射测试文件后读 `/proc/self/maps` 与 `/proc/self/smaps`，两处映射行都被隐藏（dmesg `hid show_map` / `hid show_smap`），无 BTI。
- 规则矩阵：sus_path / kstat / open_redirect / hide_modules / sus_map / uname / opath / enable_log / avc_spoof 通过；详见 `../susfs-tools/README.md`。
- 合并版真机复核（2026-10-27）：热替换加载正常、版本握手 v2.3.0；`/proc/susfs_map` 规则对 uid 10000 隐藏 maps/smaps；打开 BakaSU 管理器无 BTI/Oops。
- show wrapper 加固（review 后）：saved original 先于 wrapper 指针发布并 `smp_wmb()`；卸载恢复后 `susfs_fp_drain()` 再清指针；隐藏判定前移，去掉 `seq_file->count` 回滚。真机复核：加载/规则/`rmmod` 正常。

## 已知问题

- `hide_mounts` 的任意 bind mount 不会被标记（`recorded=0`）；实际挂载隐藏未验证。
- `hide_modules` 的 `/sys/module/<name>` 对 root 也会隐藏：当前 SukiSU 的 `u:r:ksu:s0` 未被识别为 `su_ctx`。
- `sus_map` 只对 `susfs_is_current_proc_umounted_app()` 为真的 app 生效；`su` 授予 root 的进程不隐藏（测试时用 uid 10000 的 `su 10000` 可命中）。
