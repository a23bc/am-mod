# AM Mod — Apple Music (Android) LSPosed 模块

**目标：给 Apple Music 的基础交互控件（返回、分享、更多、播放、收藏、三点菜单…）换样式。**
明确排除 mini player 与底部导航 —— 用户不改这两块。

**版本：0.2.2**（骨架 + 占位样式，最终样式待定）

---

## 0. 当前状态（2026-11-15 实测）

| 环节 | 状态 | 判据 |
| --- | --- | --- |
| CI 编译 | ✅ | run `43705842200` 全绿；`bash tools/fetch-artifact.sh 43705842200` 可取产物 |
| 装机 | ✅ | `adb install -r` → Success，`versionName=0.2.2` |
| 模块启用 / 作用域 | ✅ | 用户已在 LSPosed 启用；`module loaded` 出现 |
| hook 命中 | ✅ | 主页 19 个控件、进详情页后累计 48 个，`errors=0` |
| 占位样式生效 | ✅ | 每个命中控件叠加圆角描边（`STYLE` 行数 = `CTRL` 行数） |
| **最终样式** | ⛔ 待定 | 需要用户指定；当前只是可验证的占位效果 |
| **Compose 顶部工具栏** | ⛔ 未覆盖 | 返回 / 分享 / 更多按钮不是独立 View，见 §3 |

---

## 1. 原理

1. LSPosed 把模块 APK 注入**指定 App 的进程**，不用改 Apple Music 的 APK。
2. 入口是 `assets/xposed_init` 里的类名，LSPosed 回调 `handleLoadPackage`；
   我们只在 `lpparam.packageName == com.apple.android.music` 时动手。
3. 找到 View 后按规则改样式。要**跨页面**生效，靠三条触发路径互为兜底：

| 路径 | 覆盖 |
| --- | --- |
| `ViewGroup.addView` | 新建 View（列表滚动、弹窗、动态内容） |
| `Activity.onResume` | 冷启动时整棵树扫一次 |
| 1s 轮询 `DecorView` | Fragment 切换复用旧 View（不再 addView）的情况 |

`SEEN` 按 `identityHashCode` 去重，轮询不会重复改同一个 View，也不会刷 logcat。
`compileOnly` 是关键：Xposed API 由 LSPosed 运行时提供，**不能**打进 APK。

---

## 2. 设备实测（PJZ110）

| 项 | 结果 | 判据 |
| --- | --- | --- |
| Android | 17 | `adb shell getprop ro.build.version.release` |
| Apple Music | `7.0.0` / versionCode `1614` / targetSdk 36 | `adb shell "dumpsys package com.apple.android.music \| grep versionName"` |
| 包名 / 入口 | `com.apple.android.music` / `.common.MainActivity` | `adb shell pm list packages apple` |
| Root | KernelSU (`u:r:ksu:s0`) | `adb shell "su -c id"` |
| LSPosed | 2.2.1 + `zygisk_lsposed` | `adb shell pm list packages lsposed` |

设备上还有其它在 hook Apple Music 的模块：`org.francasio.sing`（AppleMusicSing，运行中）、
`dev.a23bc.glass.extended`（未启用）、`com.a23bc.amviewdumper`（未启用）。改同一批 View 时注意冲突。

---

## 3. 控件分两类（关键结论）

> **作废声明**：v0.1.0 结论「主界面是传统 View，不用跟 Compose 死磕」**部分作废**。
> 对内容区成立，但用户要改的**顶部基础控件恰恰是 Compose**。

`evidence/ui-detail-2026-11-15.xml` 是详情页基线，`uiautomator dump` 的父链证明：
`向上浏览。`（返回）、`分享`、`更多选项` 三个按钮都挂在
`ComposeView(floating_app_bar_container)` 下面，全链是无 id 的 `android.view.View`
语义节点，**不是可 hook 的独立 View 实例**。所以 View hook 改不到它们的视觉样式。

### A. 传统 View 类（有稳定 id，已能改）

`logs/amod-*.log` 里 `CTRL` 行去重后的 30 个控件，全部已应用占位样式：

| 用途 | id |
| --- | --- |
| 播放控制 | `play_pause`、`previous_rewind`、`next_fast_forward`、`button_play`、`button_shuffle`、`button_action` |
| 更多 / 菜单 | `list_left_icon`、`more`、`menu_action_button`、`bubble_tip_close`、`bubble_tip_symbol` |
| 收藏 / 状态 | `list_favorite_icon`、`list_favorite_icon_status`、`shuffle_repeat_badge`、`badge_platter` |
| 播放器辅助 | `player_lyrics`、`player_queue`、`media_route_button`、`vocal_ctrl`、`vocal_off`、`video_surface`、`audio_badge_icon`、`audio_badge_with_text` |
| 图形 / 其它 | `thumbnail`、`lyrics_thumbnail_container`、`collection_header_artist`、`button_icon`、`icon_search`、`image_view` |

注意 `uiautomator dump` 是 accessibility 树，和真实 View 树不等价；定位一律以模块
`CTRL` 日志里的 `id=` 为准。

### B. Compose 类（无 id，本版只标记容器）

`floating_app_bar_container` 会出现在 `COMPOSE_TOOLBAR` 日志里，但内部按钮画在
Compose 的 Canvas / semantics 上。要改它们得换做法（见 §7），不是调 `View.setBackground`。

---

## 4. 跑通

本机**不跑 Gradle**，编译走 GitHub Actions。

```bash
git push origin main
gh run watch
bash tools/fetch-artifact.sh <run_id>       # 取 APK 到 tools/artifacts/
adb install -r tools/artifacts/app-debug.apk
```

LSPosed 管理器 → 模块 → 启用 **AM Mod** → 作用域勾 **Apple Music** → **强停 Apple Music 重开**。

抓证据：

```bash
bash tools/capture.sh
```

| `capture.sh` 指标 | 判据 |
| --- | --- |
| `module loaded = 0` | 没启用 / 作用域没勾 / 没强停 |
| `control entries = 0` | 三条触发路径都没命中 |
| `styles applied < control entries` | 有控件样式应用失败，看 `style error` 行 |
| `errors > 0` | 看 `error` 行定位 |
| `compose toolbar` | 顶部 Compose 容器出现次数，仅供标记 |

日志进 `logs/`，截图进 `screenshots/`，基线证据进 `evidence/`，APK 进 `tools/artifacts/`；前三者按要求存放，后三者都不入库。

---

## 5. 目录

```
.github/workflows/build.yml    CI：JDK 17 + Gradle 8.7 + AGP 8.6.1
app/src/main/assets/xposed_init          模块入口类名
app/src/main/AndroidManifest.xml         四个 xposed* meta-data + 推荐作用域
app/src/main/java/.../HookMain.java      三条触发路径 / 控件筛选 / 占位样式
app/src/main/res/values/arrays.xml       推荐作用域
evidence/ui-dump-2026-11-15.xml          主页基线
evidence/ui-detail-2026-11-15.xml        详情页基线（含 Compose 工具栏父链）
tools/capture.sh                         抓界面 XML + AMMod 日志并统计
tools/fetch-artifact.sh                  取 CI 产物（绕开本机 HTTP/2 问题）
```

本仓库**不带 Gradle Wrapper**：CI 用 `gradle/actions/setup-gradle@v4` 的
`gradle-version: '8.7'` 装 Gradle 到 PATH，直接跑 `gradle :app:assembleDebug`。

---

## 6. 已排除的坑

- **本机 `curl` 访问 `api.xposed.info` / `repo1.maven.org` 报 schannel TLS 握手失败**
  （`github.com` 正常）。本机代理限制，不影响 Actions；`settings.gradle` 配了官方仓库 + 阿里云兜底。
- **`gh` 取 artifact 报 EOF**：本机代理对 `api.github.com` 的 HTTP/2 连接不稳（`gh run list`
  正常，artifacts 接口 EOF）。`curl --http1.1` 正常。`tools/fetch-artifact.sh` 已固化：
  查 artifact id → 取 302 预签名地址 → 裸 URL 下载（**不能**带 `Authorization`，双认证会被 Azure 拒）。
- **Git Bash(MSYS) 改写 `/sdcard/...`**：`adb shell uiautomator dump /sdcard/x.xml` 会静默失败，
  必须写成 `adb shell "uiautomator dump /sdcard/x.xml"`。
- **Windows python 不认 `/tmp/...`**：本机 python 是原生 Windows 版，脚本里经 `cygpath -m`
  转成 `E:/...`。`core.autocrlf=true` 会把 shell 脚本转 CRLF，`.gitattributes` 已强制 `*.sh` 用 LF。
- **`ClassNotFoundException: androidx.fragment.app.Fragment`**（v0.2.1 实测）：Apple Music
  把 AndroidX 类重打包了，hook Fragment 这条路**作废**，改用 DecorView 轮询。
- **v0.1.0 的 View 树 walk 中途断掉**：单个异常节点打断整棵树；现在逐节点 try/catch。
- **v0.1.0 把示例染色对准 mini player / 底栏**：方向猜错，已作废并移除。

---

## 7. 下一步（两件待定）

1. **最终样式**：当前圆角描边只是占位，需要指定要换成什么（配色 / 圆角 / 图标 / 毛玻璃…），
   然后改 `HookMain.applyStyle()`，并把 `DUMP_CONTROLS` 关掉。
2. **Compose 顶部工具栏**（返回 / 分享 / 更多）：要不要一起改？需要另一套做法——
   例如在 `floating_app_bar_container` 上按语义节点 bounds 叠加自绘层并转发点击，
   或全局替换图标资源；不能靠 View 属性。
