package com.idea.mydiary.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.idea.mydiary.R;
import com.idea.mydiary.customviews.PaintView;

import java.lang.ref.WeakReference;

import static com.idea.mydiary.activities.MainActivity.SELECTED_NOTE_ID;

public class PaintActivity extends AppCompatActivity {
    public static final String PAINTING_URL = "PAINTING_URL";
    private PaintView mPaintView;
    private ImageButton mChangeBackgroundColorBtn;
    private ImageButton mChangePencilColorBtn;
    private ImageButton mEraserActiveBtn;
    private SeekBar mSeekBar;
    private TextView mSeekBarValue;
    private ImageButton mChangeBrushSizeBtn;
    private long mNoteId;

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private static final int REQUEST_PERMISSIONS_CODE = 14;
    private static final int PERMISSIONS_COUNT = PERMISSIONS.length;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paint);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.string_canvas));
        initViews();

        mNoteId = getIntent().getLongExtra(SELECTED_NOTE_ID, -1L);
        // Log.d("HRD", String.valueOf(mNoteId));

        mChangeBackgroundColorBtn.setOnClickListener(changeBackgroundListener);
        mChangePencilColorBtn.setOnClickListener(changePencilColorListener);
        mEraserActiveBtn.setOnClickListener(activateEraserListener);
        mChangeBrushSizeBtn.setOnClickListener(changeBrushSizeListener);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mPaintView.init(metrics);

        mPaintView.setBrushSize(5f);
    }

    private void initViews() {
        mPaintView = findViewById(R.id.paintView);
        mChangeBackgroundColorBtn = findViewById(R.id.change_background_color);
        mChangePencilColorBtn = findViewById(R.id.change_pencil_color);
        mEraserActiveBtn = findViewById(R.id.eraser_active);
        mChangeBrushSizeBtn = findViewById(R.id.change_brush_size);
    }

    SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mSeekBarValue.setText(String.valueOf(progress));
            mPaintView.setBrushSize((float) progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    View.OnClickListener changeBrushSizeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            displayChangeBrushSizeDialog();
            deactivateEraser();
        }
    };

    View.OnClickListener changeBackgroundListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            changePaintViewBackgroundColor();
            deactivateEraser();
        }
    };

    View.OnClickListener changePencilColorListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            changePaintViewPencilColor();
            deactivateEraser();
        }
    };

    View.OnClickListener activateEraserListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPaintView.activateEraser();
            v.setBackgroundColor(ContextCompat.getColor(
                    PaintActivity.this, R.color.colorAccent));
        }
    };

    private void deactivateEraser() {
        mEraserActiveBtn.setBackgroundColor(ContextCompat.getColor(
                PaintActivity.this, R.color.cardBackground));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_paint, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setActivityResult();
                return true;
            case R.id.menu_normal:
                mPaintView.normal();
                return true;
            case R.id.menu_emboss:
                mPaintView.emboss();
                return true;
            case R.id.menu_blur:
                mPaintView.blur();
                return true;
            case R.id.menu_save:
                saveCanvas();
                return true;
            case R.id.menu_clear:
                clearCanvasConfirmationDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveCanvas() {
//        String url = mPaintView.saveCanvasAsPNG();
//        if(!url.isEmpty())
//            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
//
//        setActivityResult(url);
        new SaveCanvasAsyncTask(this).execute(mPaintView);
    }

    public void setActivityResult(String url) {
        Intent data = getIntent();
        data.putExtra(SELECTED_NOTE_ID, mNoteId);
        data.putExtra(PAINTING_URL, url);
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    private void setActivityResult() {
        Intent data = getIntent();
        data.putExtra(SELECTED_NOTE_ID, mNoteId);
        setResult(Activity.RESULT_OK, data);
        finish();
    }


    private void changePaintViewBackgroundColor() {
        ColorPickerDialogBuilder
                .with(this, R.style.DialogTheme)
                .setTitle(getString(R.string.string_choose_color))
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int selectedColor) {
                        mPaintView.setBackgroundColor("#" + Integer.toHexString(selectedColor));
                    }
                })
                .setPositiveButton(getString(R.string.string_yes), new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        mPaintView.setBackgroundColor("#" + Integer.toHexString(selectedColor));
                    }
                })
                .setNegativeButton(getString(R.string.string_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .build()
                .show();
    }

    private void changePaintViewPencilColor() {
        ColorPickerDialogBuilder
                .with(this, R.style.DialogTheme)
                .setTitle(getString(R.string.string_choose_color))
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .initialColor(mPaintView.getBrushColor())
                .setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int selectedColor) {
                        mPaintView.setPencilColor("#" + Integer.toHexString(selectedColor));
                    }
                })
                .setPositiveButton(getString(R.string.string_ok), new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        mPaintView.setPencilColor("#" + Integer.toHexString(selectedColor));
                    }
                })
                .setNegativeButton(getString(R.string.string_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .build()
                .show();
    }

    public void displayChangeBrushSizeDialog() {
        View customLayout = LayoutInflater.from(this).inflate(R.layout.slider_dialog, null);
        mSeekBar = customLayout.findViewById(R.id.seekBar);
        mSeekBarValue = customLayout.findViewById(R.id.textViewSeekValue);
        mSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        mSeekBar.setProgress((int) mPaintView.getBrushSize());
        mSeekBarValue.setText(String.valueOf(mPaintView.getBrushSize()));

        new AlertDialog.Builder(this, R.style.DialogTheme)
                .setView(customLayout)
                .setTitle(R.string.string_brush_size)
                .setPositiveButton(android.R.string.yes, null)
                .show();
    }

    public void clearCanvasConfirmationDialog() {
        new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(R.string.string_clear_canvas)
                .setMessage(R.string.string_clear_canvas_confirm)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mPaintView.clear();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // PERMISSIONS HANDLING
    private boolean permissionsDenied() {
        for (int i = 0; i < PERMISSIONS_COUNT; i++) {
            if (checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionsDenied()) {
            Toast.makeText(this, "Perm Denied", Toast.LENGTH_LONG).show();
            ((ActivityManager) (this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
            Log.e("HRD", "Perm Denied");
            recreate();
        } else {
            onResume();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        }
    }

    private static class SaveCanvasAsyncTask extends AsyncTask<PaintView, Void, String> {
        private final WeakReference<PaintActivity> mActivityReference;

        private SaveCanvasAsyncTask(PaintActivity context) {
            mActivityReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(PaintView... params) {
            return params[0].saveCanvasAsPNG();
        }

        @Override
        protected void onPostExecute(String url) {
            PaintActivity activity = mActivityReference.get();
            Toast.makeText(activity, "Saved", Toast.LENGTH_SHORT).show();
            activity.setActivityResult(url);
        }
    }
}