package com.idea.mydiary;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;

import static com.idea.mydiary.Utils.APP_FOLDER;

/**
 * TODO: document your custom view class.
 */
public class PaintView extends View {
    public static float BRUSH_SIZE = 5f;
    public int mDefaultColor = Color.BLACK;
    public static final int DEFAULT_BG_COLOR = Color.WHITE;
    private static final float TOUCH_TOLERANCE = 4;
    private float mX, mY;
    private Path mPath;
    private Paint mPaint;
    private ArrayList<FingerPath> paths = new ArrayList<>();
    private int currentColor;
    private int backgroundColor = DEFAULT_BG_COLOR;
    private float strokeWidth;
    private boolean emboss;
    private boolean blur;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private int mSelectedColor;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);

    public PaintView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public PaintView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        mDefaultColor = context.getColor(R.color.colorAccent);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(mDefaultColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xff);
        mSelectedColor = mDefaultColor;

        mEmboss = new EmbossMaskFilter(new float[]{1, 1, 1}, 0.4f, 6, 3.5f);
        mBlur = new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL);
    }

    public void init(DisplayMetrics metrics) {
        int height = metrics.heightPixels;
        int width = metrics.widthPixels;

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        currentColor = mDefaultColor;
        strokeWidth = BRUSH_SIZE;
    }

    public String saveCanvasAsPNG(String noteId) {
        final File folder = APP_FOLDER;
        long time = Calendar.getInstance().getTimeInMillis();
        if (!folder.exists()) {
            folder.mkdir();
        }

        String filename = noteId +"_"+ time + ".png";
        File f = new File(folder, filename);
        OutputStream stream = null;

        try {
            f.createNewFile();
            stream = new FileOutputStream(f.getPath());
            mBitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
            stream.close();
        } catch (IOException e) {
            Log.d("HRD", "IOExcepion");
            e.printStackTrace();
        }

        return folder + filename;
    }

    public void normal() {
        emboss = false;
        blur = false;
    }

    public void emboss() {
        emboss = true;
        blur = false;
    }

    public void blur() {
        emboss = false;
        blur = true;
    }

    public void clear() {
        currentColor = mDefaultColor;
        paths.clear();
        normal();
        invalidate();
    }

    public float getBrushSize() {
        return strokeWidth;
    }

    public void setBrushSize(float size) {
        strokeWidth = size;
    }

    public void setBackgroundColor(String colorString) {
        backgroundColor = Color.parseColor(colorString);
        invalidate();
    }

    public void setPencilColor(String colorString) {
        currentColor = Color.parseColor(colorString);
        mSelectedColor = currentColor;
        invalidate();
    }

    public void activateEraser() {
        currentColor = backgroundColor;
        normal();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        mCanvas.drawColor(backgroundColor);

        for (FingerPath fp : paths) {
            mPaint.setColor(fp.color);
            mPaint.setStrokeWidth(fp.strokeWidth);
            mPaint.setMaskFilter(null);

            if (fp.emboss)
                mPaint.setMaskFilter(mEmboss);
            else if (fp.blur)
                mPaint.setMaskFilter(mBlur);

            mCanvas.drawPath(fp.path, mPaint);

        }

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.restore();
    }

    private void touchStart(float x, float y) {
        mPath = new Path();
        FingerPath fp = new FingerPath(currentColor, emboss, blur, strokeWidth, mPath);
        paths.add(fp);

        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        mPath.lineTo(mX, mY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                break;
        }

        return true;
    }

    public int getBrushColor() {
        return mSelectedColor;
    }
}
