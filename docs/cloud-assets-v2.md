# 云端资产版本化设计（schema v2）

> 状态：提案，未实现。实现前先冻结本文档的 schema 与 App/脚本契约。

## 1. 原则

- **稳定件内置**：漏洞利用工具（`prep_*`、`t64560_*`、`forker`、`brm_plant`、`pc98260_su`、`run*.sh`）随 APK 交付，不走网络。
- **快变件云端**：管理器 LKM（ko）与 ksud 同步上游频繁，放 GitHub release assets，由签名清单下发。
- **按需下发**：只下载“所选漏洞 + 所选管理器 + 所选版本”的那一对 ko/ksud；内置件也只部署所选漏洞需要的一组。
- **可回退**：老版本必须还能用；清单升级不能打断已发布 APK。

## 2. 现状（schema 1）

```json
{
  "schema": 1,
  "kernel": "6.18.21",
  "shared": [run.sh, take_slide.sh, syms.txt],
  "managers": {
    "kernelsu": { "label": "KernelSU", "version": "v3.3.1 (32680)", "files": [ko, ksud] }
  }
}
```

限制：每个管理器只有一个版本；App 只能选“管理器种类”，不能选版本；切换上游等于强制升级。

## 3. 目标 schema（v2）

```json
{
  "schema": 2,
  "kernel": "6.18.21",
  "generated": "2026-10-27T00:00:00Z",
  "shared": [
    { "name": "take_slide.sh", "url": "...", "sha256": "...", "size": 614 },
    { "name": "syms.txt",       "url": "...", "sha256": "...", "size": 6534125 },
    { "name": "run.sh",         "url": "...", "sha256": "...", "size": 4789 }
  ],
  "managers": {
    "kernelsu": {
      "label": "KernelSU",
      "versions": [
        {
          "version": "v3.3.1 (32680)",
          "channel": "stable",
          "kmi": "android17-6.18",
          "late_load": true,
          "manager_pkg": "me.weishu.kernelsu",
          "manager_version": "3.3.1",
          "files": [
            { "name": "ksu-base.ko",   "url": "...", "sha256": "...", "size": 349224 },
            { "name": "ksud_official", "url": "...", "sha256": "...", "size": 4990848 }
          ]
        },
        { "version": "v3.3.0 (32680)", "channel": "legacy", "late_load": true, "...": "..." }
      ]
    }
  }
}
```

字段约定：

| 字段 | 必填 | 含义 |
|---|---|---|
| `version` | 是 | 展示与选择用的版本串（含构建号），同一管理器内唯一 |
| `channel` | 否 | `stable` / `beta` / `legacy`，仅用于 UI 标注与默认排序 |
| `kmi` | 是 | 目标 KMI（如 `android17-6.18`），App 与当前内核不符则置灰 |
| `late_load` | 是 | 该版 ksud 是否支持 `late-load`（98260 移交给 KSU 依赖它） |
| `manager_pkg` / `manager_version` | 是 | 管理器 APK 包名与建议版本，用于“已装版本不匹配”提示 |
| `files` | 是 | 该版本独占的 ko/ksud，校验规则同 v1（size + sha256 + ko vermagic） |

## 4. App 行为

- 选择：`管理器`（种类）→ `版本` 两级下拉；版本按种类记忆，默认选 `channel=stable` 中最新的一个。
- 兼容提示：
  - `kmi` 与本机内核不符 → 不可选并注明原因；
  - 已装管理器版本 ≠ `manager_version` → 允许继续，但显示警告（模块可加载、管理器功能可能不匹配）；
  - `late_load=false`：98260 流程自动退回 `prep_patch + insmod`；64560 不受影响。
- 下载：只拉所选版本的两个文件 + 需要的 shared；sha256/size/vermagic 校验不变。
- 缓存：以 sha256 为文件名；`prune()` 不再按“本次清单”删除，而是保留最近使用的 N 个版本（默认 3），超出按 LRU 淘汰；`.part` 一律清理。
- 离线：有缓存则离线可跑；无缓存且断网时明确报“需要联网下载所选版本”。

## 5. 脚本契约

App 通过环境变量传给 `run.sh` / `run98260.sh`：

| 变量 | 示例 | 用途 |
|---|---|---|
| `PYB110_MANAGER` | `kernelsu` | 现有，管理器种类 |
| `PYB110_MANAGER_VERSION` | `v3.3.1 (32680)` | 新增，仅用于日志与校验 |
| `PYB110_LATE_LOAD` | `1` / `0` | 新增，98260 据此选 late-load 或 insmod 回退 |
| `PYB110_DISABLE_ANTIROOT` | `1` / `0` | 现有 |

脚本内仍保留自动回退：late-load 失败 → `prep_patch + insmod`，与标志位无关，标志位只决定“先试哪条”。

## 6. 发布流程

```
release assets（tag: assets-latest）
  shared/take_slide.sh, syms-6.18.21.txt, run.sh
  kernelsu/v3.3.1/ksu-base.ko, ksud_official
  kernelsu/v3.3.0/...
  bakasu/v4.2.0-rc3/...
  ...
  manifest.json + manifest.sig
```

`tools/publish_assets.py` 改动：

1. `tools/managers.json` 从单版本改为 `versions[]`，每项带 `dir`（release 内子目录）；
2. 生成 schema 2 清单，`generated` 时间戳 + ECDSA 签名；
3. 旧 schema 1 清单保留一个版本周期，供老 APK 使用（App 端对 schema 1 保持兼容解析）。

## 7. 风险与边界

- **版本三元组绑定**：ko / ksud / 管理器 APK 必须同源同版；清单不打包管理器 APK，只提示版本。
- **老版本安全**：legacy 渠道可能含已知问题，需要标注；默认不选。
- **内核升级**：`kernel` 字段仍做硬门槛；跨 KMI 不混用。
- **清单增长**：只增元数据，资产在 release；但每个版本 ~6–8 MB，保留过多会拖慢发布。
- **签名密钥**：与现有清单同钥；密钥轮换需要 App 侧双公钥过渡。

## 8. 待确认

1. 每个管理器保留几个版本？（提案：最近 3 个，含当前 stable）
2. 是否把管理器 APK 也纳入云端版本（可省去用户手动装）？
3. legacy/beta 渠道是否在 UI 中可见，还是仅 `stable`？
4. 缓存上限按“版本数”还是“总字节数”（提案：版本数 3，向后兼容简单）。
