# AM Mod — Apple Music (Android) LSPosed 模块

**目标：给基础交互控件（返回、分享、更多、播放、收藏、三点菜单…）换样式。**
明确排除 mini player 与底部导航。

**当前版本 v0.5.0**：顶部 **返回 / 分享 / 更多选项** 三个按钮被**整体替换**成
液态玻璃按钮（不是叠遮罩），玻璃内折射的是真实界面背景，图标重画。
其它控件仍保持原样（`APPLY_STYLE=false`）。

---

## 0. 当前状态（2026-11-15 实测）

| 环节 | 状态 | 判据 |
| --- | --- | --- |
| CI 编译 | ✅ | 最新 build workflow 全绿 |
| 装机 | ✅ | `adb install -r` → Success，`versionName=0.5.0` |
| 玻璃层安装 | ✅ | `glass install enter 1440x476` → `glass installed: 3 full replacement buttons` |
| **触摸穿透** | ✅ | 点右上「更多选项」区域后弹出「设置 / 账户」菜单（节点 12） |
| 界面其它部分 | ✅ 未改 | `capture.sh`：`styles applied = 0` |
| 视觉效果 | ⚠️ 可用但仍有瑕疵 | `screenshots/glass-replace-final-20261115.png`，见 §1 已知不足 |
| 控件普查 | ✅ | `evidence/inventory.md` / `.json`：9 页、845 条、91 条 Compose 节点 |
| 完整 Compose 集成 | ⛔ 未做 | 技术判断与代价见 §1；等用户确认是否升级构建链 |

---

## 1. 整体替换（v0.5.0）

> **作废声明**：v0.4.x 的「在原按钮上叠一层半透明玻璃罩」被用户否掉 ——
> 验收标准是「整个按钮替换成液态玻璃」，不是加遮罩。该方案已废弃。

### 关于「完整 Compose 集成」的判断

参考库 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（分支 `kmp`，
Apache-2.0，`io.github.kyant0:backdrop:2.0.1`）是 **Compose Multiplatform** 库。
查 `gradle/libs.versions.toml` 得到它的构建基线：
**CMP 1.12.0 + Kotlin 2.4.10 + AGP 9.3.2**；当前模块是 AGP 8.6.1 + Gradle 8.7 纯 Java。

直接集成要整体升级构建链，还要在目标 App 进程里给 `ComposeView` 补
`LifecycleOwner` / `SavedStateRegistryOwner`，并与 Apple Music 自带的 Compose 共存。
更关键的是：库的 `rememberCanvasBackdrop(onDraw)` 最终也只是**把背景画进 Canvas，
再用同一批 AGSL shader 采样** —— 这一步用原生 `RuntimeShader` 就能做，且已经实测可行。

所以本轮先把验收标准做出来（整体替换 + 折射真实背景），**没有引入 Compose 运行时**。
如果用户要的是「必须跑 Compose 代码」这件事本身，再单独升级构建链。

### 实现链路

1. `GlassBackdrop` 每 500ms 把 `DecorView` 的工具栏区域画进 Bitmap；
   期间把模块自己的 overlay 全部设 `INVISIBLE`，避免递归。
2. 三个按钮区域用「正上方同尺寸的干净背景」贴补，**抹掉原生按钮（含图标）**；
   再对整块背景做一次轻度模糊，消掉贴补接缝，也让 background 像在玻璃后面。
3. `CoverView` 用这块不透明背景盖住整个工具栏 → 原生按钮不再可见。
4. 三个 `GlassButtonView` 以同一块**真实背景**为 `content`，
   用库同款 `RoundedRectRefractionShaderString` 做边缘折射，
   叠玻璃表面（渐变 tint + 描边 + 库同款 `DefaultHighlightShaderString`），
   所有绘制裁到圆形内，最后**重画白色图标**（返回箭头 / 分享 / 三点）。
5. 所有 overlay 都 `setClickable(false)`，事件穿透到下层原按钮，功能不变。

用到的库组件：`BackdropShaders.REFRACTION` / `.HIGHLIGHT`（源码引用，保留 Apache-2.0 头）、
`LiquidButton` 的 `blur(2dp)` + `lens(12dp, 24dp)`、`Highlight.Default` 白 / 45° / falloff 1、
`BlendMode.Plus`。

### 已知不足（如实）

- 圆形玻璃外仍有一圈很淡的方形轮廓（背景贴补 / View 边界的痕迹），没完全消掉。
- 玻璃偏亮、不够透；`mTint` / `mHighlight` 强度还能再调。
- 背景是 500ms 刷新的快照，不是逐帧；滚动时玻璃里的背景会滞后，且有 CPU 开销。
- 背景贴补是近似 inpaint，复杂纹理下可能有重复感。
- 只做顶部三个按钮，其余基础控件未动；玻璃层在所有页面常显。

---

## 2. 控件普查结果

```bash
bash tools/inventory.sh
```

`evidence/inventory.md` / `.json`：**9 个页面 / 845 条控件记录 / 91 条 Compose 语义节点**，
屏幕绝对 bounds。判定口径：accessibility 树里父链含
`androidx.compose.ui.platform.ComposeView` 即 Compose 绘制。

四类 Compose 容器：`floating_app_bar_container`（顶部工具栏 23 条）、
`top_and_bottom_gradients`(8)、`dialogs_host`(8)、无 id（广播/资料库列表 52 条）。
返回按钮语义节点 `[68,240][164,336]`、可点击父区 `[20,192][212,384]`。

传统 View 侧去重后有 30 个基础控件（`play_pause`、`menu_action_button`、
`button_play`、`list_favorite_icon`…），`CTRL` 日志带绝对 bounds。

**边界**：只覆盖这 9 页里当时可见、已渲染的节点；`mergeDescendants` 折叠的
Composable 看不到内部；设置页、长按菜单、横屏、深色模式、滚动后列表项未采。

---

## 3. 原理

1. LSPosed 把模块 APK 注入**指定 App 的进程**，不改 Apple Music 的 APK。
2. 入口 `assets/xposed_init` → `handleLoadPackage`，只对 `com.apple.android.music` 动手。
3. 三条触发路径互为兜底：`ViewGroup.addView`、`Activity.onResume`、
   1s 轮询 `DecorView`（Apple Music 重打包了 AndroidX，找不到 Fragment 类）。

`compileOnly` 是关键：Xposed API 由 LSPosed 运行时提供，**不能**打进 APK。

---

## 4. 设备（PJZ110）

Android 17 / KernelSU / LSPosed 2.2.1 / Apple Music `7.0.0` (1614)。
判据：`adb shell getprop ro.build.version.release`、
`adb shell "dumpsys package com.apple.android.music | grep versionName"`。

共存模块：`org.francasio.sing`（运行中）、`dev.a23bc.glass.extended`、
`com.a23bc.amviewdumper`（后两个未启用）。

---

## 5. 常用命令

```bash
git push origin main && gh run watch
bash tools/fetch-artifact.sh <run_id>      # curl --http1.1
adb install -r tools/artifacts/app-debug.apk

# curl 报 schannel TLS 握手失败 (exit 35) 时的备用下载：
powershell -NoProfile -File tools/fetch-artifact.ps1 -RunId <run_id>

adb logcat -c && bash tools/capture.sh
bash tools/inventory.sh
adb exec-out screencap -p > screenshots/glass.png
```

更新 APK 后必须**强停 Apple Music 重开**才注入新代码；抓日志前先 `adb logcat -c`。

---

## 6. 目录

```
app/src/main/java/.../HookMain.java        三条触发路径 / 控件筛选 / 玻璃层安装
app/src/main/java/.../BackdropShaders.java 库同款 AGSL shader（Apache-2.0，含来源）
app/src/main/java/.../GlassBackdrop.java  界面快照 / 抹除原按钮 / 羽化接缝
app/src/main/java/.../GlassToolbar.java   CoverView + 三个替换按钮 + 定时刷新
app/src/main/java/.../GlassButtonView.java 真实背景折射 / 玻璃表面 / 重画图标
.github/workflows/build.yml                CI：JDK 17 + Gradle 8.7 + AGP 8.6.1
tools/capture.sh | fetch-artifact.sh | fetch-artifact.ps1
tools/inventory.sh | inventory.py
evidence/inventory.md | .json
screenshots/glass-*-20261115.png          各轮效果对照
```

---

## 7. 已排除的坑

- 本机 `curl` 对 `api.xposed.info` / `repo1.maven.org` / 偶发对 `api.github.com`
  schannel TLS 握手失败；`gh run download` 对 artifacts 端点 EOF。
  `.NET Invoke-WebRequest` 可用，已固化为 `fetch-artifact.ps1`。
- `gh` 取 artifact 需走 302 预签名地址，之后**不能**再带 `Authorization`（双认证被 Azure 拒）。
- Git Bash(MSYS) 改写 `/sdcard/...`：整条 adb shell 命令必须放引号里。
- Windows python 不认 `/tmp/...`；`core.autocrlf=true` 会把 `*.sh` 转 CRLF，`.gitattributes` 已固定 LF。
- `ClassNotFoundException: androidx.fragment.app.Fragment`（Apple Music 重打包 AndroidX）。
- CI 编译错误：`android.annotation.RequiresApi` 不存在、`PorterDuff.Mode.PLUS` 不存在
  → 去掉注解、用 `BlendMode.PLUS`；`GlassToolbar.isInstalled()` 重写时被删导致编译失败。
- `decor.post` 里重读 ComposeView `getWidth()` 会拿到 0 → 扫描时快照坐标/尺寸再传入。
- 页面显隐靠 View 树 id 不可靠（后台播放页 View 常驻）→ 放弃启发式，改常显。
- 快照必须把 overlay 设 INVISIBLE，否则 `decor.draw()` 无限递归。
- 首版不透明快照 + 填充式高光 = 银色金属纽扣；填充式高光会提亮整片，必须只描边。

---

## 8. 下一步（等用户定）

1. 精修玻璃：更透 / 更亮 / 色散折射 / 胶囊形；消掉圆形外的方形轮廓。
2. 推广到其余基础控件（30 个传统 View 控件可直接 `APPLY_STYLE`）。
3. 是否升级到 **CMP 1.12 + Kotlin 2.4 + AGP 9.x** 做真正的 Compose 集成（§1 的代价）。
4. 是否用 accessibility 语义节点做精确的页面显隐，减少常显带来的开销。
