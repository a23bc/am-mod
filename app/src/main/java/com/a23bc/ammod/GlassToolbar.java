package com.a23bc.ammod;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
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
 * 把顶部工具栏的返回 / 分享 / 更多选项**整体替换**成液态玻璃按钮。
 *
 * 布局（全部挂在 DecorView 上）：
 *   - CoverView：覆盖整个工具栏区域，画「干净背景」快照，盖掉原生按钮；
 *   - 三个 GlassButtonView：各自以同一块真实背景为 content，用库同款折射 shader
 *     做边缘折射，叠玻璃表面，并重画图标。
 *
 * 所有 View 都不 clickable，触摸事件穿透到下层原按钮，功能不变。
 * 快照每 500ms 刷新一次，让玻璃里的背景跟随界面变化。
 */
public class GlassToolbar {

    private static final String TAG = "AMMod";
    private static final long REFRESH_MS = 500L;
    private static final Set<Integer> INSTALLED = new HashSet<>();

    private final View mDecor;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final List<View> mOverlays = new ArrayList<>();
    private final List<GlassButtonView> mButtons = new ArrayList<>();
    private final CoverView mCover;
    private GlassBackdrop mBackdrop;

    static void install(View decor, int toolX, int toolY, int toolW, int toolH) {
        Log.d(TAG, "glass install enter " + toolW + "x" + toolH);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "glass skipped: need API 33+");
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
            Log.d(TAG, "glass installed: 3 full replacement buttons");
        } catch (Throwable t) {
            synchronized (INSTALLED) {
                INSTALLED.remove(key);
            }
            Log.d(TAG, "glass install error: " + t);
        }
    }

    private GlassToolbar(View decor, int toolX, int toolY, int toolW, int toolH) {
        mDecor = decor;
        float scale = toolW / 1440f;
        float density = decor.getResources().getDisplayMetrics().density;

        mCover = new CoverView(decor.getContext());
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(toolW, toolH);
        clp.leftMargin = toolX;
        clp.topMargin = toolY;
        ((ViewGroup) decor).addView(mCover, clp);
        mOverlays.add(mCover);

        int[][] specs = {
                {20, 192, GlassButtonView.TYPE_BACK},
                {1028, 192, GlassButtonView.TYPE_SHARE},
                {1228, 192, GlassButtonView.TYPE_MORE},
        };
        Rect[] rects = new Rect[specs.length];
        for (int i = 0; i < specs.length; i++) {
            float rx = specs[i][0] * scale;
            float ry = specs[i][1] * scale;
            float size = 192 * scale;
            int sz = Math.max(1, (int) size);
            GlassButtonView v = new GlassButtonView(decor.getContext());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sz, sz);
            lp.leftMargin = toolX + (int) rx;
            lp.topMargin = toolY + (int) ry;
            ((ViewGroup) decor).addView(v, lp);
            v.configure(sz, sz, density, specs[i][2], (int) rx, (int) ry);
            v.bringToFront();
            mButtons.add(v);
            mOverlays.add(v);
            rects[i] = new Rect((int) rx, (int) ry, (int) (rx + size), (int) (ry + size));
        }

        mBackdrop = new GlassBackdrop(decor, mOverlays, toolX, toolY, toolW, toolH, rects);
    }

    private void start() {
        refresh();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refresh();
                mHandler.postDelayed(this, REFRESH_MS);
            }
        }, REFRESH_MS);
    }

    private void refresh() {
        Bitmap b = mBackdrop.capture();
        mCover.setBackdrop(b);
        mCover.invalidate();
        for (GlassButtonView v : mButtons) {
            v.setBackdrop(b);
            v.invalidate();
        }
    }

    /** 不透明背景层：盖住原生按钮，让新按钮成为唯一的按钮。 */
    private static final class CoverView extends View {
        private Bitmap mBackdrop;

        CoverView(Context context) {
            super(context);
            setClickable(false);
            setFocusable(false);
        }

        void setBackdrop(Bitmap b) {
            mBackdrop = b;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (mBackdrop != null && !mBackdrop.isRecycled()) {
                canvas.drawBitmap(mBackdrop, 0f, 0f, null);
            }
        }
    }
}
