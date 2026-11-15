package com.a23bc.ammod;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.view.View;

/**
 * 单个液态玻璃按钮（对应返回 / 分享 / 更多选项之一）。
 *
 * 绘制顺序：界面快照（backdrop）→ 半透明玻璃底色 → 库同款方向性高光，
 * 整层再套 RenderEffect：先 blur(2dp)，再用库的 RoundedRectRefraction shader 做边缘折射。
 *
 * 触摸穿透：不 clickable、不设置 OnTouchListener，事件会继续派发给下层原按钮。
 * 本类的 API 33+ 调用由 GlassToolbar.install() 的 SDK_INT 判断保护。
 */
public class GlassButtonView extends View {

    private final Paint mTint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();
    private final Rect mSrc = new Rect();
    private final Rect mDst = new Rect();

    private Bitmap mSnapshot;
    private int mSrcX;
    private int mSrcY;
    private float mRadius;

    public GlassButtonView(Context context) {
        super(context);
        setClickable(false);
        setFocusable(false);
        // LiquidButton: onDrawSurface 会叠一层半透明 surface
        mTint.setColor(Color.argb(34, 255, 255, 255));
        // Highlight.Default 的 blendMode = BlendMode.Plus
        mHighlight.setBlendMode(BlendMode.PLUS);
    }

    void configure(int w, int h, float density) {
        mRect.set(0f, 0f, w, h);
        mDst.set(0, 0, w, h);
        mRadius = Math.min(w, h) / 2f;

        RuntimeShader lens = new RuntimeShader(BackdropShaders.REFRACTION);
        lens.setFloatUniform("size", (float) w, (float) h);
        lens.setFloatUniform("offset", 0f, 0f);
        lens.setFloatUniform("cornerRadii", mRadius, mRadius, mRadius, mRadius);
        // LiquidButton: lens(12dp, 24dp)
        lens.setFloatUniform("refractionHeight", 12f * density);
        lens.setFloatUniform("refractionAmount", -24f * density);
        lens.setFloatUniform("depthEffect", 0f);

        RenderEffect blur = RenderEffect.createBlurEffect(
                2f * density, 2f * density, Shader.TileMode.CLAMP);
        RenderEffect refraction = RenderEffect.createRuntimeShaderEffect(lens, "content");
        setRenderEffect(RenderEffect.createChainEffect(refraction, blur));

        RuntimeShader hi = new RuntimeShader(BackdropShaders.HIGHLIGHT);
        hi.setFloatUniform("size", (float) w, (float) h);
        hi.setFloatUniform("cornerRadii", mRadius, mRadius, mRadius, mRadius);
        // Highlight.Default: White alpha 0.5, angle 45°, falloff 1
        hi.setColorUniform("color", Color.valueOf(1f, 1f, 1f, 0.5f));
        hi.setFloatUniform("angle", (float) (45.0 * Math.PI / 180.0));
        hi.setFloatUniform("falloff", 1f);
        mHighlight.setShader(hi);
    }

    void setSnapshot(Bitmap snapshot, int srcX, int srcY) {
        mSnapshot = snapshot;
        mSrcX = srcX;
        mSrcY = srcY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 抓快照时自己不画，否则 decor.draw() 会无限递归。
        if (GlassToolbar.CAPTURING) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        if (mSnapshot != null && !mSnapshot.isRecycled()) {
            mSrc.set(mSrcX, mSrcY, mSrcX + w, mSrcY + h);
            canvas.drawBitmap(mSnapshot, mSrc, mDst, null);
        } else {
            canvas.drawColor(Color.argb(220, 44, 48, 56));
        }
        canvas.drawRoundRect(mRect, mRadius, mRadius, mTint);
        canvas.drawRoundRect(mRect, mRadius, mRadius, mHighlight);
    }
}
