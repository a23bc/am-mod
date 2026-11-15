package com.a23bc.ammod;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * v0.4.0 —— 控件普查 + 顶部工具栏液态玻璃预览。
 *
 * 普查：只输出每个控件的真实 View 身份（id / 类名 / 屏幕绝对 bounds /
 * clickable / content-desc），不改传统 View 控件（APPLY_STYLE=false）。
 *
 * 预览：返回 / 分享 / 更多选项是 Compose 绘制的，不是独立 View，
 * 改不了属性；改为在 DecorView 上叠三个自绘 View，用 Kyant0/AndroidLiquidGlass
 * 同款 AGSL shader 做玻璃高光与边缘折射，触摸穿透保留原按钮行为。见 GlassToolbar。
 *
 * 三条触发路径互为兜底：
 *   1. ViewGroup.addView —— 新建 View；
 *   2. Activity.onResume —— 冷启动整棵树；
 *   3. 1s 轮询 DecorView —— Fragment 切换复用旧 View（不再 addView）。
 *      Apple Music 重打包了 AndroidX，找不到 androidx.fragment.app.Fragment，
 *      没法 hook Fragment，所以保留这个粗糙但可验证的轮询。
 */
public class HookMain implements IXposedHookLoadPackage {

    private static final String TAG = "AMMod";
    private static final String TARGET = "com.apple.android.music";
    private static final String VERSION = "v0.4.0";

    private static final boolean DUMP_CONTROLS = true;
    // 传统 View 控件仍保持原样，先只做顶部三个按钮的玻璃预览。
    private static final boolean APPLY_STYLE = false;
    private static final boolean ENABLE_GLASS = true;
    private static final long TICKER_MS = 1000L;

    private static final Set<String> EXCLUDED_PREFIX = new HashSet<>(Arrays.asList(
            "mini_player_", "navigation_bar_item_", "top_level_", "player_sheet_"
    ));

    private static final Set<Integer> SEEN = new HashSet<>();
    private static long sLastScan = 0L;
    private static boolean sTickerStarted = false;
    private static boolean sGlassPosted = false;
    private static boolean sPageHasToolbar = false;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET.equals(lpparam.packageName)) {
            return;
        }
        log("module loaded in " + lpparam.processName + " (" + VERSION + ")");

        XposedBridge.hookAllMethods(ViewGroup.class, "addView", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    if (param.args.length == 0 || !(param.args[0] instanceof View)) {
                        return;
                    }
                    final View child = (View) param.args[0];
                    child.post(new Runnable() {
                        @Override
                        public void run() {
                            scan(child, false);
                        }
                    });
                } catch (Throwable t) {
                    log("addView hook error: " + t);
                }
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    final View decor = ((Activity) param.thisObject).getWindow().getDecorView();
                    decor.post(new Runnable() {
                        @Override
                        public void run() {
                            scan(decor, true);
                        }
                    });
                    startTicker(decor);
                } catch (Throwable t) {
                    log("onResume hook error: " + t);
                }
            }
        });
    }

    private static void startTicker(final View decor) {
        if (sTickerStarted) {
            return;
        }
        sTickerStarted = true;
        decor.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (decor.isAttachedToWindow() && decor.getWindowVisibility() == View.VISIBLE) {
                    scan(decor, true);
                }
                decor.postDelayed(this, TICKER_MS);
            }
        }, TICKER_MS);
        log("ticker started (" + TICKER_MS + "ms)");
    }

    private static void scan(View root, boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - sLastScan < 300L) {
            return;
        }
        sLastScan = now;
        if (force) {
            sPageHasToolbar = false;
        }
        try {
            walk(root, 0);
        } catch (Throwable t) {
            log("scan error: " + t);
        }
        if (force) {
            GlassToolbar.setVisible(sPageHasToolbar);
        }
    }

    /** 每个节点单独 try/catch：v0.1.0 就是被一个异常节点打断了整棵树。 */
    private static void walk(View v, int depth) {
        try {
            handle(v, depth);
        } catch (Throwable t) {
            log("node error " + v.getClass().getName() + ": " + t);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            int count;
            try {
                count = g.getChildCount();
            } catch (Throwable t) {
                return;
            }
            for (int i = 0; i < count; i++) {
                View c;
                try {
                    c = g.getChildAt(i);
                } catch (Throwable t) {
                    continue;
                }
                if (c != null) {
                    walk(c, depth + 1);
                }
            }
        }
    }

    private static void handle(View v, int depth) {
        String id = idName(v);
        if (isExcluded(id)) {
            return;
        }

        // 这些 id 只出现在带顶部工具栏（返回 / 分享 / 更多选项）的页面：
        // 歌单详情、全屏播放页。主页没有，用它控制玻璃层显隐。
        if ("collection_header_artist".equals(id) || "button_play".equals(id)
                || "play_pause".equals(id) || "player_controls".equals(id)
                || "menu_action_button".equals(id)) {
            sPageHasToolbar = true;
        }

        if (v.getClass().getName().contains("compose.ui.platform")
                || "floating_app_bar_container".equals(id)) {
            // 轮询每秒都会经过这里，必须去重，否则 COMPOSE_VIEW 会刷爆 logcat。
            if (SEEN.add(System.identityHashCode(v))) {
                int[] lc = location(v);
                log("COMPOSE_VIEW " + v.getClass().getName()
                        + " id=" + id
                        + " abs=[" + lc[0] + "," + lc[1] + "]["
                        + (lc[0] + v.getWidth()) + "," + (lc[1] + v.getHeight()) + "]"
                        + " depth=" + depth);
            }
            // 工具栏有尺寸时才装玻璃层；post 出去避免在遍历 View 树时 addView。
            if (ENABLE_GLASS && !sGlassPosted
                    && "floating_app_bar_container".equals(id)
                    && v.getWidth() > 0 && v.getHeight() > 0) {
                sGlassPosted = true;
                final View decor = v.getRootView();
                final View toolbar = v;
                decor.post(new Runnable() {
                    @Override
                    public void run() {
                        GlassToolbar.install(decor, toolbar);
                    }
                });
            }
            return;
        }

        boolean clickable = v.isClickable() || v.isLongClickable();
        boolean imageLike = v instanceof ImageView || v instanceof ImageButton || v instanceof Button;
        boolean hasId = !"-".equals(id);
        String d = desc(v);
        boolean hasDesc = !"-".equals(d);
        if (!clickable && !imageLike && !hasId && !hasDesc) {
            return;
        }
        int w = v.getWidth();
        int h = v.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        boolean fresh = SEEN.add(System.identityHashCode(v));
        if (DUMP_CONTROLS && fresh) {
            int[] lc = location(v);
            log("CTRL " + v.getClass().getName()
                    + " id=" + id
                    + " click=" + v.isClickable()
                    + " desc=" + d
                    + " abs=[" + lc[0] + "," + lc[1] + "]["
                    + (lc[0] + w) + "," + (lc[1] + h) + "]"
                    + " size=" + w + "x" + h
                    + " depth=" + depth);
        }
        if (APPLY_STYLE && fresh) {
            applyStyle(v, id);
        }
    }

    /** 保留样式实现，但普查阶段不调用；定下样式方向后把 APPLY_STYLE 打开即可。 */
    private static void applyStyle(View v, String id) {
        try {
            float density = v.getResources().getDisplayMetrics().density;
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.RECTANGLE);
            g.setCornerRadius(14 * density);
            g.setStroke(Math.max(1, (int) (2 * density)), Color.argb(190, 120, 190, 255));
            g.setColor(Color.argb(28, 120, 190, 255));
            g.setBounds(0, 0, v.getWidth(), v.getHeight());
            v.getOverlay().add(g);
            log("STYLE " + id + " " + v.getWidth() + "x" + v.getHeight());
        } catch (Throwable t) {
            log("style error " + id + ": " + t);
        }
    }

    private static int[] location(View v) {
        int[] lc = new int[2];
        try {
            v.getLocationOnScreen(lc);
        } catch (Throwable ignored) {
        }
        return lc;
    }

    private static boolean isExcluded(String id) {
        for (String prefix : EXCLUDED_PREFIX) {
            if (id.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String idName(View v) {
        int id = v.getId();
        if (id == View.NO_ID) {
            return "-";
        }
        try {
            return v.getResources().getResourceEntryName(id);
        } catch (Throwable t) {
            return "0x" + Integer.toHexString(id);
        }
    }

    private static String desc(View v) {
        try {
            CharSequence c = v.getContentDescription();
            return c == null ? "-" : c.toString().replace('\n', ' ');
        } catch (Throwable t) {
            return "?";
        }
    }

    private static void log(String msg) {
        XposedBridge.log(TAG + ": " + msg);
        Log.d(TAG, msg);
    }
}
