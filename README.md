# AM Mod — Apple Music (Android) LSPosed 模块

**目标：给基础交互控件（返回、分享、更多、播放、收藏、三点菜单…）换样式。**
明确排除 mini player 与底部导航。

**当前版本 v0.4.1**：传统 View 控件仍保持原样（`APPLY_STYLE=false`）；
只给顶部 **返回 / 分享 / 更多选项** 三个 Compose 按钮做了液态玻璃预览，先给用户看效果。

---

## 0. 当前状态（2026-11-15 实测）

| 环节 | 状态 | 判据 |
| --- | --- | --- |
| CI 编译 | ✅ | 最新 build workflow 全绿 |
| 装机 | ✅ | `adb install -r` → Success，`versionName=0.4.1` |
| 玻璃层安装 | ✅ | `glass install enter 1440x476` → `glass installed: 3 buttons` |
| 视觉效果 | ✅ | `screenshots/glass-detail-final-20261115.png` |
| **触摸穿透** | ✅ | 点返回按钮 `(116,288)` 后 `向上浏览。` 消失、回到主页（节点数 157→118） |
| 界面其它部分 | ✅ 未改 | `capture.sh`：`styles applied = 0` |
| 控件普查 | ✅ | `evidence/inventory.md` / `.json`：9 页、845 条、91 条 Compose 节点 |
| 最终样式 | ⛔ 待定 | 等用户看效果后定 |

---

## 1. 液态玻璃预览（v0.4.1）

### 能不能直接「用这个库」

参考仓库 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（`kmp` 分支，
Apache-2.0，Maven `io.github.kyant0:backdrop:2.0.1`），是 **Compose Multiplatform** 库：
核心是 `Backdrop` + `Modifier.drawBackdrop { vibrancy(); blur(); lens() }`，
官方还提供 `rememberCanvasBackdrop(onDraw)` 可自定义背景。

**直接集成到本模块不现实**，两条硬约束：

1. 返回 / 分享 / 更多选项由 Apple Music 自己的 `ComposeView(floating_app_bar_container)` 绘制，
   不是独立 View；我们没法把目标 App 的 Compose 树接进库的 `Backdrop`，也改不了它们的 Modifier。
2. 模块要自带 Kotlin + Compose 运行时 + Compose compiler，还得在目标进程补
   `LifecycleOwner` / `SavedStateRegistryOwner`；构建链复杂度、APK 体积、与 App 自带 Compose
   的类冲突风险都明显上升。

### 实际做法（仍然用库的样式）

以**源码方式引用库同款 AGSL shader**（`BackdropShaders.java`，保留 Apache-2.0 版权头），
在原生 View 层用 Android `RuntimeShader` / `RenderEffect` 复现：

| 库的组件 | 本模块对应 |
| --- | --- |
| `RoundedRectRefractionShaderString` | `BackdropShaders.REFRACTION` → `RenderEffect.createRuntimeShaderEffect` |
| `DefaultHighlightShaderString` | `BackdropShaders.HIGHLIGHT` → 仅描边高光 |
| `LiquidButton`: `blur(2dp)` + `lens(12dp, 24dp)` | 同参数（预览把 blur 降到 1dp 让边缘更清晰） |
| `Highlight.Default`: 白 0.5 / 45° / falloff 1 / width 0.5dp | 同角度与 falloff，预览把 width/alpha 放大 |
| `BlendMode.Plus` | `Paint.setBlendMode(BlendMode.PLUS)` |

`GlassToolbar` 在 DecorView 上按 `evidence/inventory.md` 的实测 bounds 叠三个
`GlassButtonView`（返回 `[20,192][212,384]`、分享 `[1028,192][1220,384]`、
更多选项 `[1228,192][1420,384]`，按工具栏宽度等比缩放）。
三个 View 都 `setClickable(false)`、不设触摸监听，**事件穿透到下层原按钮**。

### 诚实说明限制

- 当前是**半透明玻璃罩**：折射的是玻璃自身的 tint / 描边，**没有折射背后的界面内容**。
  原生 View 的 `RenderEffect` 只能处理自己绘制的内容，采不到下层 View 的像素。
  要真正「透过玻璃看到被扭曲的背景」，需要上面说的完整 Compose 集成 +
  `CanvasBackdrop` 喂入界面快照，或把背景快照做擦除后叠加。
- 玻璃层目前**在所有页面常显**（主页顶部本来也有按钮容器，视觉不算突兀）。
  之前尝试用 View 树特征 id 判断页面，实测不可靠：后台播放页的 View 一直在树里，
  会把主页误判成有返回按钮；且 `setVisible` 早于 `install` 会让按钮永远停在 GONE。
  已知行为，后续要用 accessibility 语义节点（`向上浏览。`）做判据才稳。
- 只做了这三个按钮，其它基础控件仍是原样。

---

## 2. 控件普查结果

```bash
bash tools/inventory.sh        # 遍历页面 + 逐页 dump + 汇总
```

产出 `evidence/inventory.md`（人读表格）和 `evidence/inventory.json`（结构化）。
坐标是**屏幕绝对像素** `[x1,y1][x2,y2]`，设备 1440x3168。

**9 个页面 / 845 条控件记录 / 91 条 Compose 语义节点**：
home、new、radio、library、search、detail、player、more_menu、home_before_detail。

判定：accessibility 树里父链含 `androidx.compose.ui.platform.ComposeView` 即 Compose 绘制。
四类容器：`floating_app_bar_container`（顶部工具栏 23 条）、`top_and_bottom_gradients`(8)、
`dialogs_host`(8)、无 id ComposeView（广播/资料库列表 52 条）。
返回按钮语义节点 `[68,240][164,336]`、可点击父区 `[20,192][212,384]`。

传统 View 侧去重后有 30 个基础控件（`play_pause`、`menu_action_button`、`button_play`、
`list_favorite_icon`…），模块 `CTRL` 日志带绝对 bounds，是后续改样式的 hook 依据。

**采集边界**：只覆盖这 9 页里当时可见、已渲染的节点；`mergeDescendants` 折叠的
Composable 看不到内部；设置页、长按菜单、横屏、深色模式、滚动后列表项未采。

---

## 3. 原理

1. LSPosed 把模块 APK 注入**指定 App 的进程**，不用改 Apple Music 的 APK。
2. 入口是 `assets/xposed_init` 里的类名，LSPosed 回调 `handleLoadPackage`；
   只在 `lpparam.packageName == com.apple.android.music` 时动手。
3. 三条触发路径互为兜底：`ViewGroup.addView`（新建 View）、`Activity.onResume`（冷启动）、
   1s 轮询 `DecorView`（Fragment 切换复用旧 View）。`SEEN` 去重，不刷 logcat。

`compileOnly` 是关键：Xposed API 由 LSPosed 运行时提供，**不能**打进 APK。

---

## 4. 设备实测（PJZ110）

| 项 | 结果 | 判据 |
| --- | --- | --- |
| Android | 17 | `adb shell getprop ro.build.version.release` |
| Apple Music | `7.0.0` / versionCode `1614` / targetSdk 36 | `adb shell "dumpsys package com.apple.android.music \| grep versionName"` |
| Root / LSPosed | KernelSU / 2.2.1 + `zygisk_lsposed` | `adb shell "su -c id"`、`adb shell pm list packages lsposed` |

共存模块：`org.francasio.sing`（AppleMusicSing，运行中）、`dev.a23bc.glass.extended`、
`com.a23bc.amviewdumper`（后两个未启用）。

---

## 5. 常用命令

```bash
# 编译（本机不跑 Gradle）
git push origin main && gh run watch
bash tools/fetch-artifact.sh <run_id>
adb install -r tools/artifacts/app-debug.apk

# 抓模块日志（真实 View 树 + 绝对坐标）
adb logcat -c && bash tools/capture.sh

# 全页面控件普查
bash tools/inventory.sh

# 截图
adb exec-out screencap -p > screenshots/glass-detail.png
```

更新 APK 后务必**强停 Apple Music 重开**才会注入新代码；抓日志前先 `adb logcat -c`，
否则会混进旧版本的累积日志。

---

## 6. 目录

```
app/src/main/java/.../HookMain.java        三条触发路径 / 控件筛选 / 玻璃层安装
app/src/main/java/.../BackdropShaders.java 库同款 AGSL shader（Apache-2.0，含来源）
app/src/main/java/.../GlassToolbar.java    三个玻璃按钮的定位与安装
app/src/main/java/.../GlassButtonView.java 玻璃绘制 + RenderEffect
app/src/main/assets/xposed_init            模块入口类名
.github/workflows/build.yml                CI：JDK 17 + Gradle 8.7 + AGP 8.6.1
tools/capture.sh | fetch-artifact.sh      抓日志 / 取 CI 产物
tools/inventory.sh | inventory.py         全页面控件普查
evidence/inventory.md | .json             控件位置清单
screenshots/glass-*-20261115.png          各轮效果对照
```

---

## 7. 已排除的坑

- **本机 `curl` 访问 `api.xposed.info` / `repo1.maven.org` TLS 握手失败**（`github.com` 正常）。
  本机代理限制，不影响 Actions；`settings.gradle` 配了官方仓库 + 阿里云兜底。
- **`gh` 取 artifact 报 EOF**：本机代理对 `api.github.com` 的 HTTP/2 不稳。
  `tools/fetch-artifact.sh` 用 `curl --http1.1` 走 302 预签名地址（**不能**再带
  `Authorization`，双认证会被 Azure 拒）。
- **Git Bash(MSYS) 改写 `/sdcard/...`**：整条 adb shell 命令必须放在引号里。
- **Windows python 不认 `/tmp/...`**：脚本用 `cygpath -m`；`.gitattributes` 强制 `*.sh` 用 LF。
- **`ClassNotFoundException: androidx.fragment.app.Fragment`**：Apple Music 重打包了 AndroidX。
- **`android.annotation.RequiresApi` 不存在**（要 AndroidX 注解）、`PorterDuff.Mode.PLUS` 不存在
  （Compose 的 `BlendMode.Plus` 对应原生 `BlendMode.PLUS`）—— 两个 CI 编译错误。
- **ComposeView 尺寸在下一帧抖动**：`decor.post` 里再读 `toolbar.getWidth()` 会拿到 0，
  导致玻璃层静默装不上；改为扫描时快照坐标/尺寸再传入。
- **页面显隐判据不可靠**：后台播放页 View 一直在 View 树里；已放弃 id 启发式，改常显。
- **首版不透明快照 + 填充式高光**会变成三个银色金属纽扣；改为半透明玻璃罩 + 仅描边高光。

---

## 8. 下一步（等用户看效果后定）

1. 效果方向：玻璃要更透 / 更亮 / 带色散折射 / 换成胶囊形，还是回到克制风格；
   改 `GlassButtonView.configure()` 的参数即可。
2. 要不要推广到其它基础控件（30 个传统 View 控件可直接 `APPLY_STYLE`）与其余 Compose 控件。
3. 要不要做完整 Compose 集成（真正折射背后内容），代价见 §1。
4. 要不要用 accessibility 语义节点做精确的页面显隐。
