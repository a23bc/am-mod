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
 * v0.2.2 —— 基础交互控件版。
 *
 * 目标：跨页面覆盖「返回、分享、更多、播放、收藏、三点菜单」这类基础交互控件，
 * 明确排除 mini player 与底部导航。
 *
 * 三条触发路径，互为兜底：
 *   1. ViewGroup.addView —— 新建 View（含列表滚动、弹窗）；
 *   2. Activity.onResume —— 冷启动时整棵树扫一次；
 *   3. 1s 轮询 DecorView —— Fragment 切换常复用旧 View（不再 addView），
 *      而 Apple Music 把 AndroidX Fragment 类重打包了，实测
 *      ClassNotFoundException: androidx.fragment.app.Fragment，没法 hook 它。
 *      轮询是明知粗糙但可验证的兜底，靠 SEEN 去重保证不重复动手。
 *
 * 已知边界：顶部工具栏的返回/分享/更多按钮由 ComposeView
 * (floating_app_bar_container) 绘制，不是独立 View，本版只能标记容器，
 * 改不到它们的视觉样式。详见 README。
 */
public class HookMain implements IXposedHookLoadPackage {

    private static final String TAG = "AMMod";
    private static final String TARGET = "com.apple.android.music";
    private static final String VERSION = "v0.2.2";

    private static final boolean DUMP_CONTROLS = true;
    private static final boolean APPLY_STYLE = true;
    private static final long TICKER_MS = 1000L;

    private static final Set<String> EXCLUDED_PREFIX = new HashSet<>(Arrays.asList(
            "mini_player_", "navigation_bar_item_", "top_level_", "player_sheet_"
    ));

    private static final Set<Integer> SEEN = new HashSet<>();
    private static long sLastScan = 0L;
    private static boolean sTickerStarted = false;

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
        try {
            walk(root, 0);
        } catch (Throwable t) {
            log("scan error: " + t);
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

        if ("floating_app_bar_container".equals(id)) {
            log("COMPOSE_TOOLBAR " + v.getClass().getName()
                    + " size=" + v.getWidth() + "x" + v.getHeight()
                    + " depth=" + depth);
            return;
        }

        boolean clickable = v.isClickable() || v.isLongClickable();
        boolean imageLike = v instanceof ImageView || v instanceof ImageButton || v instanceof Button;
        if (!clickable && !imageLike) {
            return;
        }
        int w = v.getWidth();
        int h = v.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float density = v.getResources().getDisplayMetrics().density;
        if (w > 240 * density || h > 120 * density) {
            return;
        }

        boolean fresh = SEEN.add(System.identityHashCode(v));
        if (DUMP_CONTROLS && fresh) {
            log("CTRL " + v.getClass().getName()
                    + " id=" + id
                    + " click=" + v.isClickable()
                    + " desc=" + desc(v)
                    + " size=" + w + "x" + h
                    + " depth=" + depth);
        }
        if (APPLY_STYLE && fresh) {
            applyStyle(v, id, density);
        }
    }

    /** 占位样式：圆角描边 + 极淡底色，用 overlay 叠加，不动原 background / ripple。 */
    private static void applyStyle(View v, String id, float density) {
        try {
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
