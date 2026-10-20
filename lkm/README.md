# LKM 构建

设备内核：`6.18.21-android17-5-gf957accaf563-ab15926176-4k`。

## 环境

- GKI 源：`android17-6.18-2026-06_r9`（SUBLEVEL=21）
- KDIR：`gki_defconfig` + `modules_prepare` + `security/selinux/built-in.a`
- DDK：`ghcr.io/ylarod/ddk-min:android17-6.18-20260828`（clang r584948c）

四个脚本都假定在 DDK 容器内、KMI 源与 KDIR 已挂载：

```bash
docker run --rm \
  -v <gki-6.18.21>:/work/gki-6.18.21 \
  -v <kdir>:/work/kdir \
  -v <kernelsu-source>:/work/ksun \
  -v <bakasu-source>:/work/resukisu \
  -v <out>:/out \
  ghcr.io/ylarod/ddk-min:android17-6.18-20260828 \
  bash /work/build-next.sh
```

## 产物

| 脚本 | 产物 | strip 后 |
| --- | --- | --- |
| `build-kernelsu.sh` | 官方 KernelSU | 348,848 B |
| `build-next.sh` | KernelSU Next | 350,000 B |
| `build-bakasu.sh` | BakaSU | 385,440 B |
| `build-sukisu.sh` | SukiSU Ultra (v4.2.1) | 376,136 B |

vermagic 均为 `6.18.21-4k SMP preempt mod_unload modversions aarch64`。

## 注意

- 不能只改 vermagic 复用其它 minor 版本的官方 ko；ABI/CFI 不匹配会在加载后崩溃。
- 每次设备重启 KASLR slide 变化，ko 必须在设备端用 `prep_patch` 按当次 slide 重新写符号。
- 模块加载参数只需 `allow_shell=1`；官方 / Next / BakaSU 都定义了该参数。
- 管理器与 ksud 建议直接用官方 release / CI 产物，不要本地重复编译（管理器 debug 签名还会与旧 release 签名冲突）。
