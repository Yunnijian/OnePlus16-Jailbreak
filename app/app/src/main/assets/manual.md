# Shark 使用手册

Shark 通过 Stellar 或 Shizuku 以 shell 身份驱动设备端 98260 exploit，加载管理器 LKM。首次使用先完成授权，再一键越狱。

> Shark 目前支持欧加 6.18.21 内核设备（如一加 16）、欧真加 6.12.69 内核设备，以及单独适配的 vivo V2505A（内核 `6.12.23-android16-5-g41f4cb1fa7c7-abogki465038634-4k`）。V2505A 入口仅对该完整内核版本开放，当前只在 KernelSU 上完成实机验证。

## 快速开始

1. 先安装所选 ROOT 管理器（KernelSU / FinaSU / KernelSU Next / BakaSU / SukiSU Ultra），**暂不支持ci版本的 KernelSU Next**。
2. 启动并使用**无线调试或者adb**激活 **Stellar（推荐）** 或 **Shizuku** 服务，并授予 Shark 权限。
3. 回到 Shark 首页点「一键越狱」，等待成功日志。V2505A 入口以 `DONE`、`SU_ID_RC=0` 和 `uid=0(root)` 为准。
4. 成功后管理器会自动拉起；开启对应开关时还会停用安全弹窗、加载 **SUSFS / VFS**（VFS 开关仅 6.18.21 显示；V2505A 只显示 SUSFS，使用 6.12 版 `libsusfs_guard_lkm_612.so`）。
5. 若日志显示 **MODULE NOT LOADED**，无需重启设备，直接再次尝试越狱即可。
6. 每次进入 App 会自动检查更新：Gitee Releases 优先，GitHub 兜底；发现新版本会弹窗，可直接下载并交给系统安装器。也可在「关于」页手动点「检查更新」。

## 权限与准备

- **Stellar 优先、Shizuku 兜底**：两者都在时用 Stellar，Stellar 不可用才回落到 Shizuku。
- **管理器必须先安装**：ksud 不随包内置，运行时从管理器 APK 的 `lib/arm64` 原地执行。
- **建议在干净开机后执行**；6.18、6.12 与 V2505A 是三个独立入口，App 按完整内核版本自动选择。

## 一键越狱流程

**preflight**（内核 release / trace 通道）→ **98260 UAF**（fake cred / SELinux 零写）→ **cred 子进程一次性 ksud late-load** → 等模块与 su → **收尾**（ksud install、卸内核防护、SUSFS、VFS、截图恢复、拉起管理器）。

V2505A 使用独立的单入口二进制 `libv2505a`（同一份 C plant 以 `-DV2505A` 编译），不复用 `t98260`、`t61269` 的常量。它还会在唤醒 root 子进程前清零 `vr.ko` 使用的 `__tracepoint_sys_exit->funcs` 槽位。

- **快路径** plant 约 2–3 秒、全流程约 4–9 秒；回收未命中时会多花几秒，属正常波动。
- **三条链失败后**：plant 持有的页会转交给 holder，同一 boot 可直接再点一次；不要手工 kill 或 reap holder。

## 功能说明

- **首页**：ROOT 管理器下拉、四个或五个流程开关（VFS 开关仅 6.18 显示；V2505A 只显示 SUSFS）；信息卡显示内核版本与自动选择的漏洞利用方式。
- **日志**：本次运行的实时流式日志。
- **历史**：每次运行保存一个 `.log`；点开看全文，左滑条目露出「导出」按钮，可分享给其他应用。
- **关于**：版本号、检查更新、源代码 / 加入社区 / 问题反馈入口，以及由 AboutLibraries 生成的依赖清单（按库显示版本与许可证，点开可看详情）和随 APK 离线内置的开源许可全文（GPL-3.0、NOTICE、Apache-2.0、MIT、GPL-2.0、MPL-2.0）。
- **拓展功能 → Droidspaces 支持**（仅 6.18.21）：点「下载模块」会获取 GitHub 上 `Droidspaces-LKM` 的最新 release，确认后下载模块 zip；文件保存到 `Download/Shark/`，之后到 KernelSU 管理器「模块 → 安装」选择该文件刷入。模块不内置在 APK 中。
- **ROOT 管理器 → Dynamic Switching**（6.18.21 / 6.12.69）：选中后首次干净 boot 运行一次，由 `libksud_universal.so` 加载通用 KSU LKM（初始 KernelSU，内嵌 6.18/6.12 双 KMI KO）。加载后拓展功能出现「管理器热切换」项，可在 KernelSU / KernelSU-Next / BakaSU / SukiSU / FinaSU 间切换，无需重启或重新越狱；切换后重新打开目标管理器 App。

## 异常处理

| 日志 | 处理动作 |
| --- | --- |
| `PROBE_FAIL` / `kernel mismatch` | 当前内核不在支持列表（6.18.21、6.12.69，或精确匹配的 V2505A 内核）。 |
| `MANAGER` / `KSUD_MISSING` | 先安装对应的管理器 APK 再运行。 |
| `STALE_PLANT` | 上一轮 plant 仍在运行；等它转成 holder 后重试。 |
| `PLANT_FAIL` | plant 会保留 holder 并退出，同一 boot 可直接重试。 |
| `LATE_LOAD_FAIL` | 模块未在限定时间内出现；查看日志里的 ksud 输出，确认管理器版本匹配后直接重试。 |
| 内核崩溃/重启 | 属 98260 竞态固有风险，重启后再跑。 |

## 运行方式

- **6.18.21** → CVE-2026-98260-6.18（`libt98260.so`）；**6.12.69** → CVE-2026-98260-6.12（`libt61269.so`）。
- **V2505A 精确内核** → CVE-2026-98260-vivo-V2505A（`libv2505a.so`）；这是第三个独立单入口，额外处理 vivo `vr.ko`，不复用前两条工具。
- 三个入口都直接以 App 原生库（`nativeLibraryDir`）原地运行，不向 `/data/local/tmp` 部署工具；ksud 也从未安装的管理器 APK 原地执行。SUSFS 三个入口都支持（6.12/V2505A 使用 `libsusfs_guard_lkm_612.so`），VFS 开关仅对 6.18 入口开放。
- 仅用于自有设备的系统研究。
