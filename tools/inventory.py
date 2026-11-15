#!/usr/bin/env python3
"""汇总 tools/inventory.sh 导出的 accessibility 树，产出控件位置清单。

输出 evidence/inventory.md（人读）与 evidence/inventory.json（结构化）。
父链含 androidx.compose.ui.platform.ComposeView 的节点即判定为 Compose 绘制。
"""
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
LOGDIR = ROOT / "logs"
OUT = ROOT / "evidence"
NUM = re.compile(r"-?\d+")


def bounds(s):
    nums = [int(x) for x in NUM.findall(s or "")[:4]]
    return nums if len(nums) == 4 else [0, 0, 0, 0]


def short(s):
    return (s or "").replace("com.apple.android.music:id/", "").replace("android:id/", "android:")


def is_control(n):
    return (n.get("clickable") == "true" or n.get("long-clickable") == "true"
            or bool(n.get("resource-id")) or bool(n.get("content-desc"))
            or bool(n.get("text")))


def fmt(b):
    return "[%d,%d][%d,%d]" % tuple(b)


def esc(s):
    return str(s).replace("|", "\\|").replace("\n", " ")


rows, pages = [], []
for f in sorted(LOGDIR.glob("inv-*.xml")):
    page = f.stem[4:]
    tree = ET.parse(f)
    parent = {c: p for p in tree.iter() for c in p}
    nodes = list(tree.iter("node"))
    stat = dict(page=page, nodes=len(nodes), compose_views=0,
                compose_controls=0, view_controls=0)
    for n in nodes:
        cls = n.get("class") or ""
        if "ComposeView" in cls:
            stat["compose_views"] += 1
        chain, cur = [], n
        while cur is not None and cur.get("class") is not None:
            chain.append(cur)
            cur = parent.get(cur)
        cv = next((x for x in chain if "ComposeView" in (x.get("class") or "")), None)
        compose = cv is not None
        if not is_control(n):
            continue
        b = bounds(n.get("bounds"))
        rows.append(dict(
            page=page, cls=cls.split(".")[-1], cls_full=cls,
            id=short(n.get("resource-id")), desc=n.get("content-desc") or "",
            text=(n.get("text") or "")[:60], bounds=b,
            w=b[2] - b[0], h=b[3] - b[1],
            clickable=n.get("clickable") == "true", compose=compose,
            container=short(cv.get("resource-id")) if cv is not None else "",
            depth=len(chain),
        ))
        stat["compose_controls" if compose else "view_controls"] += 1
    pages.append(stat)

OUT.mkdir(parents=True, exist_ok=True)
(OUT / "inventory.json").write_text(
    json.dumps(dict(pages=pages, controls=rows), ensure_ascii=False, indent=2),
    encoding="utf-8")


def key(r):
    return (r["id"], r["desc"], r["text"], r["cls_full"])


compose_rows = [r for r in rows if r["compose"]]
view_rows = [r for r in rows if not r["compose"]]

L = []
L.append("# Apple Music 控件普查（含 Compose）\n")
L.append("采集：`bash tools/inventory.sh` → 逐页 `uiautomator dump` → `tools/inventory.py` 汇总。\n")
L.append("坐标为屏幕绝对像素 `[x1,y1][x2,y2]`，设备 1440x3168。\n")
L.append("\n## 覆盖页面\n")
L.append("| 页面 | 节点 | ComposeView | Compose 语义控件 | 传统 View 控件 |")
L.append("| --- | --- | --- | --- | --- |")
for s in pages:
    L.append("| %s | %d | %d | %d | %d |" % (
        s["page"], s["nodes"], s["compose_views"],
        s["compose_controls"], s["view_controls"]))

L.append("\n## 所有 Compose 绘制的控件（按页面）\n")
L.append("判定：父链含 `androidx.compose.ui.platform.ComposeView` 的 accessibility 节点。\n")
L.append("| 页面 | 容器 | 语义 / 文案 | 类名 | 绝对 bounds | 尺寸 | 可点击 | id |")
L.append("| --- | --- | --- | --- | --- | --- | --- | --- |")
for r in compose_rows:
    L.append("| %s | %s | %s | %s | %s | %dx%d | %s | %s |" % (
        r["page"], esc(r["container"] or "-"), esc(r["desc"] or r["text"] or "-"),
        esc(r["cls"]), fmt(r["bounds"]), r["w"], r["h"],
        "是" if r["clickable"] else "否", esc(r["id"] or "-")))

L.append("\n## Compose 控件去重汇总\n")
seen = {}
for r in compose_rows:
    seen.setdefault((r["container"], r["desc"] or r["text"], r["cls"]), []).append(r)
L.append("| 容器 | 语义 / 文案 | 类名 | 出现页面 | 示例 bounds |")
L.append("| --- | --- | --- | --- | --- |")
for (container, label, cls), rs in seen.items():
    L.append("| %s | %s | %s | %s | %s |" % (
        esc(container or "-"), esc(label or "-"), esc(cls),
        ", ".join(sorted({x["page"] for x in rs})), fmt(rs[0]["bounds"])))

L.append("\n## 传统 View 控件去重汇总\n")
vseen = {}
for r in view_rows:
    vseen.setdefault(key(r), []).append(r)
L.append("| id | 类名 | 语义 / 文案 | 绝对 bounds | 可点击 | 出现页面 |")
L.append("| --- | --- | --- | --- | --- | --- |")
for (i, d, t, cls), rs in sorted(vseen.items(), key=lambda kv: kv[0][0] or "zzz"):
    r = rs[0]
    L.append("| %s | %s | %s | %s | %s | %s |" % (
        esc(i or "-"), esc(cls.split(".")[-1]), esc(d or t or "-"),
        fmt(r["bounds"]), "是" if r["clickable"] else "否",
        ", ".join(sorted({x["page"] for x in rs}))))

L.append("\n## 边界\n")
L.append("- accessibility 树只覆盖遍历到的页面与当时可见、已渲染的节点；折叠语义")
L.append("  （`mergeDescendants`）的 Composable 会被合并成一个节点，看不到更细的内部结构。\n")
L.append("- 弹窗、菜单、横屏、深色模式、滚动后的列表项需要另跑一遍 `inventory.sh` 补采。\n")
L.append("- 模块侧 `logs/amod-*.log` 的 `CTRL` 行给的是真实 View 树 + 绝对 bounds，")
L.append("  与这里的 accessibility 树互补：改样式时以模块日志的 `id=` 为 hook 依据。\n")

(OUT / "inventory.md").write_text("\n".join(L) + "\n", encoding="utf-8")
print("pages:", len(pages), "controls:", len(rows), "compose:", len(compose_rows))
print("wrote:", OUT / "inventory.md", OUT / "inventory.json")
