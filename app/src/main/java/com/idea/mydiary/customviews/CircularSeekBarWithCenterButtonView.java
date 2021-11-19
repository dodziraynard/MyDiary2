package com.idea.mydiary.customviews;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.idea.mydiary.R;

/**
 * TODO: document your custom view class.
 */
public class CircularSeekBarWithCenterButtonView extends View {
    private Bitmap onBitmap;
    private Bitmap offBitmap;
    private int width;
    private int height;

    public CircularSeekBarWithCenterButtonView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public CircularSeekBarWithCenterButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public CircularSeekBarWithCenterButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        onBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_play);
        offBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_pause);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() != width || getHeight() != height) {
            width = onBitmap.getWidth();
            height = onBitmap.getHeight();


        }

    }


}
