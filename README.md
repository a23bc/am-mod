# AM Mod — Apple Music (Android) LSPosed 模块脚手架

一个最小可验证的 Xposed 模块：不做任何美观改动，只负责证明「模块能进 Apple Music 进程、
能拿到完整 View 树、能改得动界面」，并把界面结构打到 logcat 作为下一版的定位依据。

**版本：0.1.0**（脚手架，不是成品）

---

## 1. 原理，三句话

1. LSPosed 把你写的 APK 当「模块」注入到**指定 App 的进程**里，不用改 Apple Music 的 APK。
2. 模块入口是 `assets/xposed_init` 里那个类名，LSPosed 回调它的 `handleLoadPackage`；
   我们只在 `lpparam.packageName == com.apple.android.music` 时动手。
3. 在 `Activity.onResume` 之后遍历 `DecorView`，就能按资源 id 找到具体控件，
   改背景、改文字、换 View、叠模糊层 —— 这就是「改界面」。

`compileOnly` 是关键：Xposed API 的类由 LSPosed 运行时提供，**不能**打进模块 APK，否则类冲突。

---

## 2. 设备实测（2026-11-15，PJZ110）

这些是本次真机上跑出来的事实，不是猜的；命令可重跑：

| 项 | 结果 | 判据 |
| --- | --- | --- |
| Android | 17 | `adb shell getprop ro.build.version.release` |
| Apple Music | `7.0.0` / versionCode `1614` / targetSdk 36 | `adb shell "dumpsys package com.apple.android.music \| grep versionName"` |
| 包名 | `com.apple.android.music` | `adb shell pm list packages apple` |
| 入口 Activity | `.common.MainActivity` | `adb shell cmd package resolve-activity --brief com.apple.android.music` |
| Root | KernelSU (`u:r:ksu:s0`) | `adb shell "su -c id"` |
| LSPosed | `org.lsposed.daemon` + `zygisk_lsposed` | `adb shell pm list packages lsposed`、`adb shell "su -c 'ls /data/adb/modules'"` |

### 界面结构结论（重要）

`evidence/ui-dump-2026-11-15.xml` 是真机 `uiautomator dump` 的基线，120 个节点：

- 主界面是**传统 View + 稳定资源 id**，只有 3 个 `ComposeView`：
  `main_container`、`listen_now_recycler`、`mini_player_title` / `mini_player_artist`、
  `navigation_bar_item_icon_container` / `_small_label_view` / `_large_label_view`、`blur_target` 等都在。
- 底部导航是 Material `BottomNavigationView` 风格的 id 命名。
- **推论：改 bottom nav / mini player 走 View hook 就行，不用跟 Compose 死磕。**
- 注意：`uiautomator dump` 看到的是 accessibility 树，和模块里 `View.getId()` +
  `getResourceEntryName()` 拿到的真实 View 树不完全等价。第二版定位以模块打出来的
  `view tree` 日志为准。

---

## 3. 第一次跑通

本机**不跑 Gradle**（JDK/SDK/Gradle 都不装到系统全局），编译走 GitHub Actions。

```bash
# 1) 推代码，CI 自动出 APK
git push origin main
gh run watch

# 2) 取 APK 并装机（或去 Actions 页面手动下载 am-mod-debug）
gh run download --name am-mod-debug --dir tools/artifacts
adb install -r tools/artifacts/app-debug.apk
```

然后在手机上：

1. LSPosed 管理器 → 模块 → 启用 **AM Mod**；
2. 作用域勾选 **Apple Music**（清单里已声明 `xposedscope`，一般会自动带出来）；
3. **强停 Apple Music**（LSPosed 的 hook 要新进程才注入），再重新打开。

### 判据：什么算「跑通」

打开 Apple Music 后，mini player 会叠一层淡红、底部导航图标区叠一层淡蓝（验证用，不是最终效果）。
再抓证据：

```bash
bash tools/capture.sh
adb logcat -s AMMod:D
```

| 现象 | 说明 |
| --- | --- |
| 一条 `module loaded` 都没有 | 模块没启用 / 作用域没勾 / 没强停 App |
| 有 `module loaded`，但没有 `view tree begin` | `onResume` hook 没命中，需要换 hook 点（改 `HookMain`） |
| 有 `view tree`，但 `highlight ... -> 0` | App 版本变了，资源 id 对不上，按新日志里的 id 重填 |
| 三者都有 | 闭环成立，可以开始写真正想改的效果 |

日志统一进 `logs/`，截图统一进 `screenshots/`，两者都不入库；`evidence/` 里的基线 XML 入库。

---

## 4. 目录

```
.github/workflows/build.yml   CI：JDK 17 + Gradle 8.7 + AGP 8.6.1，产出 debug APK
app/src/main/assets/xposed_init         模块入口类名（漏了模块完全没反应）
app/src/main/AndroidManifest.xml        四个 xposed* meta-data + 推荐作用域
app/src/main/java/.../HookMain.java     全部逻辑：加载日志 / View 树 dump / 命中染色
app/src/main/res/values/arrays.xml      推荐作用域 com.apple.android.music
evidence/ui-dump-2026-11-15.xml         真机界面基线
tools/capture.sh                        一键抓界面 XML + AMMod 日志
logs/ screenshots/ tools/artifacts/     日常产物（gitignore）
```

本仓库**不带 Gradle Wrapper**：CI 用 `gradle/actions/setup-gradle@v4` 的
`gradle-version: '8.7'` 安装 Gradle 到 PATH 后直接跑 `gradle :app:assembleDebug`。
用 Android Studio 打开项目时，IDE 会提示补 wrapper；不需要在本机装 Gradle。

---

## 5. 已排除的坑

- **本机 `curl` 访问 `api.xposed.info` / `repo1.maven.org` 报 schannel TLS 握手失败**
  （`github.com` 正常）。这是本机代理的限制，不是依赖没了；构建在 Actions 上跑，
  不受影响。`settings.gradle` 里同时配了官方仓库和阿里云 public 作兜底。
- **Git Bash(MSYS) 会改写 `/sdcard/...`**：`adb shell uiautomator dump /sdcard/x.xml`
  这种写法会静默失败，必须写成 `adb shell "uiautomator dump /sdcard/x.xml"`。
  `tools/capture.sh` 已按这个写法固化。

---

## 6. 下一步

这版只是地基。真正要「改成什么样」还需要两件事：

1. 你指定要改哪块（底部导航 / mini player / 播放页 / 配色字体圆角…）和期望效果；
2. 把 `bash tools/capture.sh` 产出的 `logs/amod-*.log` 给我 —— 里面的 `id=` 清单
   决定了第二版 hook 精确打在哪几个 View 上，不靠猜。

改效果时把 `HookMain.DUMP_TREE` 关掉，避免刷 logcat。
