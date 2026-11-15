package com.a23bc.ammod;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.view.View;

/**
 * 一个完整替换后的液态玻璃按钮。
 *
 * content = 真实界面背景快照（原按钮已被 GlassBackdrop 抹掉）；
 * 库的 RoundedRectRefraction shader 对这块真实背景做边缘折射，
 * 再叠玻璃表面（渐变 tint + 描边 + 库同款方向性高光）与重画的白色图标。
 *
 * 整个 View 是按钮本身，不是盖在原按钮上的遮罩：下层原按钮由 cover View 盖住。
 * 不 clickable、不设触摸监听，事件穿透到下层原按钮。
 */
public class GlassButtonView extends View {

    static final int TYPE_BACK = 0;
    static final int TYPE_SHARE = 1;
    static final int TYPE_MORE = 2;

    private final Paint mTint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mIcon = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mIconFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();
    private final Rect mSrc = new Rect();
    private final Rect mDst = new Rect();

    private Bitmap mBackdrop;
    private int mSrcX;
    private int mSrcY;
    private float mRadius;
    private float mInset;
    private int mType;
    private float mDensity;

    public GlassButtonView(Context context) {
        super(context);
        setClickable(false);
        setFocusable(false);
        mStroke.setStyle(Paint.Style.STROKE);
        mStroke.setColor(Color.argb(96, 255, 255, 255));
        mHighlight.setStyle(Paint.Style.STROKE);
        mHighlight.setBlendMode(BlendMode.PLUS);
        mIcon.setColor(Color.WHITE);
        mIcon.setStyle(Paint.Style.STROKE);
        mIcon.setStrokeCap(Paint.Cap.ROUND);
        mIcon.setStrokeJoin(Paint.Join.ROUND);
        mIconFill.setColor(Color.WHITE);
        mIconFill.setStyle(Paint.Style.FILL);
    }

    void configure(int w, int h, float density, int type, int srcX, int srcY) {
        mType = type;
        mDensity = density;
        mSrcX = srcX;
        mSrcY = srcY;
        mRadius = Math.min(w, h) / 2f;
        float sw = Math.max(1f, 1f * density);
        mInset = sw;
        mHighlight.setStrokeWidth(sw);
        mStroke.setStrokeWidth(Math.max(1f, 1f * density));
        mIcon.setStrokeWidth(Math.max(2f, 3.5f * density));

        mTint.setShader(new LinearGradient(
                0f, 0f, 0f, h,
                Color.argb(40, 255, 255, 255),
                Color.argb(8, 255, 255, 255),
                Shader.TileMode.CLAMP));

        RuntimeShader lens = new RuntimeShader(BackdropShaders.REFRACTION);
        lens.setFloatUniform("size", (float) w, (float) h);
        lens.setFloatUniform("offset", 0f, 0f);
        lens.setFloatUniform("cornerRadii", mRadius, mRadius, mRadius, mRadius);
        // LiquidButton: lens(12dp, 24dp)
        lens.setFloatUniform("refractionHeight", 12f * density);
        lens.setFloatUniform("refractionAmount", -24f * density);
        lens.setFloatUniform("depthEffect", 0f);

        RenderEffect blur = RenderEffect.createBlurEffect(
                1f * density, 1f * density, Shader.TileMode.CLAMP);
        RenderEffect refraction = RenderEffect.createRuntimeShaderEffect(lens, "content");
        setRenderEffect(RenderEffect.createChainEffect(refraction, blur));

        RuntimeShader hi = new RuntimeShader(BackdropShaders.HIGHLIGHT);
        hi.setFloatUniform("size", (float) w, (float) h);
        hi.setFloatUniform("cornerRadii", mRadius, mRadius, mRadius, mRadius);
        hi.setColorUniform("color", Color.valueOf(1f, 1f, 1f, 0.55f));
        hi.setFloatUniform("angle", (float) (45.0 * Math.PI / 180.0));
        hi.setFloatUniform("falloff", 1f);
        mHighlight.setShader(hi);
    }

    void setBackdrop(Bitmap backdrop) {
        mBackdrop = backdrop;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        // 1) content：真实界面背景（原按钮已被抹掉）
        if (mBackdrop != null && !mBackdrop.isRecycled()) {
            mSrc.set(mSrcX, mSrcY, mSrcX + w, mSrcY + h);
            mDst.set(0, 0, w, h);
            canvas.drawBitmap(mBackdrop, mSrc, mDst, null);
        } else {
            canvas.drawColor(Color.argb(255, 24, 26, 30));
        }

        // 2) 玻璃表面
        mRect.set(mInset, mInset, w - mInset, h - mInset);
        float r = Math.max(1f, mRadius - mInset);
        canvas.drawRoundRect(mRect, r, r, mTint);
        canvas.drawRoundRect(mRect, r, r, mStroke);
        canvas.drawRoundRect(mRect, r, r, mHighlight);

        // 3) 重画的图标（替换掉原按钮的图标）
        drawIcon(canvas, w, h);
    }

    private void drawIcon(Canvas c, int w, int h) {
        float cx = w / 2f;
        float cy = h / 2f;
        float s = Math.min(w, h);
        if (mType == TYPE_BACK) {
            c.drawLine(cx - s * 0.17f, cy, cx + s * 0.17f, cy, mIcon);
            Path p = new Path();
            p.moveTo(cx - s * 0.06f, cy - s * 0.12f);
            p.lineTo(cx - s * 0.17f, cy);
            p.lineTo(cx - s * 0.06f, cy + s * 0.12f);
            c.drawPath(p, mIcon);
        } else if (mType == TYPE_SHARE) {
            float dot = Math.max(2f, 3.2f * mDensity);
            c.drawCircle(cx - s * 0.15f, cy - s * 0.12f, dot, mIconFill);
            c.drawCircle(cx - s * 0.15f, cy + s * 0.12f, dot, mIconFill);
            c.drawCircle(cx + s * 0.16f, cy, dot, mIconFill);
            c.drawLine(cx - s * 0.11f, cy - s * 0.10f, cx + s * 0.11f, cy - s * 0.02f, mIcon);
            c.drawLine(cx - s * 0.11f, cy + s * 0.10f, cx + s * 0.11f, cy + s * 0.02f, mIcon);
        } else {
            float dot = Math.max(2f, 3.2f * mDensity);
            c.drawCircle(cx, cy - s * 0.15f, dot, mIconFill);
            c.drawCircle(cx, cy, dot, mIconFill);
            c.drawCircle(cx, cy + s * 0.15f, dot, mIconFill);
        }
    }
}
