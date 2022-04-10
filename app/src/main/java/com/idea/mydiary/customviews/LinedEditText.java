package com.idea.mydiary.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

public class LinedEditText extends AppCompatEditText {
    private final Rect rect;
    private final Paint paint;

    public LinedEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        rect = new Rect();
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(0xEEEEEEEE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int count = getLineCount();
        int height = ((View) (this.getParent())).getHeight();
        int lineHeight = getLineHeight();
        int numberOfLines = height / lineHeight;

        if (count > numberOfLines)
            numberOfLines = count;

        int baseLine = getLineBounds(0, rect);
        for (int i = 0; i < numberOfLines; i++) {
            canvas.drawLine(rect.left, baseLine + 1, rect.right, baseLine + 1, paint);
            baseLine += lineHeight;
        }
        super.onDraw(canvas);
    }
}
