# SUSFS 规则矩阵测试工具

对应源码：[../susfs4ksu-lkm](../susfs4ksu-lkm)。设备端工具都用 NDK 静态编译：

```bash
CC=$NDK/toolchains/llvm/prebuilt/<host>/bin/aarch64-linux-android31-clang
$CC -static -O2 -o susfs_ctl susfs_ctl.c
$CC -static -O2 -o susfs_version susfs_version.c
$CC -static -O2 -o map_probe map_probe.c
adb push susfs_ctl susfs_version map_probe /data/local/tmp/
adb push susfs_matrix2.sh /data/local/tmp/
adb shell 'chmod 755 /data/local/tmp/susfs_ctl /data/local/tmp/susfs_version /data/local/tmp/map_probe /data/local/tmp/susfs_matrix2.sh'
```

先加载模块，再以 root 跑矩阵：

```sh
su -c 'ksud insmod /data/local/tmp/susfs_guard_lkm-6.18.21.ko expose_proc=1'
su -c 'sh /data/local/tmp/susfs_matrix2.sh'
```

## 结果（PYB110 / 6.18.21 / 完整模块）

最近一次真机矩阵：**24 PASS / 0 FAIL / 2 SKIP / 2 NOTE**。

- **PASS**：sus_path（app uid 10000 的 stat/readdir、root 可见、del 恢复）、sus_kstat 静态伪装与删除、open_redirect（scheme 0 非 su/root 重定向、scheme 2 非 su 重定向且 root 不受影响）、hide_modules（/proc/modules、kallsyms、/sys/module、del 恢复）、**sus_map（app 映射测试文件后 maps 与 smaps 行都隐藏，dmesg `hid show_map` / `hid show_smap`，无 BTI）**、enable_log、avc_spoof、opath、uname spoof/restore、hide_mounts 前缀 add/del。
- **NOTE**：`hide_modules` 的 `/sys/module/<name>` 对 root 也隐藏；当前 SukiSU 的 `u:r:ksu:s0` 未被识别为 `su_ctx`，root 通过 `su` 仍正常。`sus_map` 只对 `susfs_is_current_proc_umounted_app()` 为真的 app 生效。
- **SKIP**：`spoof_cmdline`（持久化 `/proc/cmdline`，需要重载模块恢复，未做）；`sus_mount` 的实际挂载隐藏（任意 bind mount 不会被标记，`recorded=0`）。

`susfs_ctl` 的用法：

```sh
susfs_ctl uname <release> <version>
susfs_ctl hide_mounts 0|1
susfs_ctl map <path>
susfs_version
```

`susfs_map` 规则是 add-only，没有 proc 清除命令；测完 map 规则后重载模块即可清空。
