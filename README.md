# OnePlus 16 Jailbreak

在 **OnePlus 16（PYB110，SM8950，Android 17 / ColorOS，内核 `6.18.21-android17-5-gf957accaf563-ab15926176-4k`）** 上，通过 **CVE-2026-64560** 或 **CVE-2026-98260** 提权，并加载 **内核态 root（KernelSU / KernelSU Next / BakaSU / SukiSU Ultra）** 的一键工具与源码。

设备未解锁 BL，起手只有 adb shell（uid 2000）。工具通过 Shizuku 以 shell 身份驱动设备端 exploit，最终加载按本机 GKI 6.18.21 自编的 LKM：64560 分支走 `finit_module`，98260 分支走 `ksud late-load`（失败回退 `insmod`）。

> 仅用于自有设备的系统研究。存在竞态与重启风险，详见「风险」一节。

## 当前状态

- **双漏洞可选**：默认 `CVE-2026-64560`（逐字段零写 cred + `finit_module`）；`CVE-2026-98260` 使用本仓库的 `t98260_plant` 引擎（exec + POSIX 定时器 UAF、假 cred 指针写 + rootd），真机验证 uid 0、全量 CapEff、SELinux Permissive。98260 建议在干净开机后使用。
- **KernelSU（官方 v3.3.1）**：真机跑通，模块 Live、`su` uid 0、管理器加冕、Enforcing 下可用。
- **BakaSU（原 ReSukiSU）**：LKM/ksud/管理器对齐上游 `35252`（`v4.2.0-rc3`），真机加载成功；LKM 435,616 B。
- **KernelSU Next**：LKM 350,000 B，已按同版本 GKI 编译并在 App 中可选，尚未做实机加载验证。
- **SukiSU Ultra**：LKM/ksud 对齐 HEAD `080ba098`（版本 41097，`v4.2.1-r2-080ba098@main`），LKM 544,848 B、ksud 7,857,184 B；41068 真机加载成功，41097 为同 uapi 更新。
- App 为单页设置：ROOT 管理器与漏洞利用方式各一个下拉，另有四个流程开关；release 签名 APK v1.12 已构建。
- **全内置**：四个管理器的 ko/ksud、符号表、slide、越狱脚本与 SUSFS guard LKM 全部打包在 APK 内（`assets/pyb110` 约 51 MB，release APK 约 30 MB），运行时不联网；管理器 APK 由用户自行安装。

## 截图

<table align="center">
  <tr>
    <td align="center"><img src="docs/screenshots/app-home.jpg" width="170" alt="App 设置页"><br><sub><b>1 · App 设置页</b></sub></td>
    <td align="center"><img src="docs/screenshots/app-exploit-dropdown.jpg" width="170" alt="漏洞利用方式下拉"><br><sub><b>2 · 漏洞利用方式</b></sub></td>
    <td align="center"><img src="docs/screenshots/app-manager-dropdown.jpg" width="170" alt="ROOT 管理器下拉"><br><sub><b>3 · ROOT 管理器</b></sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/kernelsu-manager.jpg" width="170" alt="KernelSU 管理器"><br><sub><b>4 · KernelSU 管理器</b></sub></td>
    <td align="center"><img src="docs/screenshots/bakasu-manager.jpg" width="170" alt="BakaSU 管理器"><br><sub><b>5 · BakaSU 管理器</b></sub></td>
    <td align="center"><img src="docs/screenshots/sukisu-manager.jpg" width="170" alt="SukiSU Ultra 管理器"><br><sub><b>6 · SukiSU Ultra 管理器</b></sub></td>
  </tr>
</table>

## 工作原理

```
┌────────────────────────────────┐
│ App (Kotlin/Compose/Miuix)     │
│  - ROOT 管理器 / 漏洞利用方式  │
│  - 四个流程开关 / 日志页       │
└───────────────┬────────────────┘
                │ Shizuku (uid 2000)
                ▼
┌───────────────────────────────────────────────┐
│ /data/local/tmp/pyb110-<boot_id>-<epoch>/     │
│  64560：run.sh                                │
│   1 抓/解 KASLR slide                         │
│   2 按 slide 打补丁生成 ko                    │
│   3 selinux_state-7 零写 → Permissive         │
│   4 perf 泄漏 cred + 两阶段写                 │
│   5 finit_module 加载 LKM                     │
│   6 反 root 守卫 + ksud 安装                  │
│  98260：run98260.sh                           │
│   1 probe（内核指纹 / trace 通道）            │
│   2 抓/解 KASLR slide                         │
│   3 t98260_plant：UAF → 假 cred → rootd       │
│   4 t98260_su → ksud late-load（回退 insmod） │
│   5 反 root 守卫 + ksud 安装                  │
└───────────────┬───────────────────────────────┘
                ▼
┌────────────────────────────────┐
│ 内核：ksu-base*.ko (LKM)       │
│  - su / 管理器加冕             │
│  - 模块参数 allow_shell=1      │
└────────────────────────────────┘
```

关键点：

**CVE-2026-64560（run.sh）**

- **KASLR slide**：读取 `sched_blocked_reason` tracepoint 的 `%pS` 文本锚点和 per-CPU raw 指针，2MB 对齐投票。
- **关闭 SELinux**：对 `selinux_state - 7` 做 8 字节零写，只覆盖 `enforcing`，保留 `initialized`（避免二次竞态与 `load_policy` 崩溃）。
- **cred 泄漏**：`capset(0)` 触发 `kmem_cache_alloc`，用 perf tracepoint raw 直接拿到新 `cred` 的内核地址。
- **两阶段写**：phase1 清 `cred+0x48`（cap_bset）；phase2 把 `cred+0x40`（cap_effective）写成地址自身，利用 bit7/bit16 获得 `CAP_SETUID`/`CAP_SYS_MODULE`。选位需跨 CPU。
- **加载**：`setfsgid/setfsuid/setresuid(0)` 后 `finit_module(..., "allow_shell=1")`。
- **管理器加冕**：`chmod 644 /data/system/packages.list` 后原子移出/移回触发 fsnotify。

**CVE-2026-98260（run98260.sh + t98260_plant）**

- **UAF 原语**：非 leader 线程 exec 与 `CLOCK_THREAD_CPUTIME_ID` POSIX 定时器竞争；`exit_itimers` 在定时器队列节点仍挂链时释放 `k_itimer`，`release_task` 之后再对已释放节点执行 `rb_erase`。
- **拉窗与命中**：50000 个单页 memfd VMA 与 mprotect 锁线程拉长 `exec_mmap` 窗口；ESRCH 门控的 `tgkill(SIGKILL)` 只杀正在 exec 的非 leader。
- **泄漏与回收**：2048 个定时器，ftrace raw 管道逐字节扫描拿到 `k_itimer` 地址；`mm_page_alloc/free` 与 PFN 校验定位被释放的 order-1 slab 页，再用 8K AF_UNIX skb 回收。
- **写入与提权**：伪造 `rb_node` 瓦片，借 `rb_erase` 完成零写/指针写；先以同一原语把 `selinux_state - 7` 写零进入 Permissive，再把假 cred 装进 `task->cred`。
- **rootd 与交接**：假 cred 子进程在 `/data/local/tmp/t98260-root.sock` 提供 rootd，`t98260_su` 以 root 执行 `ksud late-load --kmi android17-6.18`；失败时用 `prep_patch` 按当次 slide 打补丁后 `insmod`。

**两个分支共用**

- **反 root 守卫**：ColorOS 的 `com.oplus.exsystemservice` 会在检测到提权后弹窗并在 15 秒后 `reboot("Malicious app try to root devices")`。可选开关开启时，流程会在拿到 su 后禁用该包并 kill 进程，**5 秒后自动恢复**（禁用过久会导致系统截图不可用）。

## 目录结构

```
OnePlus16-Jailbreak/
├── app/                         Android App（Gradle 工程根）
│   ├── app/                     :app 模块（Kotlin/Compose/Miuix/Shizuku）
│   │   ├── src/main/java/com/pyb110/onekey/MainActivity.kt  单页设置与流程调度
│   │   ├── src/main/assets/pyb110/  内置设备端工具与四家 ko/ksud（约 48 MB）
│   │   ├── src/main/res/        自绘图标与资源
│   │   ├── build.gradle.kts     签名配置 + R8
│   │   └── proguard-rules.pro
│   ├── gradlew / gradle/
│   ├── keystore.properties.example
│   └── settings.gradle.kts
├── exploit/                     提权工具源码
│   ├── src/t64560_w2.c          64560 两阶段 cred 写
│   ├── src/t98260_plant.c       98260 UAF / 泄漏 / 假 cred / rootd
│   ├── src/t98260_su.c          rootd 客户端
│   ├── src/prep_solve.c         设备端 slide 求解
│   ├── src/prep_patch.c         ko 符号补丁（UNDEF → link+slide）
│   └── build-98260.sh           用 NDK 静态编译 t98260_plant / t98260_su
├── remote/                      设备端脚本与符号表副本（App 运行时不下载）
│   ├── scripts/run.sh           64560 主流程
│   ├── scripts/take_slide.sh    抓 slide
│   └── syms-6.18.21.txt         该内核的符号表
├── lkm/                         LKM 构建脚本与 SUSFS 源码
│   ├── build-kernelsu.sh        官方 KernelSU
│   ├── build-next.sh            KernelSU Next
│   ├── build-bakasu.sh          BakaSU（原 ReSukiSU）
│   ├── build-sukisu.sh          SukiSU Ultra
│   ├── build-susfs.sh           SUSFS guard LKM
│   ├── susfs4ksu-lkm/           SUSFS LKM 源码（上游 28b2e85，GPL-2.0）
│   ├── susfs-tools/             SUSFS 规则矩阵测试工具
│   └── README.md
├── docs/screenshots/            App 与管理器截图
├── third_party/                 上游工具与许可（CVE 公开仓库）
├── NOTICE                       第三方来源与许可
└── LICENSE                      MIT（本项目自研代码）
```

> 设备端还使用公开仓库提供的 `t64560_v36`、`t64560_loader2`、`forker` 二进制（Apache-2.0）；`t64560_w2` 与 98260 分支的 `t98260_plant`、`t98260_su` 由本仓库 [exploit/src](exploit/src) 构建。全部打包在 `app/app/src/main/assets/pyb110/` 内，来源与许可见 `NOTICE` 与下文。

## 构建

### App

环境：JDK 21、Android SDK 37、Gradle wrapper（9.7.1）、AGP 9.4.1、Kotlin 2.4.20、Compose BOM 2026.09.00、Miuix 0.9.4。

```bash
cd app
cp keystore.properties.example keystore.properties   # 填入自己的签名信息
# local.properties 里写 sdk.dir=/path/to/android-sdk
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease   # R8 + 资源压缩，约 28 MB（内置全部设备端素材）
```

`keystore.properties`、`keystore/`、`*.jks`、`local.properties` 已被 `.gitignore` 排除。生成 release 密钥示例：

```bash
keytool -genkeypair -v -keystore keystore/release.jks -alias oneplus16 \
  -keyalg RSA -keysize 4096 -validity 10000 -storetype PKCS12
```

### 内置素材

App 把全部设备端资产打包在 `app/app/src/main/assets/pyb110/`（约 48 MB 未压缩，release APK 约 28 MB），运行时不联网。每次越狱只部署当前选择需要的文件：

- 64560：`prep_solve`、`prep_patch`、`t64560_v36`、`t64560_loader2`、`t64560_w2`、`forker`、`run.sh`；
- 98260：`t98260_plant`、`t98260_su`、`prep_solve`、`prep_patch`、`forker`、`run98260.sh`；
- 共用：`syms.txt`、`take_slide.sh`，以及所选管理器的 `ksu-base*.ko` + `ksud_*`。

更新素材时直接替换 `assets/pyb110/` 下的文件并重新打包 APK。`remote/` 保留 64560 主流程、抓 slide 脚本与符号表副本，便于不装 App 时手工调试；与 `assets/pyb110/` 的对应文件需手动保持一致。

### LKM

LKM 必须按设备内核 **6.18.21** 与其符号表自编，不能只改 vermagic 跨版本复用。

环境：GKI `android17-6.18-2026-06_r9` 源 + KDIR（`gki_defconfig`、`modules_prepare`、`security/selinux/built-in.a`）+ DDK 镜像 `ghcr.io/ylarod/ddk-min:android17-6.18-20260828`（clang r584948c）。

```bash
# KernelSU（官方）
bash lkm/build-kernelsu.sh        # 在容器内运行

# KernelSU Next
bash lkm/build-next.sh

# BakaSU（原 ReSukiSU，需 tracepoint hook + 多管理器）
bash lkm/build-bakasu.sh

# SukiSU Ultra
bash lkm/build-sukisu.sh

# SUSFS guard LKM（实验，见 lkm/README.md）
bash lkm/build-susfs.sh
```

64560 的 `finit_module` 与 98260 的 `insmod` 回退都需要设备端按当次 boot 的 KASLR slide 用 `prep_patch` 重写符号；98260 默认先尝试 `ksud late-load`，失败再回退到打补丁后的 `insmod`。管理器与 ksud 建议直接用官方 release / CI 产物（BakaSU 的 `Manager-release` 与 `ksud`），不要在本地重复编译。

`lkm/susfs4ksu-lkm` 是 vendored 的 SUSFS guard LKM 源码（上游 `28b2e85`，GPL-2.0）。为 PYB110 的 BTI 内核把 reboot supercall、hide_syms 以及 sus_map 的间接 show 入口都换成了 syscall / seq_operations wrapper（并适配上游新增的 `/proc/susfs_map` 规则节点）；`sus_map` 只对 KernelSU 会 umount 的 app 生效，`su` 授予 root 的进程不隐藏。规则矩阵与工具在 `lkm/susfs-tools/`。构建产物已内置到 App：开启「SUSFS 支持」开关后，越狱成功的流程末尾会自动把它部署到设备并 `ksud insmod`。

### 设备端工具

`prep_solve.c` / `prep_patch.c` / `t64560_w2.c` 用 NDK 静态编译为 aarch64；98260 分支的 `t98260_plant` / `t98260_su` 用 [exploit/build-98260.sh](exploit/build-98260.sh) 一键编译：

```bash
bash exploit/build-98260.sh          # 默认 NDK 28.2，可用 CC=... 覆盖编译器

CC=$NDK/toolchains/llvm/prebuilt/<host>/bin/aarch64-linux-android26-clang
$CC -O2 -static -o prep_solve exploit/src/prep_solve.c
$CC -O2 -static -o prep_patch exploit/src/prep_patch.c
$CC -O2 -static -o t64560_w2 exploit/src/t64560_w2.c
```

`run.sh` / `run98260.sh` 从 `/data/local/tmp/pyb110-<boot_id>-<epoch>/` 运行，读取 `PYB110_MANAGER`（`kernelsu` / `next` / `bakasu` / `sukisu`）、`PYB110_DISABLE_ANTIROOT`（`1` / `0`）、`PYB110_UNLOAD_GUARD`（`1` / `0`）、`PYB110_SUSFS`（`1` / `0`）与 `PYB110_LATE_LOAD`（98260，默认 `1`）选择行为。

## 使用

1. 用无线调试启动 Shizuku，并授权本 App。
2. 在「ROOT管理器」下拉选择 KernelSU / KernelSU Next / BakaSU / SukiSU Ultra，在「漏洞利用方式」下拉选择 CVE-2026-64560 或 CVE-2026-98260。切换管理器后需重启再越狱（已加载模块不能热替换）。
3. 98260 建议在干净开机后执行：`t98260_plant --probe` 会先核对内核 release 与 trace 通道；plant 命中通常 10–120 秒（预算 240 秒），期间可能概率性重启。
4. 四个流程开关：
   - 「越狱后禁用安全弹窗」：拿到 su 后禁用 `com.oplus.exsystemservice` 并 kill 进程，5 秒后自动恢复；如截图等系统功能异常，软重启（重启 zygote/SystemUI）。
   - 「越狱后软重启」：流程跑完且模块确认加载后，以 `su` 执行 `setprop ctl.restart zygote`；失败不会影响已完成的越狱。
   - 「越狱后卸载内核安全防护」：拿到 su 后 `rmmod oplus_secure_guard_new`，日志给出 `UNLOAD_GUARD_GONE` 或 `UNLOAD_GUARD_STILL_LOADED`；模块被引用时不会强制卸载，重启后会重新加载。
   - 「SUSFS 支持」：开启后越狱成功时自动把内置的 `susfs_guard_lkm.ko` 部署到设备，并以 `ksud insmod`（回退 `insmod`）加载；日志给出 `SUSFS_LOADED` / `SUSFS_LOAD_FAIL`，加载失败不影响已完成的越狱。默认关闭，关闭时该 ko 不会随流程部署。
5. 点击「一键越狱」，等待日志出现 `DONE`。
6. 越狱成功后用对应管理器授权 root。

## 已知限制

- **换管理器必须重启**：`rmmod kernelsu` 会被模块自身的 kprobe/tracepoint 引用计数挡住（EAGAIN）；强制卸载会撞上 `file_wrapper` 未反注册导致 kpanic。
- **KernelSU Next** 尚未做实机加载验证。
- **98260 只适配 6.18.21**：`t98260_plant --probe` 会校验完整内核 release 与 trace 通道，不匹配直接退出；依赖 `/sys/kernel/tracing`，建议干净开机后执行。
- **98260 的假 cred 只替换 `task->cred`**：plant 进程内 `/proc/self/status` 的 `CapEff` 读的是 `real_cred`，显示 0 属正常；以 rootd / `t98260_su` 子进程为准。
- **98260 越狱后自动清理 rootd**：模块加载且正常 `su` 可用后，流程会 `pkill t98260_plant` 并删除 `/data/local/tmp/t98260-root.sock`，日志出现 `ROOTD_CLEANED`；若 ksud 未装好、`su` 不可用，则保留 rootd 兜底并打印 `ROOTD_KEPT`。
- App 依赖 Shizuku 服务；重启后需重新启动 Shizuku。
- 反 root 开关关闭时，加载后可能触发 ColorOS 安全弹窗并在 15 秒后重启。

## 风险

- CVE-2026-64560 与 CVE-2026-98260 都是竞态 UAF，任何一次运行都可能导致内核 panic / 重启；98260 在命中前也可能重启。
- 98260 在 ksud 安装失败时会保留 0666 的 rootd socket（日志 `ROOTD_KEPT`，见「已知限制」），请及时处理。
- 本工具仅用于**自有设备**的系统研究与 root 管理；请勿用于未授权设备。
- 使用 root 与内核模块存在数据损坏、保修失效等风险，风险自负。

## 来源与许可

- 提权原语与 `t64560_v36` / `t64560_loader2` / `forker`、部分上游脚本：  
  [Feng1586/CVE-2026-64560-public](https://github.com/Feng1586/CVE-2026-64560-public)（Apache-2.0）。
- CVE-2026-98260 PYB110 公开移植（设计背景与流程参考；内置 `t98260_plant` / `t98260_su` 由本仓库 [exploit/src](exploit/src) 构建）：  
  [AzirX1337/CVE-2026-98260-OnePlus16-PYB110-Jailbreak](https://github.com/AzirX1337/CVE-2026-98260-OnePlus16-PYB110-Jailbreak)（MIT，许可全文见 `third_party/cve-2026-98260-LICENSES/AzirX1337-MIT.txt`）。
- 官方 KernelSU：[tiann/KernelSU](https://github.com/tiann/KernelSU)（GPL-2.0）。
- KernelSU Next：[rifsxd/KernelSU-Next](https://github.com/rifsxd/KernelSU-Next)（GPL-2.0）。
- BakaSU（原 ReSukiSU）：[Baka-SU/BakaSU](https://github.com/Baka-SU/BakaSU)（GPL-2.0）。
- SukiSU Ultra：[SukiSU-Ultra/SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra)（GPL-3.0）。
- SUSFS guard LKM：[inforcqb/susfs4ksu-lkm](https://github.com/inforcqb/susfs4ksu-lkm)（GPL-2.0；源码 vendored 在 `lkm/susfs4ksu-lkm`，BTI 兼容修改见 `MODIFICATIONS.md`）。
- UI：[Miuix](https://github.com/miuix-kotlin-multiplatform/miuix)（Apache-2.0）；远程执行：[Shizuku API](https://github.com/RikkaApps/Shizuku-API)（Apache-2.0）。
- 本项目自研代码（App、exploit 封装、构建脚本）采用 MIT，详见 `LICENSE`；第三方二进制与源码遵循各自许可，详见 `NOTICE`。

## 校验和

- release APK：`app/app/build/outputs/apk/release/app-release.apk` / 30,752,334 B / md5 `8781ae996cefc8173f62b926db23e1f0`（本地构建产物，不入库）。
- KernelSU LKM（6.18.21）：349,224 B；Next：350,000 B；BakaSU：435,616 B；SukiSU：544,848 B（strip 后）。
- SUSFS guard LKM（6.18.21，内置 assets）：3,372,960 B / sha256 `b810eeb9a923d163e562698766f6ac98514b1aaa8f682dc736fbaa476d542526`（未 strip）。
