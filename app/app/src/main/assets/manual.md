# Shark 使用手册

Shark 通过 Stellar 或 Shizuku 以 shell 身份驱动设备端 98260 exploit，加载管理器 LKM。首次使用先完成授权，再一键越狱。

> Shark 目前仅支持欧加的 6.18.21 内核设备如 一加16，以及欧真加的 6.12.69 内核设备，如一加15/T，OPPO Find X9 Ultra，OPPO Pad 3Pro，一加ACE6T，真我Neo8等等，明确暂不支持其他厂商设备以及欧真加系列非上述内核版本的设备。

## 快速开始

1. 先安装所选 ROOT 管理器（KernelSU / FinaSU / KernelSU Next / BakaSU / SukiSU Ultra），**暂不支持ci版本的 KernelSU Next**。
2. 启动并使用**无线调试或者adb**激活 **Stellar（推荐）** 或 **Shizuku** 服务，并授予 Shark 权限。
3. 回到 Shark 首页点「一键越狱」，等待日志出现 **DONE**。
4. 成功后管理器会自动拉起；开启对应开关时还会停用安全弹窗、加载 **SUSFS / VFS**（VFS支持开关仅支持6.18.21）。
5. 若日志显示 **MODULE NOT LOADED**，无需重启设备，直接再次尝试越狱即可。

## 权限与准备

- **Stellar 优先、Shizuku 兜底**：两者都在时用 Stellar，Stellar 不可用才回落到 Shizuku。
- **管理器必须先安装**：ksud 不随包内置，运行时从管理器 APK 的 `lib/arm64` 原地执行。
- **建议在干净开机后执行**；6.18 与 6.12 链路按内核自动选择，不匹配会被 preflight 拒绝。

## 一键越狱流程

**preflight**（内核 release / trace 通道）→ **98260 UAF**（fake cred / SELinux 零写）→ **cred 子进程一次性 ksud late-load** → 等模块与 su → **收尾**（ksud install、卸内核防护、SUSFS、VFS、截图恢复、拉起管理器）。

- **快路径** plant 约 2–3 秒、全流程约 4–9 秒；回收未命中时会多花几秒，属正常波动。
- **失败后不必立刻重启**：喷发页会交给 holder 并退出，同一 boot 可直接再点一次；SELinux 会在下次成功后由 KSU 恢复 Enforcing。

## 功能说明

- **首页**：ROOT 管理器下拉、四个或五个流程开关（VFS 开关仅 6.18 显示）；信息卡显示内核版本与自动选择的漏洞利用方式。
- **日志**：本次运行的实时流式日志。
- **历史**：每次运行保存一个 `.log`；点开看全文，左滑条目露出「导出」按钮，可分享给其他应用。

## 异常处理

| 日志 | 处理动作 |
| --- | --- |
| `PROBE_FAIL` / `kernel mismatch` | 当前内核不在支持列表（只支持 6.18.21 与 6.12.69）。 |
| `MANAGER` / `KSUD_MISSING` | 先安装对应的管理器 APK 再运行。 |
| `STALE_PLANT` | 同一 boot 还有竞态中的 plant；等它结束或冷启动。 |
| `PLANT_FAIL` | UAF 回收连续未命中，程序会以 holder 形式退出；直接重试一次，仍失败再冷启动。 |
| `LATE_LOAD_FAIL` | 模块未在 15 秒内出现；查看日志里的 ksud 输出，确认管理器版本匹配。 |
| 内核崩溃/重启 | 属 98260 竞态固有风险，重启后再跑。 |

## 运行方式

- **6.18.21** → CVE-2026-98260-6.18（`t98260`）；**6.12.69** → CVE-2026-98260-6.12（`t61269`），按 uname 自动选择。
- 两种链路都是 **Rust 单入口 + C 核心**，ksud 从管理器 APK 原地执行；SUSFS 需在设置里开启并随包提供对应 ko；VFS（hybridmount）目前只内置 6.18 的 ko。
- 仅用于自有设备的系统研究。
