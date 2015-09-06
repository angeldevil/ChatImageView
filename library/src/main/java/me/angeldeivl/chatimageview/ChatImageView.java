package me.angeldeivl.chatimageview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.support.annotation.DrawableRes;
import android.util.AttributeSet;
import android.widget.ImageView;

import me.angeldeivl.library.R;

public class ChatImageView extends ImageView {

    private int mMaskResId;
    private Bitmap mBitmap;
    private Bitmap mMaskBmp;
    private NinePatchDrawable mMaskDrawable;

    private Bitmap mResult;

    private Paint mPaint;
    private Paint mMaskPaint;

    private Matrix mMatrix;

    public ChatImageView(Context context) {
        super(context);
        init();
    }

    public ChatImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ChatImageView, 0, 0);
        if (a != null) {
            mMaskResId = a.getResourceId(R.styleable.ChatImageView_chat_image_mask, 0);
            a.recycle();
        }
        init();
    }

    private void init() {
        mMatrix = new Matrix();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (mMaskResId <= 0) {
            return;
        }
        mMaskBmp = BitmapFactory.decodeResource(getResources(), mMaskResId);
        byte[] ninePatchChunk = mMaskBmp.getNinePatchChunk();
        if (ninePatchChunk != null && NinePatch.isNinePatchChunk(ninePatchChunk)) {
            mMaskDrawable = new NinePatchDrawable(getResources(), mMaskBmp, ninePatchChunk, new Rect(), null);
        }
        internalSetImage();
    }

    private void internalSetImage() {
        if (mMaskResId <= 0) {
            return;
        }
        if (mBitmap == null) {
            return;
        }
        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        boolean canReUseBitmap = mResult != null
                && mResult.getWidth() == width && mResult.getHeight() == height;
        if (!canReUseBitmap) {
            mResult = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(mResult);
        if (canReUseBitmap) {
            canvas.drawColor(Color.TRANSPARENT);
        }
        // CENTER_CROP Bitmap
        mMatrix.reset();
        float scale;
        float dx = 0, dy = 0;
        int bmpWidth = mBitmap.getWidth();
        int bmpHeight = mBitmap.getHeight();
        if (bmpWidth * height > width * bmpHeight) {
            scale = (float) height / (float) bmpHeight;
            dx = (width - bmpWidth * scale) * 0.5f;
        } else {
            scale = (float) width / (float) bmpWidth;
            dy = (height - bmpHeight * scale) * 0.5f;
        }
        mMatrix.setScale(scale, scale);
        mMatrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        canvas.save();
        canvas.concat(mMatrix);
        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
        canvas.restore();

        if (mMaskDrawable != null) {
            mMaskDrawable.getPaint().setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            mMaskDrawable.setBounds(0, 0, width, height);
            mMaskDrawable.draw(canvas);
        } else if (mMaskBmp != null) {
            if (mMaskPaint == null) {
                mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            }
            canvas.drawBitmap(mMaskBmp, 0, 0, mMaskPaint);
        }
        super.setImageBitmap(mResult);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        mBitmap = bm;
        if (mMaskResId > 0 && mBitmap != null) {
            internalSetImage();
        } else {
            super.setImageBitmap(bm);
        }
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        mBitmap = getBitmapFromDrawable(getResources().getDrawable(resId));
        internalSetImage();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        Bitmap bmp = getBitmapFromDrawable(drawable);
        if (mBitmap == bmp) {
            super.setImageDrawable(drawable);
        } else {
            mBitmap = bmp;
            internalSetImage();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        internalSetImage();
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        try {
            Bitmap bitmap;

            if (drawable instanceof ColorDrawable) {
                bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }
}
