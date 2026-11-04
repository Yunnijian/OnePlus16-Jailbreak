# Shark 使用手册

Shark 通过 Stellar 或 Shizuku 以 shell 身份驱动设备端 98260 exploit，加载管理器 LKM。首次使用先完成授权，再一键越狱。

> Shark 目前支持欧加 6.18.21 内核设备（如一加 16）、欧真加 6.12.69 内核设备，以及单独适配的 vivo V2505A（内核 `6.12.23-android16-5-g41f4cb1fa7c7-abogki465038634-4k`）。V2505A 入口仅对该完整内核版本开放，当前只在 KernelSU 上完成实机验证。

## 快速开始

1. 先安装所选 ROOT 管理器（KernelSU / FinaSU / KernelSU Next / BakaSU / SukiSU Ultra），**暂不支持ci版本的 KernelSU Next**。
2. 启动并使用**无线调试或者adb**激活 **Stellar（推荐）** 或 **Shizuku** 服务，并授予 Shark 权限。
3. 回到 Shark 首页点「一键越狱」，等待成功日志。V2505A 入口以 `V2505A_LATE_LOAD_OK`、`SU_RC=0` 和 `uid=0(root)` 为准。
4. 成功后管理器会自动拉起；开启对应开关时还会停用安全弹窗、加载 **SUSFS / VFS**（VFS 开关仅 6.18.21 显示；V2505A 只显示 SUSFS，使用 6.12 的 ko）。
5. 若日志显示 **MODULE NOT LOADED**，无需重启设备，直接再次尝试越狱即可。

## 权限与准备

- **Stellar 优先、Shizuku 兜底**：两者都在时用 Stellar，Stellar 不可用才回落到 Shizuku。
- **管理器必须先安装**：ksud 不随包内置，运行时从管理器 APK 的 `lib/arm64` 原地执行。
- **建议在干净开机后执行**；6.18、6.12 与 V2505A 是三个独立入口，App 按完整内核版本自动选择。

## 一键越狱流程

**preflight**（内核 release / trace 通道）→ **98260 UAF**（fake cred / SELinux 零写）→ **cred 子进程一次性 ksud late-load** → 等模块与 su → **收尾**（ksud install、卸内核防护、SUSFS、VFS、截图恢复、拉起管理器）。

V2505A 使用自己的 `v2505a_plant` 和 `v2505a_run.sh`，不复用 `t98260`、`t61269`。它还会在唤醒 root 子进程前清零 `vr.ko` 使用的 `__tracepoint_sys_exit->funcs` 槽位。

- **快路径** plant 约 2–3 秒、全流程约 4–9 秒；回收未命中时会多花几秒，属正常波动。
- **三条链失败后**：plant 持有的页会转交给 holder，同一 boot 可直接再点一次；不要手工 kill 或 reap holder。

## 功能说明

- **首页**：ROOT 管理器下拉、四个或五个流程开关（VFS 开关仅 6.18 显示；V2505A 只显示 SUSFS）；信息卡显示内核版本与自动选择的漏洞利用方式。
- **日志**：本次运行的实时流式日志。
- **历史**：每次运行保存一个 `.log`；点开看全文，左滑条目露出「导出」按钮，可分享给其他应用。

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

- **6.18.21** → CVE-2026-98260-6.18（`t98260`）；**6.12.69** → CVE-2026-98260-6.12（`t61269`）。
- **V2505A 精确内核** → CVE-2026-98260-vivo-V2505A（`v2505a_plant` + `v2505a_run.sh`）；这是第三个独立入口，额外处理 vivo `vr.ko`，不复用前两条工具。
- 三条链路都从已安装管理器 APK 原地执行 ksud；SUSFS 开关三个入口都支持（V2505A 用 6.12 的 ko），VFS 开关仅对 6.18 入口开放。
- 仅用于自有设备的系统研究。
