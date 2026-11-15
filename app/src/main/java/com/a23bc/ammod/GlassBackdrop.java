package com.a23bc.ammod;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import java.util.List;

/**
 * 生成「干净背景」快照，作为液态玻璃的 backdrop content。
 *
 * 1. 把 DecorView 顶部工具栏区域画进 Bitmap；期间把模块自己的 overlay 全部设 INVISIBLE，
 *    否则会递归把自己画进去。
 * 2. 原生的返回 / 分享 / 更多按钮也在快照里 —— 用整图的缩小再放大模糊版把这三块区域
 *    覆盖掉（近似 inpaint），否则玻璃里会看到被折射的旧按钮，就又变成「叠遮罩」了。
 * 3. 结果是不透明的干净背景：cover View 用它盖住原按钮，GlassButtonView 用它做折射源。
 */
public class GlassBackdrop {

    private static final String TAG = "AMMod";
    private static final int DOWNSCALE = 16;

    private final View mDecor;
    private final List<View> mOverlays;
    private final int mX;
    private final int mY;
    private final int mW;
    private final int mH;
    private final Rect[] mButtonRects;

    private final Bitmap mRaw;
    private final Bitmap mClean;
    private final Bitmap mSmall;
    private final Bitmap mBlurred;
    private final Paint mFilter = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

    GlassBackdrop(View decor, List<View> overlays, int x, int y, int w, int h,
                  Rect[] buttonRects) {
        mDecor = decor;
        mOverlays = overlays;
        mX = x;
        mY = y;
        mW = w;
        mH = h;
        mButtonRects = buttonRects;
        mRaw = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mClean = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mBlurred = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mSmall = Bitmap.createBitmap(Math.max(1, w / DOWNSCALE),
                Math.max(1, h / DOWNSCALE), Bitmap.Config.ARGB_8888);
    }

    Bitmap capture() {
        setOverlaysVisible(false);
        try {
            Canvas c = new Canvas(mRaw);
            c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            c.translate(-mX, -mY);
            mDecor.draw(c);
        } catch (Throwable t) {
            Log.d(TAG, "glass backdrop draw error: " + t);
        } finally {
            setOverlaysVisible(true);
        }

        // 全图快速模糊：缩小再放大，用于把原按钮区域抹掉
        Canvas sc = new Canvas(mSmall);
        sc.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        sc.drawBitmap(mRaw, null,
                new Rect(0, 0, mSmall.getWidth(), mSmall.getHeight()), mFilter);
        Canvas bc = new Canvas(mBlurred);
        bc.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        bc.drawBitmap(mSmall, null, new Rect(0, 0, mW, mH), mFilter);

        // 干净背景 = 原图，三个按钮区域换成模糊背景
        Canvas cc = new Canvas(mClean);
        cc.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        cc.drawBitmap(mRaw, 0f, 0f, null);
        for (Rect r : mButtonRects) {
            Rect rr = new Rect(Math.max(0, r.left - 12), Math.max(0, r.top - 12),
                    Math.min(mW, r.right + 12), Math.min(mH, r.bottom + 12));
            cc.drawBitmap(mBlurred, rr, rr, null);
        }
        return mClean;
    }

    private void setOverlaysVisible(boolean visible) {
        for (View v : mOverlays) {
            v.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
