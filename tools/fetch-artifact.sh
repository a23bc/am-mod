#!/usr/bin/env bash
# 取指定 GitHub Actions run 的 am-mod-debug 产物，解压到 tools/artifacts/。
# 用法：bash tools/fetch-artifact.sh <run_id>
#
# 为什么不用 gh run download：本机代理对 api.github.com 的 HTTP/2 连接不稳，
# gh（Go 客户端）会报 EOF；curl --http1.1 稳定。302 之后的 Azure Blob 请求
# 不能带 Authorization（预签名 URL 自带鉴权，双认证会被拒）。
#
# cygpath -m 把路径转成 E:/... 形式：Windows 原生 python 不认 Git Bash 的 /e/... 路径。
set -euo pipefail

RUN_ID="${1:-}"
if [ -z "$RUN_ID" ]; then
  echo "usage: bash tools/fetch-artifact.sh <run_id>" >&2
  exit 2
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$(cygpath -m "$ROOT/tools/artifacts")"
mkdir -p "$OUT"

TOKEN="$(gh auth token)"
API="https://api.github.com/repos/a23bc/am-mod/actions"

ART_ID="$(curl -sS --http1.1 -H "Authorization: Bearer $TOKEN" \
  "$API/runs/$RUN_ID/artifacts" \
  | python -c "import sys,json; a=json.load(sys.stdin)['artifacts']; m=[x for x in a if x['name']=='am-mod-debug']; print(m[0]['id'] if m else '')")"
if [ -z "$ART_ID" ]; then
  echo "artifact am-mod-debug not found in run $RUN_ID" >&2
  exit 1
fi

LOC="$(curl -sS --http1.1 -D - -o /dev/null -H "Authorization: Bearer $TOKEN" \
  "$API/artifacts/$ART_ID/zip" \
  | tr -d '\r' | sed -n 's/^[Ll]ocation: //p' | head -1)"
if [ -z "$LOC" ]; then
  echo "no 302 Location for artifact $ART_ID" >&2
  exit 1
fi

curl -sS --http1.1 -L -o "$OUT/am-mod-debug.zip" "$LOC"
python -c "import zipfile,sys; zipfile.ZipFile(sys.argv[1]).extractall(sys.argv[2])" \
  "$OUT/am-mod-debug.zip" "$OUT"
echo "wrote $OUT/app-debug.apk"
