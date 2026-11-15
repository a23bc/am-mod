#!/usr/bin/env bash
# 遍历 Apple Music 主要页面，逐页导出 accessibility 树，再汇总成控件清单。
# 用法：bash tools/inventory.sh
#
# 为什么走 accessibility 树：Compose 绘制的按钮不是独立 View，模块里的
# View 树拿不到；uiautomator dump 能把 Compose 语义节点（含 bounds）暴露出来。
# 注意 MSYS 会改写 /sdcard/...，所以整条 adb shell 命令必须放在引号里。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT_M="$(cygpath -m "$ROOT")"
LOG="$ROOT_M/logs"
mkdir -p "$LOG" "$ROOT_M/evidence"

dump() {
  local name="$1"
  adb shell "uiautomator dump /sdcard/inv.xml" >/dev/null 2>&1
  adb shell "uiautomator dump /sdcard/inv.xml" >/dev/null 2>&1
  adb shell "cat /sdcard/inv.xml" > "$LOG/inv-$name.xml" 2>/dev/null || true
  if [ -s "$LOG/inv-$name.xml" ]; then
    echo "  $name: ok"
  else
    echo "  $name: FAILED"
  fi
}

tap() { adb shell "input tap $1 $2" >/dev/null 2>&1; }

echo "restarting Apple Music"
adb shell "am force-stop com.apple.android.music"
adb shell "monkey -p com.apple.android.music -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1"
adb shell "uiautomator dump /sdcard/inv.xml" >/dev/null 2>&1

echo "capturing tabs"
for spec in "home 246 2980" "new 483 2980" "radio 720 2980" "library 957 2980" "search 1194 2980"; do
  set -- $spec
  tap "$2" "$3"
  dump "$1"
done

echo "capturing detail + player"
tap 246 2980
dump home_before_detail
tap 512 1217
dump detail
tap 720 2728
dump player
adb shell "input keyevent 4" >/dev/null 2>&1
tap 1324 288
dump more_menu

echo "summarizing"
python "$ROOT_M/tools/inventory.py"
