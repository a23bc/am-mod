#!/usr/bin/env bash
# 抓一次真机证据：当前界面层级 + AMMod 日志，统一落到 logs/。
# 用法：bash tools/capture.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/logs"
STAMP="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"

# 注意：整条 adb shell 命令必须放在引号里，否则 Git Bash(MSYS) 会把
# /sdcard/... 改写成 Windows 路径，uiautomator dump 会静默失败。
adb shell "uiautomator dump /sdcard/am-window.xml" >/dev/null
adb shell "cat /sdcard/am-window.xml" > "$OUT/ui-$STAMP.xml"
adb logcat -d -s AMMod:D > "$OUT/amod-$STAMP.log"

loaded=0; trees=0; hits=0; total=0
while IFS= read -r line; do
  total=$((total + 1))
  case "$line" in
    *"module loaded"*)      loaded=$((loaded + 1)) ;;
    *"view tree begin"*)    trees=$((trees + 1)) ;;
    *"highlight "*"tinted"*) hits=$((hits + 1)) ;;
  esac
done < "$OUT/amod-$STAMP.log"

echo "AMMod lines     : $total"
echo "module loaded   : $loaded   (0 = 作用域/启用没生效)"
echo "view tree dumps : $trees   (0 = onResume hook 没命中)"
echo "highlight hits  : $hits    (0 = 资源 id 对不上)"
echo "wrote:"
echo "  $OUT/ui-$STAMP.xml"
echo "  $OUT/amod-$STAMP.log"
