package com.a23bc.ammod;

import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 顶部 Compose 工具栏的液态玻璃叠加层。
 *
 * 返回 / 分享 / 更多选项不是独立 View，改不了属性；
 * 所以在 DecorView 上按 evidence/inventory.md 的实测 bounds 叠三个自绘 View，
 * 用 Kyant0/AndroidLiquidGlass 同款 shader 做玻璃高光与边缘折射，
 * 保持触摸穿透让原按钮照常工作。
 *
 * 安装时机由 HookMain 轮询重试：ComposeView 的尺寸在 Fragment 切换时会短暂变 0，
 * 一次失败不能放弃。按钮常显（顶部三个位置在主页也有原生按钮容器，不突兀）。
 * 坐标按 1440x3168 基线等比缩放。
 */
public class GlassToolbar {

    private static final String TAG = "AMMod";
    private static final Set<Integer> INSTALLED = new HashSet<>();
    private static final List<GlassButtonView> BUTTONS = new ArrayList<>();

    private final View mDecor;
    private final View mToolbar;

    static boolean isInstalled() {
        return !BUTTONS.isEmpty();
    }

    static void install(View decor, View toolbar) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "glass skipped: need API 33+, got " + Build.VERSION.SDK_INT);
            return;
        }
        if (isInstalled() || !(decor instanceof ViewGroup) || toolbar.getWidth() <= 0) {
            return;
        }
        int key = System.identityHashCode(decor);
        synchronized (INSTALLED) {
            if (!INSTALLED.add(key)) {
                return;
            }
        }
        try {
            new GlassToolbar(decor, toolbar).start();
            Log.d(TAG, "glass installed: 3 buttons on floating toolbar");
        } catch (Throwable t) {
            synchronized (INSTALLED) {
                INSTALLED.remove(key);
            }
            Log.d(TAG, "glass install error: " + t);
        }
    }

    private GlassToolbar(View decor, View toolbar) {
        mDecor = decor;
        mToolbar = toolbar;
    }

    private void start() {
        int[] loc = new int[2];
        mToolbar.getLocationOnScreen(loc);
        int toolX = loc[0];
        int toolY = loc[1];
        int toolW = mToolbar.getWidth();
        int toolH = mToolbar.getHeight();
        float scale = toolW / 1440f;

        // evidence/inventory.md：返回 [20,192][212,384]、分享 [1028,192][1220,384]、
        // 更多选项 [1228,192][1420,384]，容器 [0,0][1440,416]
        addButton(toolX, toolY, 20 * scale, 192 * scale, 192 * scale);
        addButton(toolX, toolY, 1028 * scale, 192 * scale, 192 * scale);
        addButton(toolX, toolY, 1228 * scale, 192 * scale, 192 * scale);

        Log.d(TAG, "glass bounds: toolbar=" + toolX + "," + toolY
                + " " + toolW + "x" + toolH + " scale=" + scale);
    }

    private void addButton(int toolX, int toolY, float rx, float ry, float size) {
        int x = toolX + (int) rx;
        int y = toolY + (int) ry;
        int sz = Math.max(1, (int) size);
        GlassButtonView v = new GlassButtonView(mDecor.getContext());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sz, sz);
        lp.leftMargin = x;
        lp.topMargin = y;
        ((ViewGroup) mDecor).addView(v, lp);
        v.configure(sz, sz, mDecor.getResources().getDisplayMetrics().density);
        v.bringToFront();
        BUTTONS.add(v);
    }
}
