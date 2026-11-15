# Apple Music 控件普查（含 Compose）

采集：`bash tools/inventory.sh` → 逐页 `uiautomator dump` → `tools/inventory.py` 汇总。

坐标为屏幕绝对像素 `[x1,y1][x2,y2]`，设备 1440x3168。


## 覆盖页面

| 页面 | 节点 | ComposeView | Compose 语义控件 | 传统 View 控件 |
| --- | --- | --- | --- | --- |
| detail | 154 | 3 | 9 | 131 |
| home | 116 | 3 | 5 | 98 |
| home_before_detail | 116 | 3 | 5 | 98 |
| library | 112 | 3 | 30 | 54 |
| more_menu | 42 | 0 | 0 | 31 |
| new | 69 | 3 | 3 | 54 |
| player | 149 | 3 | 9 | 125 |
| radio | 115 | 3 | 25 | 54 |
| search | 132 | 3 | 5 | 109 |

## 所有 Compose 绘制的控件（按页面）

判定：父链含 `androidx.compose.ui.platform.ComposeView` 的 accessibility 节点。

| 页面 | 容器 | 语义 / 文案 | 类名 | 绝对 bounds | 尺寸 | 可点击 | id |
| --- | --- | --- | --- | --- | --- | --- | --- |
| detail | floating_app_bar_container | - | ComposeView | [0,0][1440,416] | 1440x416 | 否 | floating_app_bar_container |
| detail | floating_app_bar_container | - | View | [20,192][212,384] | 192x192 | 是 | - |
| detail | floating_app_bar_container | 向上浏览。 | View | [68,240][164,336] | 96x96 | 否 | - |
| detail | floating_app_bar_container | - | View | [1028,192][1220,384] | 192x192 | 是 | - |
| detail | floating_app_bar_container | 分享 | View | [1076,240][1172,336] | 96x96 | 否 | - |
| detail | floating_app_bar_container | - | View | [1228,192][1420,384] | 192x192 | 是 | - |
| detail | floating_app_bar_container | 更多选项 | View | [1268,232][1380,344] | 112x112 | 否 | - |
| detail | top_and_bottom_gradients | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | top_and_bottom_gradients |
| detail | dialogs_host | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | dialogs_host |
| home | dialogs_host | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | dialogs_host |
| home | top_and_bottom_gradients | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | top_and_bottom_gradients |
| home | floating_app_bar_container | - | ComposeView | [0,0][1440,476] | 1440x476 | 否 | floating_app_bar_container |
| home | floating_app_bar_container | - | View | [1184,208][1376,400] | 192x192 | 是 | - |
| home | floating_app_bar_container | 更多选项 | View | [1224,248][1336,360] | 112x112 | 否 | - |
| home_before_detail | dialogs_host | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | dialogs_host |
| home_before_detail | top_and_bottom_gradients | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | top_and_bottom_gradients |
| home_before_detail | floating_app_bar_container | - | ComposeView | [0,0][1440,476] | 1440x476 | 否 | floating_app_bar_container |
| home_before_detail | floating_app_bar_container | - | View | [1184,208][1376,400] | 192x192 | 是 | - |
| home_before_detail | floating_app_bar_container | 更多选项 | View | [1224,248][1336,360] | 112x112 | 否 | - |
| library | dialogs_host | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | dialogs_host |
| library | top_and_bottom_gradients | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | top_and_bottom_gradients |
| library | - | 资料库 | TextView | [80,224][976,390] | 896x166 | 否 | library_header |
| library | - | - | View | [976,211][1168,403] | 192x192 | 是 | - |
| library | - | 新建播放列表 | View | [1024,259][1120,355] | 96x96 | 否 | - |
| library | - | - | View | [1184,211][1376,403] | 192x192 | 是 | - |
| library | - | 更多选项 | View | [1224,251][1336,363] | 112x112 | 否 | - |
| library | - | - | View | [80,405][1360,570] | 1280x165 | 是 | - |
| library | - | 播放列表 | TextView | [260,418][1360,570] | 1100x152 | 否 | library_categories-播放列表 |
| library | - | - | View | [80,570][1360,735] | 1280x165 | 是 | - |
| library | - | 艺人 | TextView | [260,583][1360,735] | 1100x152 | 否 | library_categories-艺人 |
| library | - | - | View | [80,735][1360,900] | 1280x165 | 是 | - |
| library | - | 专辑 | TextView | [260,748][1360,900] | 1100x152 | 否 | library_categories-专辑 |
| library | - | - | View | [80,900][1360,1065] | 1280x165 | 是 | - |
| library | - | 歌曲 | TextView | [260,913][1360,1065] | 1100x152 | 否 | library_categories-歌曲 |
| library | - | - | View | [80,1065][1360,1257] | 1280x192 | 是 | - |
| library | - | 电视和影片 | TextView | [260,1078][1360,1241] | 1100x163 | 否 | library_categories-电视和影片 |
| library | - | 最近添加 | TextView | [80,1323][1360,1551] | 1280x228 | 否 | recently_added_header |
| library | - | - | View | [80,1551][698,2421] | 618x870 | 是 | - |
| library | - | 愛情諾曼底 | TextView | [80,2197][378,2270] | 298x73 | 否 | - |
| library | - | Shawn Huang | TextView | [80,2270][460,2341] | 380x71 | 否 | recently_added_subtitle-Shawn Huang |
| library | - | - | View | [746,1551][1360,2415] | 614x864 | 是 | - |
| library | - | Todas Mías... | TextView | [746,2193][1106,2264] | 360x71 | 否 | - |
| library | - | Leonel García | TextView | [746,2264][1121,2335] | 375x71 | 否 | recently_added_subtitle-Leonel García |
| library | - | - | View | [80,2421][698,3168] | 618x747 | 是 | - |
| library | - | Flight Number 001 (f… | TextView | [80,3067][692,3138] | 612x71 | 否 | - |
| library | - | FLY COAST | TextView | [80,3138][395,3168] | 315x30 | 否 | recently_added_subtitle-FLY COAST |
| library | - | - | View | [746,2421][1360,3168] | 614x747 | 是 | - |
| library | - | 幸福香巴拉 | TextView | [746,3063][1044,3136] | 298x73 | 否 | - |
| library | - | 索朗紮西 (Suo Langzhaxi) | TextView | [746,3136][1360,3168] | 614x32 | 否 | recently_added_subtitle-索朗紮西 (Suo Langzhaxi) |
| new | dialogs_host | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | dialogs_host |
| new | top_and_bottom_gradients | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | top_and_bottom_gradients |
| new | - | - | View | [0,0][1440,3168] | 1440x3168 | 是 | - |
| player | floating_app_bar_container | - | ComposeView | [0,0][1440,416] | 1440x416 | 否 | floating_app_bar_container |
| player | floating_app_bar_container | - | View | [20,192][212,384] | 192x192 | 是 | - |
| player | floating_app_bar_container | 向上浏览。 | View | [68,240][164,336] | 96x96 | 否 | - |
| player | floating_app_bar_container | - | View | [1028,192][1220,384] | 192x192 | 是 | - |
| player | floating_app_bar_container | 分享 | View | [1076,240][1172,336] | 96x96 | 否 | - |
| player | floating_app_bar_container | - | View | [1228,192][1420,384] | 192x192 | 是 | - |
| player | floating_app_bar_container | 更多选项 | View | [1268,232][1380,344] | 112x112 | 否 | - |
| player | top_and_bottom_gradients | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | top_and_bottom_gradients |
| player | dialogs_host | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | dialogs_host |
| radio | dialogs_host | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | dialogs_host |
| radio | top_and_bottom_gradients | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | top_and_bottom_gradients |
| radio | - | 广播 | TextView | [80,224][355,390] | 275x166 | 否 | new_header |
| radio | - | - | View | [1184,211][1376,403] | 192x192 | 是 | - |
| radio | - | 更多选项 | View | [1224,251][1336,363] | 112x112 | 否 | - |
| radio | - | - | View | [80,490][474,884] | 394x394 | 是 | - |
| radio | - | - | View | [522,490][917,885] | 395x395 | 是 | - |
| radio | - | - | View | [965,490][1360,885] | 395x395 | 是 | - |
| radio | - | - | View | [80,957][474,1351] | 394x394 | 是 | - |
| radio | - | - | View | [522,957][917,1352] | 395x395 | 是 | - |
| radio | - | - | View | [965,957][1360,1352] | 395x395 | 是 | - |
| radio | - | - | View | [80,1467][542,1641] | 462x174 | 是 | - |
| radio | - | On Air Now | TextView | [80,1484][542,1589] | 462x105 | 否 | - |
| radio | - | - | View | [80,1641][1248,2836] | 1168x1195 | 是 | - |
| radio | - | Apple Music 1 • 18:00–19:00 | TextView | [144,2476][769,2529] | 625x53 | 否 | - |
| radio | - | Rocket Hour | TextView | [144,2529][501,2600] | 357x71 | 否 | - |
| radio | - | King Charles joins Elton for a “musical voyage.” | TextView | [144,2600][1184,2751] | 1040x151 | 否 | - |
| radio | - | - | View | [1296,1641][1440,2836] | 144x1195 | 是 | - |
| radio | - | Apple Music Hits • 18:00–19:00 | TextView | [1360,2476][1440,2529] | 80x53 | 否 | - |
| radio | - | Rocket Hour | TextView | [1360,2529][1440,2600] | 80x71 | 否 | - |
| radio | - | King Charles joins Elton for a “musical voyage”. | TextView | [1360,2600][1440,2751] | 80x151 | 否 | - |
| radio | - | - | View | [80,2953][1082,3129] | 1002x176 | 是 | - |
| radio | - | Latest Radio Episodes � | TextView | [80,2968][1082,3077] | 1002x109 | 否 | - |
| radio | - | - | View | [80,3129][1296,3168] | 1216x39 | 是 | - |
| radio | - | - | View | [1296,3129][1440,3168] | 144x39 | 是 | - |
| search | dialogs_host | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | dialogs_host |
| search | top_and_bottom_gradients | - | ComposeView | [0,0][1440,3168] | 1440x3168 | 否 | top_and_bottom_gradients |
| search | floating_app_bar_container | - | ComposeView | [0,0][1440,412] | 1440x412 | 否 | floating_app_bar_container |
| search | floating_app_bar_container | - | View | [1184,208][1376,400] | 192x192 | 是 | - |
| search | floating_app_bar_container | 更多选项 | View | [1224,248][1336,360] | 112x112 | 否 | - |

## Compose 控件去重汇总

| 容器 | 语义 / 文案 | 类名 | 出现页面 | 示例 bounds |
| --- | --- | --- | --- | --- |
| floating_app_bar_container | - | ComposeView | detail, home, home_before_detail, player, search | [0,0][1440,416] |
| floating_app_bar_container | - | View | detail, home, home_before_detail, player, search | [20,192][212,384] |
| floating_app_bar_container | 向上浏览。 | View | detail, player | [68,240][164,336] |
| floating_app_bar_container | 分享 | View | detail, player | [1076,240][1172,336] |
| floating_app_bar_container | 更多选项 | View | detail, home, home_before_detail, player, search | [1268,232][1380,344] |
| top_and_bottom_gradients | - | ComposeView | detail, home, home_before_detail, library, new, player, radio, search | [0,0][1440,3168] |
| dialogs_host | - | ComposeView | detail, home, home_before_detail, library, new, player, radio, search | [0,0][1440,3168] |
| - | 资料库 | TextView | library | [80,224][976,390] |
| - | - | View | library, new, radio | [976,211][1168,403] |
| - | 新建播放列表 | View | library | [1024,259][1120,355] |
| - | 更多选项 | View | library, radio | [1224,251][1336,363] |
| - | 播放列表 | TextView | library | [260,418][1360,570] |
| - | 艺人 | TextView | library | [260,583][1360,735] |
| - | 专辑 | TextView | library | [260,748][1360,900] |
| - | 歌曲 | TextView | library | [260,913][1360,1065] |
| - | 电视和影片 | TextView | library | [260,1078][1360,1241] |
| - | 最近添加 | TextView | library | [80,1323][1360,1551] |
| - | 愛情諾曼底 | TextView | library | [80,2197][378,2270] |
| - | Shawn Huang | TextView | library | [80,2270][460,2341] |
| - | Todas Mías... | TextView | library | [746,2193][1106,2264] |
| - | Leonel García | TextView | library | [746,2264][1121,2335] |
| - | Flight Number 001 (f… | TextView | library | [80,3067][692,3138] |
| - | FLY COAST | TextView | library | [80,3138][395,3168] |
| - | 幸福香巴拉 | TextView | library | [746,3063][1044,3136] |
| - | 索朗紮西 (Suo Langzhaxi) | TextView | library | [746,3136][1360,3168] |
| - | 广播 | TextView | radio | [80,224][355,390] |
| - | On Air Now | TextView | radio | [80,1484][542,1589] |
| - | Apple Music 1 • 18:00–19:00 | TextView | radio | [144,2476][769,2529] |
| - | Rocket Hour | TextView | radio | [144,2529][501,2600] |
| - | King Charles joins Elton for a “musical voyage.” | TextView | radio | [144,2600][1184,2751] |
| - | Apple Music Hits • 18:00–19:00 | TextView | radio | [1360,2476][1440,2529] |
| - | King Charles joins Elton for a “musical voyage”. | TextView | radio | [1360,2600][1440,2751] |
| - | Latest Radio Episodes � | TextView | radio | [80,2968][1082,3077] |

## 传统 View 控件去重汇总

| id | 类名 | 语义 / 文案 | 绝对 bounds | 可点击 | 出现页面 |
| --- | --- | --- | --- | --- | --- |
| action_bar_root | FrameLayout | - | [0,0][1440,3168] | 否 | detail, home, home_before_detail, library, more_menu, new, player, radio, search |
| action_sheet_coordinator | ViewGroup | - | [0,0][1440,3168] | 否 | more_menu |
| action_sheet_list | RecyclerView | - | [0,1275][1440,3168] | 否 | more_menu |
| action_sheet_outside | View | - | [0,0][1440,3168] | 是 | more_menu |
| android:content | FrameLayout | - | [0,0][1440,3168] | 否 | detail, home, home_before_detail, library, more_menu, new, player, radio, search |
| artwork_area | FrameLayout | - | [64,1341][384,1661] | 否 | more_menu |
| artwork_radial_gradient | View | - | [0,384][1440,1946] | 否 | detail, player |
| artwork_shadow | View | - | [224,384][1216,1352] | 否 | detail, player |
| audio_badge_container | LinearLayout | - | [679,1564][1025,1624] | 否 | detail, player |
| audio_badge_icon | ImageView | - | [638,2232][694,2268] | 否 | player |
| audio_badge_text | TextView | 无损 | [714,2222][803,2278] | 否 | player |
| audio_badge_with_text | LinearLayout | - | [610,2208][831,2292] | 是 | player |
| background_layers | View | - | [0,0][1440,3168] | 否 | player |
| blur_target | FrameLayout | - | [0,0][1440,3168] | 否 | detail, home, home_before_detail, library, new, player, radio, search |
| bottom_nav_blur | FrameLayout | - | [80,2856][1360,3104] | 否 | detail, home, home_before_detail, library, new, radio, search |
| bottom_nav_view | FrameLayout | - | [80,2856][1360,3104] | 否 | detail, home, home_before_detail, library, new, radio, search |
| bubble_tip_parent | ViewGroup | - | [0,0][1440,821] | 否 | detail, player |
| button_action | FrameLayout | 下载播放列表 | [1104,1688][1296,1880] | 是 | detail, player |
| button_icon | ImageView | - | [196,1740][284,1828] | 否 | detail, player |
| button_label | TextView | 播放 | [682,1696][845,1872] | 否 | detail, player |
| button_play | LinearLayout | 播放 | [400,1688][1040,1880] | 是 | detail, player |
| button_shuffle | FrameLayout | 随机播放 | [144,1688][336,1880] | 是 | detail, player |
| buttons_container | ViewGroup | - | [1008,2664][1296,2792] | 否 | detail, home, home_before_detail, library, new, radio, search |
| caption_and_audio_badges_flexbox | ViewGroup | - | [64,1564][1376,1624] | 否 | detail, player |
| caption_badges_separator | TextView |  •  | [633,1568][679,1624] | 否 | detail, player |
| card | CardView | - | [1168,1880][1312,2024] | 否 | player |
| card_aspect_ratio | View | - | [80,641][944,1793] | 否 | home, home_before_detail |
| card_content | ViewGroup | - | [80,641][944,1793] | 是 | home, home_before_detail |
| collection_header_artist | TextView | Juan | [634,1448][806,1548] | 是 | detail, player |
| collection_header_caption | TextView | 2 周前更新 | [416,1568][633,1624] | 否 | detail, player |
| collection_header_image_view | ImageView | - | [288,424][1152,1288] | 否 | detail, player |
| collection_header_title | TextView | Favourite Songs   | [64,1348][1376,1448] | 否 | detail, player |
| collection_list_item | ViewGroup | - | [0,1946][1440,2170] | 是 | detail, player |
| collection_page_header | ViewGroup | - | [0,384][1440,1946] | 否 | detail, player |
| container | ViewGroup | - | [0,288][1440,3104] | 否 | player |
| content_container | ViewGroup | - | [0,0][1440,3168] | 否 | detail, home, home_before_detail, library, new, player, radio, search |
| controls | FrameLayout | - | [0,2132][1440,3104] | 否 | player |
| controls_tap_target | View | - | [0,2132][1440,3104] | 是 | player |
| current_player_item | LinearLayout | - | [0,288][1440,528] | 是 | player |
| current_time_progress | TextView | 0:00 | [128,2220][1312,2280] | 否 | player |
| divider | View | - | [64,1944][1376,1946] | 否 | detail, player |
| dummyView | TextView | 在 Apple Music 中搜索，编辑栏 | [80,433][1360,625] | 是 | search |
| explanation | TextView | 由你制作 | [144,1543][880,1603] | 否 | home, home_before_detail |
| explanation | TextView | 推荐 | [1056,1485][1440,1545] | 否 | home, home_before_detail |
| floating_search_bar | RelativeLayout | - | [0,393][1440,665] | 否 | search |
| floating_search_bar_blur | FrameLayout | - | [80,433][1360,625] | 否 | search |
| header_page_c_listen_now_container | ViewGroup | - | [80,224][1360,393] | 否 | home, home_before_detail, search |
| header_page_c_top_main_title | TextView | 主页 | [80,224][1168,393] | 否 | home, home_before_detail |
| header_page_c_top_main_title | TextView | 搜索 | [80,224][1168,393] | 否 | search |
| header_section_linked | LinearLayout | - | [0,421][1440,641] | 是 | home, home_before_detail |
| header_title_textview | TextView | 专属精选推荐 标题 | [80,485][552,585] | 否 | home, home_before_detail |
| header_title_textview | TextView | 最近播放   标题 | [80,1985][450,2085] | 否 | home, home_before_detail |
| header_title_textview | TextView | 乡村音乐 标题 | [80,3055][395,3155] | 否 | home, home_before_detail |
| hi_res_lossless_badge | TextView | 高解析度无损 | [679,1564][1025,1624] | 否 | detail, player |
| image | ImageView | - | [144,705][880,1441] | 否 | home, home_before_detail |
| image_alignment_container | FrameLayout | - | [80,2141][640,2701] | 否 | home, home_before_detail |
| image_and_motion_container | CardView | - | [144,705][880,1441] | 否 | home, home_before_detail |
| image_container | ViewGroup | - | [1168,1880][1312,2024] | 否 | player |
| image_view | ImageView | - | [64,1962][256,2154] | 否 | detail, player |
| image_view_shadow_container | CardView | - | [288,424][1152,1288] | 否 | detail, player |
| item_artwork_area | ViewGroup | - | [0,1962][256,2154] | 否 | detail, player |
| item_play_view | FrameLayout | - | [64,1962][256,2154] | 否 | detail, player |
| landing_search_recycler | RecyclerView | - | [0,0][1440,3168] | 否 | search |
| legibility_gradient_bottom | View | - | [992,929][1440,1793] | 否 | home, home_before_detail |
| list_favorite_icon | ImageView | 喜爱 | [960,288][1168,528] | 是 | player |
| list_favorite_icon_status | ImageView | - | [0,2026][64,2090] | 否 | detail, player |
| list_layout_container | LinearLayout | - | [0,1725][1440,1922] | 是 | more_menu |
| list_left_icon | ImageView | - | [64,1775][160,1871] | 否 | more_menu |
| list_left_icon | ImageView | 更多选项 | [1168,288][1376,528] | 是 | player |
| list_title | TextView | 排序方式 | [224,1781][1376,1866] | 否 | more_menu |
| list_title | TextView | 置顶播放列表 | [224,1978][1376,2063] | 否 | more_menu |
| list_title | TextView | 下载 | [224,2175][1376,2260] | 否 | more_menu |
| list_title | TextView | 添加到播放列表… | [224,2372][1376,2457] | 否 | more_menu |
| list_title | TextView | 插播 | [224,2569][1376,2654] | 否 | more_menu |
| list_title | TextView | 分享播放列表 | [224,2766][1376,2851] | 否 | more_menu |
| list_title | TextView | 撤销喜爱 | [224,2963][1376,3048] | 否 | more_menu |
| listen_now_container | FrameLayout | - | [0,0][1440,3168] | 否 | home, home_before_detail |
| listen_now_recycler | RecyclerView | - | [0,0][1440,3168] | 否 | home, home_before_detail |
| listen_now_refresh_layout | ViewGroup | - | [0,0][1440,3168] | 否 | home, home_before_detail |
| loading_progress | FrameLayout | - | [0,528][1440,2132] | 是 | player |
| lyrics_main_content | RecyclerView | - | [0,528][1440,3104] | 否 | player |
| lyrics_thumbnail_container | CardView | 专辑插图 | [128,288][368,528] | 是 | player |
| main_container | ViewGroup | - | [0,0][1440,3168] | 否 | detail, home, home_before_detail, library, new, player, radio, search |
| menu_action_button | ImageView | 歌曲菜单项 | [1288,1946][1440,2170] | 是 | detail, player |
| mini_player | FrameLayout | - | [0,2632][1440,3168] | 否 | detail, home, home_before_detail, library, new, radio, search |
| mini_player_artist | TextView | Shakira & Cardi B | [296,2730][1008,2790] | 否 | detail, home, home_before_detail, library, new, radio, search |
| mini_player_blur | FrameLayout | - | [80,2632][1360,2824] | 否 | detail, home, home_before_detail, library, new, radio, search |
| mini_player_content | ViewGroup | 正在播放：Puntería | [80,2632][1360,2824] | 是 | detail, home, home_before_detail, library, new, radio, search |
| mini_player_next_btn | ImageView | 下一个音轨 | [1168,2664][1296,2792] | 是 | detail, home, home_before_detail, library, new, radio, search |
| mini_player_play_btn | ImageView | 播放 | [1008,2664][1136,2792] | 是 | detail, home, home_before_detail, library, new, radio, search |
| mini_player_subtitle | TextView | Shakira & Cardi B | [400,408][933,493] | 是 | player |
| mini_player_title | TextView | Puntería | [296,2665][1008,2730] | 否 | detail, home, home_before_detail, library, new, player, radio, search |
| miniplayer_text_container | LinearLayout | - | [296,2632][1008,2824] | 否 | detail, home, home_before_detail, library, new, radio, search |
| nav_host_fragment_activity_main | FrameLayout | - | [0,0][1440,3168] | 否 | detail, home, home_before_detail, library, new, player, radio, search |
| navigation_bar_item_icon_container | FrameLayout | - | [186,2892][306,3012] | 否 | detail, home, home_before_detail, library, new, radio, search |
| navigation_bar_item_icon_view | ImageView | - | [186,2892][306,3012] | 否 | detail, home, home_before_detail, library, new, radio, search |
| navigation_bar_item_labels_group | ViewGroup | - | [202,3009][291,3104] | 否 | detail, home, home_before_detail, library, new, radio, search |
| navigation_bar_item_large_label_view | TextView | 主页 | [202,3009][291,3065] | 否 | detail, home, home_before_detail |
| navigation_bar_item_large_label_view | TextView | 资料库 | [891,3009][1024,3065] | 否 | library |
| navigation_bar_item_large_label_view | TextView | 新发现 | [417,3009][550,3065] | 否 | new |
| navigation_bar_item_large_label_view | TextView | 广播 | [676,3009][765,3065] | 否 | radio |
| navigation_bar_item_large_label_view | TextView | 搜索 | [1149,3009][1238,3065] | 否 | search |
| navigation_bar_item_small_label_view | TextView | 新发现 | [417,3009][550,3065] | 否 | detail, home, home_before_detail, library, radio, search |
| navigation_bar_item_small_label_view | TextView | 广播 | [676,3009][765,3065] | 否 | detail, home, home_before_detail, library, new, search |
| navigation_bar_item_small_label_view | TextView | 资料库 | [891,3009][1024,3065] | 否 | detail, home, home_before_detail, new, radio, search |
| navigation_bar_item_small_label_view | TextView | 搜索 | [1149,3009][1238,3065] | 否 | detail, home, home_before_detail, library, new, radio |
| navigation_bar_item_small_label_view | TextView | 主页 | [202,3009][291,3065] | 否 | library, new, radio, search |
| nested_nav_host_container | FrameLayout | - | [0,0][1440,3168] | 否 | detail, home, home_before_detail, library, new, player, radio, search |
| nested_nav_host_fragment | FrameLayout | - | [0,0][1440,3168] | 否 | detail, home, home_before_detail, library, new, player, radio, search |
| next_fast_forward | ImageView | 下一个音轨 | [1012,2442][1284,2686] | 是 | player |
| play_pause | ImageView | 播放 | [588,2432][852,2696] | 是 | player |
| playback_buttons | LinearLayout | - | [64,1688][1376,1880] | 是 | detail, player |
| player_controls | ViewGroup | - | [0,2132][1440,3104] | 是 | player |
| player_coordinator | ViewGroup | - | [0,0][1440,3168] | 否 | detail, home, home_before_detail, library, new, player, radio, search |
| player_fragments_host | FrameLayout | - | [0,0][1440,3104] | 否 | player |
| player_lyrics | ImageView | 查看完整歌词 | [188,2808][428,3048] | 是 | player |
| player_queue | ImageView | 播放器队列 | [1012,2808][1252,3048] | 是 | player |
| player_root | ViewGroup | - | [0,2632][1440,3168] | 否 | detail, home, home_before_detail, library, new, player, radio, search |
| player_sheet_container | FrameLayout | - | [0,2632][1440,3168] | 是 | detail, home, home_before_detail, library, new, player, radio, search |
| playlist_fragment_container | ScrollView | - | [0,0][1440,3168] | 否 | detail, player |
| playlist_fragment_root | FrameLayout | - | [0,0][1440,3168] | 否 | detail, player |
| power_swoosh_card_a | CardView | - | [80,641][944,1793] | 否 | home, home_before_detail |
| power_swoosh_card_b | CardView | - | [992,641][1440,1793] | 否 | home, home_before_detail |
| previous_rewind | ImageView | 上一个音轨 | [156,2442][428,2686] | 是 | player |
| progress | SeekBar | 0 分钟 0 秒钟经过时间3 分钟 1 秒钟时间长度 | [128,2132][1312,2200] | 否 | player |
| recycler_view | RecyclerView | - | [0,0][1440,3168] | 否 | detail, player |
| recycler_view_gradients | FrameLayout | - | [0,528][1440,3104] | 否 | player |
| search_bar | LinearLayout | - | [80,433][1360,625] | 否 | search |
| search_edit_frame | LinearLayout | - | [112,433][1328,625] | 否 | search |
| search_landing_tile_image | ImageView | King Charles III Meets Elton John | [80,697][696,1044] | 否 | search |
| search_landing_tile_image | ImageView | Hits | [744,697][1360,1044] | 否 | search |
| search_landing_tile_image | ImageView | Concerts | [80,1092][696,1439] | 否 | search |
| search_landing_tile_image | ImageView | Charts | [744,1092][1360,1439] | 否 | search |
| search_landing_tile_image | ImageView | Spatial Audio | [80,1487][696,1834] | 否 | search |
| search_landing_tile_image | ImageView | Local | [744,1487][1360,1834] | 否 | search |
| search_landing_tile_image | ImageView | DJ Mixes | [80,1882][696,2229] | 否 | search |
| search_landing_tile_image | ImageView | Apple Music Radio | [744,1882][1360,2229] | 否 | search |
| search_landing_tile_image | ImageView | Replay Monthly | [80,2277][696,2624] | 否 | search |
| search_landing_tile_image | ImageView | Apple Music Exclusives | [744,2277][1360,2624] | 否 | search |
| search_landing_tile_image | ImageView | Apple Music Live | [80,2672][696,3019] | 否 | search |
| search_landing_tile_image | ImageView | Fitness | [744,2672][1360,3019] | 否 | search |
| search_landing_tile_image | ImageView | Essentials | [80,3067][696,3168] | 否 | search |
| search_landing_tile_image | ImageView | Pop | [744,3067][1360,3168] | 否 | search |
| search_landing_tile_title | TextView | King Charles III Meets Elton John | [120,757][656,1004] | 否 | search |
| search_landing_tile_title | TextView | Hits | [784,919][1320,1004] | 否 | search |
| search_landing_tile_title | TextView | Concerts | [120,1314][656,1399] | 否 | search |
| search_landing_tile_title | TextView | Charts | [784,1314][1320,1399] | 否 | search |
| search_landing_tile_title | TextView | Spatial Audio | [120,1709][656,1794] | 否 | search |
| search_landing_tile_title | TextView | Local | [784,1709][1320,1794] | 否 | search |
| search_landing_tile_title | TextView | DJ Mixes | [120,2104][656,2189] | 否 | search |
| search_landing_tile_title | TextView | Apple Music Radio | [784,2023][1320,2189] | 否 | search |
| search_landing_tile_title | TextView | Replay Monthly | [120,2499][656,2584] | 否 | search |
| search_landing_tile_title | TextView | Apple Music Exclusives | [784,2418][1320,2584] | 否 | search |
| search_landing_tile_title | TextView | Apple Music Live | [120,2894][656,2979] | 否 | search |
| search_landing_tile_title | TextView | Fitness | [784,2894][1320,2979] | 否 | search |
| search_plate | LinearLayout | - | [112,433][1328,625] | 否 | search |
| search_src_text | AutoCompleteTextView | 艺人、歌曲、歌词以及更多内容 | [112,457][1328,601] | 是 | search |
| search_view_background | RelativeLayout | - | [80,433][1360,625] | 否 | search |
| search_view_background_stroke | FrameLayout | - | [80,433][1360,625] | 否 | search |
| searchview | LinearLayoutCompat | - | [80,433][1360,625] | 否 | search |
| seek_bar_controls | LinearLayout | - | [0,2132][1440,2320] | 是 | player |
| subtitle | TextView | Danny Chan | [304,2065][1288,2137] | 否 | detail, player |
| subtitle | TextView | Sam Hunt | [304,2289][1288,2361] | 否 | detail, player |
| subtitle | TextView | David Stephen Goldsmith, Andrew Michael Britton, Wayne Antho | [304,2513][1288,2585] | 否 | detail, player |
| subtitle | TextView | Lady Gaga | [304,2737][1288,2809] | 否 | detail, player |
| subtitle | TextView | Khalil Fong | [304,2961][1288,3033] | 否 | detail, player |
| subtitleView | TextView | 所有激励你追逐梦想的歌曲。 | [144,1677][880,1737] | 否 | home, home_before_detail |
| subtitleView | TextView | This year, it was “So Easy (To Fall in Love)” with Olivia Dean. | [1056,1619][1440,1737] | 否 | home, home_before_detail |
| subtitleView | TextView | 杨羊烊. | [80,2804][640,2879] | 否 | home, home_before_detail |
| subtitleView | TextView | 广播电台 | [688,2804][1248,2879] | 否 | home, home_before_detail |
| subtitleView | TextView | Marshmello & Thomas Rhett | [1296,2804][1440,2879] | 否 | home, home_before_detail |
| swiping_grid_a_standard_container | ViewGroup | - | [80,2141][640,2927] | 是 | home, home_before_detail |
| tab_navigation_host | FrameLayout | - | [0,0][1440,3168] | 否 | detail, home, home_before_detail, library, new, player, radio, search |
| text_area | LinearLayout | - | [448,1458][1376,1543] | 否 | more_menu |
| text_metadata_container | LinearLayout | - | [368,323][960,493] | 否 | player |
| thumbnail | ImageView | - | [128,288][368,528] | 否 | player |
| title | TextView | 偏偏喜歡你 | [304,1980][1288,2065] | 否 | detail, player |
| title | TextView | Breaking Up Was Easy in the 90's | [304,2204][1288,2289] | 否 | detail, player |
| title | TextView | In The Summer | [304,2428][1288,2513] | 否 | detail, player |
| title | TextView | Vanish Into You | [304,2652][1288,2737] | 否 | detail, player |
| title | TextView | 特別的人 | [304,2876][1288,2961] | 否 | detail, player |
| title | TextView | Half Of Me (feat. Riley Green) | [304,3100][1288,3168] | 否 | detail, player |
| titleView | TextView | Favourite Songs | [144,1607][880,1677] | 否 | home, home_before_detail |
| titleView | TextView | Today’s Hits: Best of 2026 | [1056,1549][1440,1619] | 否 | home, home_before_detail |
| titleView | TextView | 烂奇迹(不小心惹了上帝) | [80,2729][640,2804] | 否 | home, home_before_detail |
| titleView | TextView | Juan’s Station | [688,2729][1248,2804] | 否 | home, home_before_detail |
| titleView | TextView | Where We Go - Single | [1296,2729][1440,2804] | 否 | home, home_before_detail |
| top_level_home_page | FrameLayout | 主页 | [128,2856][365,3104] | 否 | detail, home, home_before_detail, library, new, radio, search |
| top_level_library_page | FrameLayout | 资料库 | [839,2856][1076,3104] | 是 | detail, home, home_before_detail, library, new, radio, search |
| top_level_new_page | FrameLayout | 新发现 | [365,2856][602,3104] | 是 | detail, home, home_before_detail, library, new, radio, search |
| top_level_radio_page | FrameLayout | 广播 | [602,2856][839,3104] | 是 | detail, home, home_before_detail, library, new, radio, search |
| top_level_search_page | FrameLayout | 搜索 | [1076,2856][1312,3104] | 是 | detail, home, home_before_detail, library, new, radio, search |
| total_time_progress | TextView | -3:01 | [128,2220][1312,2280] | 否 | player |
| video_surface | FrameLayout | - | [144,2668][264,2788] | 否 | detail, home, home_before_detail, library, new, radio, search |
| video_surface | TextureView | - | [144,2668][264,2788] | 否 | detail, home, home_before_detail, library, new, radio, search |
| video_surface | ImageView | - | [144,2668][264,2788] | 否 | detail, home, home_before_detail, library, new, radio, search |
| video_surface_container | FrameLayout | - | [144,2668][264,2788] | 否 | detail, home, home_before_detail, library, new, radio, search |
| video_texture | TextureView | - | [992,641][1440,1793] | 否 | home, home_before_detail |
| vocal_ctrl | FrameLayout | 人声音量关闭 | [1128,1840][1440,2132] | 是 | player |
| vocal_ctrl_drag_limits | View | - | [1128,1840][1440,2132] | 否 | player |
| vocal_off | ImageView | - | [1168,1880][1312,2024] | 否 | player |
| - | RelativeLayout | 正在显示“Favourite Songs”的选项。 | [0,1277][1440,1725] | 是 | more_menu |
| - | ImageView | 播放 | [116,1393][332,1609] | 是 | more_menu |
| - | TextView | Favourite Songs | [448,1458][977,1543] | 否 | more_menu |
| - | FrameLayout | - | [80,697][696,1044] | 是 | search |

## 边界

- accessibility 树只覆盖遍历到的页面与当时可见、已渲染的节点；折叠语义
  （`mergeDescendants`）的 Composable 会被合并成一个节点，看不到更细的内部结构。

- 弹窗、菜单、横屏、深色模式、滚动后的列表项需要另跑一遍 `inventory.sh` 补采。

- 模块侧 `logs/amod-*.log` 的 `CTRL` 行给的是真实 View 树 + 绝对 bounds，
  与这里的 accessibility 树互补：改样式时以模块日志的 `id=` 为 hook 依据。

