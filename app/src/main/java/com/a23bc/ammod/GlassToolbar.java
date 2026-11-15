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
 * 坐标与尺寸由 HookMain 在扫描到工具栏那一刻快照后传入：
 * ComposeView 的尺寸会在下一帧抖动，install 里再去读 getWidth() 会拿到 0。
 */
public class GlassToolbar {

    private static final String TAG = "AMMod";
    private static final Set<Integer> INSTALLED = new HashSet<>();
    private static final List<GlassButtonView> BUTTONS = new ArrayList<>();

    private final View mDecor;
    private final int mToolX;
    private final int mToolY;
    private final int mToolW;
    private final int mToolH;

    static boolean isInstalled() {
        return !BUTTONS.isEmpty();
    }

    static void install(View decor, int toolX, int toolY, int toolW, int toolH) {
        Log.d(TAG, "glass install enter " + toolW + "x" + toolH
                + " installed=" + isInstalled());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "glass skipped: need API 33+, got " + Build.VERSION.SDK_INT);
            return;
        }
        if (isInstalled()) {
            return;
        }
        if (!(decor instanceof ViewGroup) || toolW <= 0 || toolH <= 0) {
            Log.d(TAG, "glass abort: bad bounds or decor not ViewGroup");
            return;
        }
        int key = System.identityHashCode(decor);
        synchronized (INSTALLED) {
            if (!INSTALLED.add(key)) {
                return;
            }
        }
        try {
            new GlassToolbar(decor, toolX, toolY, toolW, toolH).start();
            Log.d(TAG, "glass installed: 3 buttons on floating toolbar");
        } catch (Throwable t) {
            synchronized (INSTALLED) {
                INSTALLED.remove(key);
            }
            Log.d(TAG, "glass install error: " + t);
        }
    }

    private GlassToolbar(View decor, int toolX, int toolY, int toolW, int toolH) {
        mDecor = decor;
        mToolX = toolX;
        mToolY = toolY;
        mToolW = toolW;
        mToolH = toolH;
    }

    private void start() {
        float scale = mToolW / 1440f;

        // evidence/inventory.md：返回 [20,192][212,384]、分享 [1028,192][1220,384]、
        // 更多选项 [1228,192][1420,384]，容器 [0,0][1440,416]
        addButton(20 * scale, 192 * scale, 192 * scale);
        addButton(1028 * scale, 192 * scale, 192 * scale);
        addButton(1228 * scale, 192 * scale, 192 * scale);

        Log.d(TAG, "glass bounds: toolbar=" + mToolX + "," + mToolY
                + " " + mToolW + "x" + mToolH + " scale=" + scale);
    }

    private void addButton(float rx, float ry, float size) {
        int x = mToolX + (int) rx;
        int y = mToolY + (int) ry;
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
