package com.a23bc.ammod;

import android.content.Context;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.view.View;

/**
 * 单个液态玻璃按钮（返回 / 分享 / 更多选项之一）。
 *
 * 半透明玻璃罩：不画不透明底，让下层原按钮图标透出来；
 * 只叠玻璃 tint + 细描边 + 库同款 Highlight.Default 方向性边缘高光（仅描边），
 * 整层再套 RenderEffect：blur(2dp) + 库的 RoundedRectRefraction 边缘折射。
 *
 * 触摸穿透：不 clickable、不设 OnTouchListener，事件继续派发给下层原按钮。
 * API 33+ 调用由 GlassToolbar.install() 的 SDK_INT 判断保护。
 */
public class GlassButtonView extends View {

    private final Paint mTint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();

    private float mRadius;
    private float mInset;

    public GlassButtonView(Context context) {
        super(context);
        setClickable(false);
        setFocusable(false);
        mTint.setColor(Color.argb(26, 255, 255, 255));
        mStroke.setStyle(Paint.Style.STROKE);
        mStroke.setColor(Color.argb(64, 255, 255, 255));
        mHighlight.setStyle(Paint.Style.STROKE);
        // Highlight.Default 的 blendMode = BlendMode.Plus
        mHighlight.setBlendMode(BlendMode.PLUS);
    }

    void configure(int w, int h, float density) {
        mRadius = Math.min(w, h) / 2f;
        float sw = Math.max(1f, 0.5f * density);   // Highlight.width = 0.5dp
        mInset = sw;
        mHighlight.setStrokeWidth(sw);
        mStroke.setStrokeWidth(Math.max(1f, 1f * density));

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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mRect.set(mInset, mInset, getWidth() - mInset, getHeight() - mInset);
        float r = Math.max(1f, mRadius - mInset);
        canvas.drawRoundRect(mRect, r, r, mTint);
        canvas.drawRoundRect(mRect, r, r, mStroke);
        canvas.drawRoundRect(mRect, r, r, mHighlight);
    }
}
