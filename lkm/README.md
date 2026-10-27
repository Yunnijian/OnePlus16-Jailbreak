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
| `build-sukisu.sh` | SukiSU Ultra v4.2.1-r2（41068） | 535,328 B |
| `build-susfs.sh` | SUSFS guard LKM（vendored） | 3,342,896 B（未 strip） |

vermagic 均为 `6.18.21-4k SMP preempt mod_unload modversions aarch64`。

## SUSFS guard LKM（实验）

源码 vendored 在 [susfs4ksu-lkm](susfs4ksu-lkm/)（upstream `inforcqb/susfs4ksu-lkm` commit `b69dee4`，GPL-2.0）。
为 PYB110 的 `CONFIG_ARM64_BTI_KERNEL=y` 做了两处改动，详见 [MODIFICATIONS.md](susfs4ksu-lkm/MODIFICATIONS.md)：

- supercall：`__arm64_sys_reboot` 的 kprobe 换成 `sys_call_table[__NR_reboot]` 模块 wrapper
- hide_syms：`s_show`/`m_show` 的 kprobe 换成 `seq_operations.show` wrapper（调用原函数后回滚 `seq_file->count`）

`bash lkm/build-susfs.sh`（容器内）产出 `susfs_guard_lkm-6.18.21.ko`。加载：

```sh
ksud insmod susfs_guard_lkm-6.18.21.ko expose_proc=1
```

**已知边界**：`sus_map` 仍使用 `show_map`/`show_smap`/`show_map_vma` 的 kprobe；在 BTI 内核上一加 map 规则、读 `/proc/<pid>/maps` 就会 `Oops - BTI` 重启。未把这三处也改成 wrapper 之前，不要下发 sus_map 规则；其余规则（sus_path / kstat / open_redirect / hide_modules / uname / opath / 日志开关）已做过真机矩阵。测试工具在 [susfs-tools](susfs-tools/)。

## 注意

- 不能只改 vermagic 复用其它 minor 版本的官方 ko；ABI/CFI 不匹配会在加载后崩溃。
- 每次设备重启 KASLR slide 变化，ko 必须在设备端用 `prep_patch` 按当次 slide 重新写符号。
- 模块加载参数只需 `allow_shell=1`；官方 / Next / BakaSU 都定义了该参数。
- 管理器与 ksud 建议直接用官方 release / CI 产物，不要本地重复编译（管理器 debug 签名还会与旧 release 签名冲突）。
