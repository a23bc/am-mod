# AM Mod — Apple Music (Android) LSPosed 模块

**目标：给基础交互控件（返回、分享、更多、播放、收藏、三点菜单…）换样式。**
明确排除 mini player 与底部导航。

**当前阶段（v0.3.1）：控件普查，不改任何样式。**
用户要求先拿到全部控件位置、并把 Compose 绘制的控件找全，再决定怎么改。
所以 `HookMain.APPLY_STYLE = false`，界面保持原样；模块只输出控件身份与屏幕绝对坐标。

---

## 0. 当前状态（2026-11-15 实测）

| 环节 | 状态 | 判据 |
| --- | --- | --- |
| CI 编译 | ✅ | 每次 push 的 build workflow 全绿 |
| 装机 | ✅ | `adb install -r` → Success，`versionName=0.3.1` |
| 模块启用 / 作用域 | ✅ | 用户已在 LSPosed 启用，`module loaded (v0.3.1)` |
| 界面未被修改 | ✅ | 清空 logcat 后 `capture.sh`：`styles applied = 0` |
| 真实 View 控件 + 绝对坐标 | ✅ | `CTRL` 行含 `abs=[x1,y1][x2,y2]`，`errors = 0` |
| 全页面控件普查 | ✅ | `evidence/inventory.md` / `.json`：9 页、845 条、91 条 Compose 节点 |
| **最终样式** | ⛔ 待定 | 用户明确说「先不改」 |

---

## 1. 控件普查结果（核心交付）

```bash
bash tools/inventory.sh        # 遍历页面 + 逐页 dump + 汇总
```

产出 `evidence/inventory.md`（人读表格）和 `evidence/inventory.json`（结构化）。
坐标是**屏幕绝对像素** `[x1,y1][x2,y2]`，设备 1440x3168。

### 覆盖的 9 个页面 / 状态

| 页面 | 节点 | ComposeView | Compose 语义控件 | 传统 View 控件 |
| --- | --- | --- | --- | --- |
| home | 116 | 3 | 5 | 98 |
| new | 69 | 3 | 3 | 54 |
| radio | 115 | 3 | 25 | 54 |
| library | 112 | 3 | 30 | 54 |
| search | 132 | 3 | 5 | 109 |
| detail（歌单详情） | 154 | 3 | 9 | 131 |
| player（全屏播放） | 149 | 3 | 9 | 125 |
| more_menu（更多选项弹窗） | 42 | 0 | 0 | 31 |
| home_before_detail | 116 | 3 | 5 | 98 |

### 所有 Compose 绘制的控件

判定：accessibility 树里父链含 `androidx.compose.ui.platform.ComposeView` 的节点。
共 **91 条**，分布在 4 类容器：

| 容器 | 说明 |
| --- | --- |
| `floating_app_bar_container` | **顶部工具栏**：返回 / 分享 / 更多选项，23 条 |
| `top_and_bottom_gradients` | 上下渐变遮罩，8 条 |
| `dialogs_host` | Compose 弹窗宿主，8 条 |
| 无 id 的 ComposeView | 广播页 / 资料库页的 Compose 列表与分类 Tab，52 条 |

顶部工具栏（详情页实测，`floating_app_bar_container` 容器 `[0,0][1440,416]`）：

| 按钮 | 可点击区域 | 语义节点 | 容器 |
| --- | --- | --- | --- |
| 返回（`向上浏览。`） | `[20,192][212,384]` | `[68,240][164,336]` | `floating_app_bar_container` |
| 分享 | `[1028,192][1220,384]` | `[1076,240][1172,336]` | 同上 |
| 更多选项 | `[1228,192][1420,384]` | `[1268,232][1380,344]` | 同上 |

> **作废声明**：v0.1.0 的「主界面是传统 View，不用跟 Compose 死磕」**部分作废**。
> 内容区成立，但返回 / 分享 / 更多选项这类基础控件是 Compose 绘制的，
> 不是独立 View 实例，不能靠 `findViewById` 或 `View.setBackground` 改。
> 后续要改它们，得在 ComposeView 上按语义节点 bounds 叠自绘层并转发点击，或全局替换图标资源。

### 传统 View 控件（模块侧 `CTRL` 日志，去重后 30 个基础控件）

| 用途 | id |
| --- | --- |
| 播放控制 | `play_pause`、`previous_rewind`、`next_fast_forward`、`button_play`、`button_shuffle`、`button_action` |
| 更多 / 菜单 | `list_left_icon`、`more`、`menu_action_button`、`bubble_tip_close`、`bubble_tip_symbol` |
| 收藏 / 状态 | `list_favorite_icon`、`list_favorite_icon_status`、`shuffle_repeat_badge`、`badge_platter` |
| 播放器辅助 | `player_lyrics`、`player_queue`、`media_route_button`、`vocal_ctrl`、`vocal_off`、`video_surface`、`audio_badge_icon`、`audio_badge_with_text` |
| 图形 / 其它 | `thumbnail`、`lyrics_thumbnail_container`、`collection_header_artist`、`button_icon`、`icon_search`、`image_view` |

`uiautomator dump`（accessibility 树）与模块 `CTRL`（真实 View 树）互补：
改样式时以模块日志的 `id=` 为 hook 依据，Compose 那批以 inventory 的 bounds 为依据。

### 采集边界（诚实说明）

- accessibility 树只覆盖**遍历到的页面 + 当时可见且已渲染的节点**；
  `mergeDescendants` 折叠的 Composable 会被合并成一个节点，看不到更细的内部结构。
- 设置页、歌曲长按菜单、横屏、深色模式、滚动后的列表项**未采**，需要时改
  `tools/inventory.sh` 里的页面序列再跑一遍。
- 所以「所有 Compose 绘制的控件」= **当前这 9 个页面里 accessibility 暴露出来的全部**，
  不是 APK 里静态的全部 Composable。要静态全量得反编译 APK 另行统计。

---

## 2. 原理

1. LSPosed 把模块 APK 注入**指定 App 的进程**，不用改 Apple Music 的 APK。
2. 入口是 `assets/xposed_init` 里的类名，LSPosed 回调 `handleLoadPackage`；
   只在 `lpparam.packageName == com.apple.android.music` 时动手。
3. 三条触发路径互为兜底：`ViewGroup.addView`（新建 View）、`Activity.onResume`（冷启动）、
   1s 轮询 `DecorView`（Fragment 切换复用旧 View）。`SEEN` 去重，不重复输出、不刷 logcat。

`compileOnly` 是关键：Xposed API 由 LSPosed 运行时提供，**不能**打进 APK。

---

## 3. 设备实测（PJZ110）

| 项 | 结果 | 判据 |
| --- | --- | --- |
| Android | 17 | `adb shell getprop ro.build.version.release` |
| Apple Music | `7.0.0` / versionCode `1614` / targetSdk 36 | `adb shell "dumpsys package com.apple.android.music \| grep versionName"` |
| 包名 / 入口 | `com.apple.android.music` / `.common.MainActivity` | `adb shell pm list packages apple` |
| Root | KernelSU (`u:r:ksu:s0`) | `adb shell "su -c id"` |
| LSPosed | 2.2.1 + `zygisk_lsposed` | `adb shell pm list packages lsposed` |

共存模块：`org.francasio.sing`（AppleMusicSing，运行中）、`dev.a23bc.glass.extended`、
`com.a23bc.amviewdumper`（后两个未启用）。改同一批 View 时注意冲突。

---

## 4. 常用命令

```bash
# 编译（本机不跑 Gradle）
git push origin main && gh run watch
bash tools/fetch-artifact.sh <run_id>
adb install -r tools/artifacts/app-debug.apk

# 抓模块日志（真实 View 树 + 绝对坐标）
adb logcat -c && bash tools/capture.sh

# 全页面控件普查
bash tools/inventory.sh
```

`capture.sh` 指标：`module loaded` / `control entries` / `styles applied`（普查模式应为 0）/
`compose views` / `errors`（应为 0）。

LSPosed 装完或更新 APK 后，务必**强停 Apple Music 重开**才会注入新代码；
抓日志前先 `adb logcat -c`，否则会混进旧版本的累积日志。

日志进 `logs/`，截图进 `screenshots/`，基线证据进 `evidence/`，APK 进 `tools/artifacts/`。
`logs/*.log`、`logs/ui-*.xml`、`logs/inv-*.xml`、截图和 APK 都不入库；`evidence/` 入库。

---

## 5. 目录

```
app/src/main/java/.../HookMain.java      三条触发路径 / 控件筛选 / 绝对坐标输出
app/src/main/assets/xposed_init          模块入口类名
app/src/main/AndroidManifest.xml         四个 xposed* meta-data + 推荐作用域
.github/workflows/build.yml              CI：JDK 17 + Gradle 8.7 + AGP 8.6.1
tools/capture.sh                         抓界面 XML + AMMod 日志并统计
tools/fetch-artifact.sh                  取 CI 产物（绕开本机 HTTP/2 问题）
tools/inventory.sh                       遍历页面并逐页 dump
tools/inventory.py                       汇总成 inventory.md / inventory.json
evidence/inventory.md                    控件位置清单（人读）
evidence/inventory.json                   控件位置清单（结构化）
evidence/ui-dump-2026-11-15.xml          主页基线
evidence/ui-detail-2026-11-15.xml        详情页基线（含 Compose 工具栏父链）
```

---

## 6. 已排除的坑

- **本机 `curl` 访问 `api.xposed.info` / `repo1.maven.org` 报 schannel TLS 握手失败**
  （`github.com` 正常）。本机代理限制，不影响 Actions；`settings.gradle` 配了官方仓库 + 阿里云兜底。
- **`gh` 取 artifact 报 EOF**：本机代理对 `api.github.com` 的 HTTP/2 连接不稳。
  `tools/fetch-artifact.sh` 用 `curl --http1.1`：查 artifact id → 取 302 预签名地址 →
  裸 URL 下载（**不能**带 `Authorization`，双认证会被 Azure 拒）。
- **Git Bash(MSYS) 改写 `/sdcard/...`**：`adb shell uiautomator dump /sdcard/x.xml` 会静默失败，
  必须写成 `adb shell "uiautomator dump /sdcard/x.xml"`。
- **Windows python 不认 `/tmp/...`**：脚本用 `cygpath -m` 转成 `E:/...`。
  `core.autocrlf=true` 会把 shell 脚本转 CRLF，`.gitattributes` 已强制 `*.sh` 用 LF。
- **`ClassNotFoundException: androidx.fragment.app.Fragment`**：Apple Music 重打包了 AndroidX，
  hook Fragment 这条路作废，改用 DecorView 轮询。
- **v0.1.0 的 View 树 walk 中途断掉**：单个异常节点打断整棵树，现已逐节点 try/catch。
- **v0.3.0 的 `COMPOSE_VIEW` 每次轮询重复输出**（一次抓到 1330 条）：已按 identityHashCode 去重。
- **累积 logcat 会混进旧版本日志**：更新 APK 后必须 `adb logcat -c` 再抓，否则
  `styles applied` 等指标会把上一版算进来。

---

## 7. 下一步（等用户定方向）

1. 用 `evidence/inventory.md` 挑出要改的控件，指定最终样式，再打开 `APPLY_STYLE`
   并改写 `HookMain.applyStyle()`。
2. 顶部 Compose 工具栏（返回 / 分享 / 更多选项）需要另一套做法，不能靠 View 属性。
3. 若某类控件没采到，先扩展 `tools/inventory.sh` 的页面序列补采，别靠猜坐标。
