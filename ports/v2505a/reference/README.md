# vivo `vr.ko` 绕过参考

本目录只记录公开方法与本次移植结论，不包含第三方仓库代码。

## 来源

- <https://github.com/janewas/cve-2026-98260-vivo-gki-research>
- <https://github.com/DONGDERS/Ghostlock-vr>
- <https://github.com/YuKongA/ghostlock-app/issues/206>

## 采用的方案

`DONGDERS/Ghostlock-vr` 的 W1c 方案：

- 对 `__tracepoint_sys_exit->funcs` 做一次零写；
- 该 tracepoint 的注册链随后永久失联；
- 不需要修改或刷写 `vendor_boot`；
- 不依赖直接定位 `vr.ko` 的运行时函数地址。

`janewas/cve-2026-98260-vivo-gki-research` 的 `docs/04-vivo-antroot-vr.md`
也确认 vivo `vr.ko` 会从 `__tracepoint_sys_exit` 检查和终止提权进程。

## V2505A 适配

从 OTA 恢复的 `vmlinux-v2505a.elf` 中确认：

```text
__tracepoint_sys_exit = 0xffffffc082594840
sizeof(struct tracepoint) = 0x50
tracepoint.funcs = +0x48
```

运行时目标：

```c
__tracepoint_sys_exit + kaslr_slide + 0x48
```

完整链路在该地址零写成功后，子进程不再被 `vr.ko` 的 `sys_exit`
探针杀死，KernelSU late-load 成功。
