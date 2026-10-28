# LKM 构建

设备内核：`6.18.21-android17-5-gf957accaf563-ab15926176-4k`。

## 环境

- GKI 源：`android17-6.18-2026-06_r9`（SUBLEVEL=21）
- KDIR：`gki_defconfig` + `modules_prepare` + `security/selinux/built-in.a`
- DDK：`ghcr.io/ylarod/ddk-min:android17-6.18-20260828`（clang r584948c）

五个脚本都假定在 DDK 容器内、KMI 源与 KDIR 已挂载：

```bash
docker run --rm \
  -v <gki-6.18.21>:/work/gki-6.18.21 \
  -v <kdir>:/work/kdir \
  -v <kernelsu-source>:/work/ksun \
  -v <bakasu-source>:/work/resukisu \
  -v <repo>/lkm/susfs4ksu-lkm:/work/susfs4ksu-lkm \
  -v <out>:/out \
  ghcr.io/ylarod/ddk-min:android17-6.18-20260828 \
  bash /work/build-next.sh
```

## 产物

| 脚本 | 产物 | strip 后 |
| --- | --- | --- |
| `build-kernelsu.sh` | 官方 KernelSU v3.3.1 | 349,224 B |
| `build-next.sh` | KernelSU Next | 350,000 B |
| `build-bakasu.sh` | BakaSU 35252（v4.2.0-rc3） | 435,616 B |
| `build-sukisu.sh` | SukiSU Ultra 41097（HEAD 080ba098） | 544,848 B |
| `build-susfs.sh` | SUSFS guard LKM（vendored） | 3,372,960 B（未 strip） |

vermagic 均为 `6.18.21-4k SMP preempt mod_unload modversions aarch64`。

## SUSFS guard LKM（实验）

源码 vendored 在 [susfs4ksu-lkm](susfs4ksu-lkm/)（upstream `inforcqb/susfs4ksu-lkm`，基线 `b69dee4`，2026-10-27 rebase 到 `28b2e85`，GPL-2.0）。
为 PYB110 的 `CONFIG_ARM64_BTI_KERNEL=y` 做了三处改动，详见 [MODIFICATIONS.md](susfs4ksu-lkm/MODIFICATIONS.md)：

- supercall：`__arm64_sys_reboot` 的 kprobe 换成 `sys_call_table[__NR_reboot]` 模块 wrapper
- hide_syms：`s_show`/`m_show` 的 kprobe 换成 `seq_operations.show` wrapper（调用原函数后回滚 `seq_file->count`）
- sus_map：`show_map`/`show_smap` 的 kprobe 换成 `proc_pid_maps_op`/`proc_pid_smaps_op.show` wrapper；`show_map_vma` 是直接调用，保留 kprobe

`bash lkm/build-susfs.sh`（容器内）产出 `susfs_guard_lkm-6.18.21.ko`。加载：

```sh
ksud insmod susfs_guard_lkm-6.18.21.ko expose_proc=1
```

**已验证**：版本 `v2.3.0`、supercall、hide_syms、sus_map 的 maps/smaps 行隐藏、sus_path、kstat、open_redirect、hide_modules、uname、opath、日志开关；规则矩阵与工具在 [susfs-tools](susfs-tools/)。

**已知边界**：`sus_map` 只对 `susfs_is_current_proc_umounted_app()` 为真的 app 生效；`su` 授予 root 的进程不隐藏。`hide_mounts` 的任意 bind mount 不会被标记（`recorded=0`），实际挂载隐藏未验证。`hide_modules` 的 `/sys/module/<name>` 对 root 也会隐藏（SukiSU 的 `u:r:ksu:s0` 未被识别为 `su_ctx`）。

## 注意

- 不能只改 vermagic 复用其它 minor 版本的官方 ko；ABI/CFI 不匹配会在加载后崩溃。
- 每次设备重启 KASLR slide 变化，ko 必须在设备端用 `prep_patch` 按当次 slide 重新写符号。
- 模块加载参数只需 `allow_shell=1`；官方 / Next / BakaSU 都定义了该参数。
- 管理器与 ksud 建议直接用官方 release / CI 产物，不要本地重复编译（管理器 debug 签名还会与旧 release 签名冲突）。
