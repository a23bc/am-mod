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

loaded=0; ctrls=0; styles=0; compose=0; errors=0; total=0
while IFS= read -r line; do
  total=$((total + 1))
  case "$line" in
    *"module loaded"*)     loaded=$((loaded + 1)) ;;
    *"CTRL "*)             ctrls=$((ctrls + 1)) ;;
    *"STYLE "*)            styles=$((styles + 1)) ;;
    *"COMPOSE_VIEW "*)     compose=$((compose + 1)) ;;
    *"error "*)            errors=$((errors + 1)) ;;
  esac
done < "$OUT/amod-$STAMP.log"

echo "AMMod lines       : $total"
echo "module loaded     : $loaded  (0 = 模块没启用 / 作用域没勾)"
echo "control entries   : $ctrls  (0 = addView hook 没命中)"
echo "styles applied    : $styles (v0.3.0 普查模式应为 0)"
echo "compose views     : $compose (模块侧看到的 Compose 容器次数)"
echo "errors            : $errors (应为 0)"
echo "wrote:"
echo "  $OUT/ui-$STAMP.xml"
echo "  $OUT/amod-$STAMP.log"
