package com.a23bc.ammod;

import android.annotation.RequiresApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
 * 返回 / 分享 / 更多选项不是独立 View，改不了它们的属性；
 * 所以在 DecorView 上按 evidence/inventory.md 的实测 bounds 叠三个自绘 View，
 * 用库同款 shader 折射「背后界面快照」，并保持触摸穿透让原按钮照常工作。
 *
 * 按钮坐标按 1440x3168 基线等比缩放（基准来自真机 uiautomator dump）。
 */
@RequiresApi(33)
public class GlassToolbar {

    private static final String TAG = "AMMod";
    static volatile boolean CAPTURING = false;

    private static final Set<Integer> INSTALLED = new HashSet<>();
    private static final long REFRESH_MS = 1200L;

    private final View mDecor;
    private final View mToolbar;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final List<Btn> mButtons = new ArrayList<>();
    private Bitmap mSnapshot;
    private int mToolX;
    private int mToolY;
    private int mToolW;
    private int mToolH;

    private static final class Btn {
        final GlassButtonView view;
        final int srcX;
        final int srcY;

        Btn(GlassButtonView v, int x, int y) {
            view = v;
            srcX = x;
            srcY = y;
        }
    }

    static void install(View decor, View toolbar) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "glass skipped: need API 33+, got " + Build.VERSION.SDK_INT);
            return;
        }
        if (!(decor instanceof ViewGroup) || toolbar.getWidth() <= 0) {
            return;
        }
        synchronized (INSTALLED) {
            if (!INSTALLED.add(System.identityHashCode(decor))) {
                return;
            }
        }
        try {
            new GlassToolbar(decor, toolbar).start();
            Log.d(TAG, "glass installed: 3 buttons on floating toolbar");
        } catch (Throwable t) {
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
        mToolX = loc[0];
        mToolY = loc[1];
        mToolW = mToolbar.getWidth();
        mToolH = mToolbar.getHeight();
        float scale = mToolW / 1440f;

        // evidence/inventory.md：返回 [20,192][212,384]、分享 [1028,192][1220,384]、
        // 更多选项 [1228,192][1420,384]，容器 [0,0][1440,416]
        addButton(20 * scale, 192 * scale, 192 * scale);
        addButton(1028 * scale, 192 * scale, 192 * scale);
        addButton(1228 * scale, 192 * scale, 192 * scale);

        capture();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                capture();
                mHandler.postDelayed(this, REFRESH_MS);
            }
        }, REFRESH_MS);
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
        mButtons.add(new Btn(v, (int) rx, (int) ry));
    }

    /** 把工具栏区域的真实界面画进 Bitmap，作为 backdrop 的 content。 */
    private void capture() {
        if (mToolW <= 0 || mToolH <= 0) {
            return;
        }
        try {
            Bitmap b = mSnapshot;
            if (b == null || b.isRecycled() || b.getWidth() != mToolW || b.getHeight() != mToolH) {
                b = Bitmap.createBitmap(mToolW, mToolH, Bitmap.Config.ARGB_8888);
                mSnapshot = b;
            } else {
                b.eraseColor(Color.TRANSPARENT);
            }
            Canvas c = new Canvas(b);
            c.translate(-mToolX, -mToolY);
            CAPTURING = true;
            try {
                mDecor.draw(c);
            } finally {
                CAPTURING = false;
            }
            for (Btn btn : mButtons) {
                btn.view.setSnapshot(b, btn.srcX, btn.srcY);
                btn.view.invalidate();
            }
        } catch (Throwable t) {
            CAPTURING = false;
            Log.d(TAG, "glass capture error: " + t);
        }
    }
}
