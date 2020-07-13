package com.idea.mydiary;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.idea.mydiary.models.DataManager;
import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;
import com.idea.mydiary.models.PlayerState;
import com.idea.mydiary.adapters.MediaAdapter;
import com.idea.mydiary.services.MediaService;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import static com.idea.mydiary.MainActivity.SELECTED_NOTE_ID;
import static com.idea.mydiary.Utils.APP_FOLDER;
import static com.idea.mydiary.Utils.copyFile;
import static com.idea.mydiary.Utils.deleteMyFile;
import static com.idea.mydiary.Utils.enableStrictModeAll;
import static com.idea.mydiary.Utils.padLeftZeros;
import static com.idea.mydiary.services.MediaService.MEDIA_DURATION;
import static com.idea.mydiary.services.MediaService.MEDIA_POSITION;
import static com.idea.mydiary.services.MediaService.PLAYER_STATE;

public class NewNoteActivity extends AppCompatActivity {

    public static final int NOTE_ID_REQUEST_CODE = 0;
    private static final int SELECT_PICTURE_REQUEST_CODE = 1;
    public static final String SELECTED_IMAGE_URL = "SELECTED_IMAGE_URL";
    public static final String AUDIO_URI = "AUDIO_URI";
    public static final String SEEK_POSITION = "SEEK_POSITION";
    public static final String MEDIA_SERVICE_INFO = "MEDIA_SERVICE_INFO";
    private Note mNote = null;
    private EditText mEditTextNoteText;
    private EditText mTextViewNoteTitle;
    private TextView mDateView;
    private boolean mIsNoteCancel = false;
    private boolean isNewNote = false;
    private Calendar mCalendar;
    private Snackbar mSnackbar;
    private boolean isSnackShowing = false;
    private RecyclerView mMediaRecyclerView;
    private DataManager mDataManager;
    List<Media> mMediaList;

    private static final String[] PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
    };

    private static final int REQUEST_PERMISSIONS_CODE = 0;
    private static final int PERMISSIONS_COUNT = PERMISSIONS.length;
    private boolean serviceBound = false;
    private MediaService mMediaService;
    boolean mReceiverRegistered = false;
    private MediaReceiver receiver;
    private boolean serviceAudioPaused = false;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.idea.mydiary.PLAY";
    public static final String Broadcast_RESUME_AUDIO = "com.idea.mydiary.RESUME";
    public static final String Broadcast_PAUSE_AUDIO = "com.idea.mydiary.PAUSE";
    public static final String Broadcast_SEEK_AUDIO = "com.idea.mydiary.SEEK";
    private Utils.AudioRecorder mAudioRecorder;
    private String mRecordingFileName;
    private AlertDialog mRecordingDialog;
    private AlertDialog mPlayerDialog;
    private String mSelectedAudioUri;
    private String STATE_AUDIO = "STATE_AUDIO";
    private TextView mTextViewDuration;
    private ImageView mPlayerPlayPause;
    private SeekBar mPlayerSeekBar;
    private boolean mPlayPressed = false;
    private MediaAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.string_edit_note));
        mDataManager = DataManager.getInstance();
        enableStrictModeAll();
        initViews();

        if (savedInstanceState != null)
            restoreSavedState(savedInstanceState);

        Intent intent = getIntent();
        long currentNoteId = intent.getIntExtra(SELECTED_NOTE_ID, -1);

//        Log.d("HRD", String.valueOf(mCurrentNoteId));
        if (currentNoteId != -1) {
            mNote = mDataManager.getNote(currentNoteId);
        } else {
            mNote = new Note();
            mNote.setId(mDataManager.getNotes().size());
            isNewNote = true;
        }

        displayViews();

        final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                mCalendar.set(Calendar.YEAR, year);
                mCalendar.set(Calendar.MONTH, month);
                mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                mNote.setDate(mCalendar.getTimeInMillis());
                displayViews();
            }
        };

        mDateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(NewNoteActivity.this, R.style.DialogTheme, dateSetListener,
                        mCalendar.get(Calendar.YEAR),
                        mCalendar.get(Calendar.MONTH),
                        mCalendar.get(Calendar.DAY_OF_MONTH)
                ).show();
            }
        });
    }

    private void restoreSavedState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey("ServiceState")) {
            serviceBound = savedInstanceState.getBoolean("ServiceState");
        }

        if (savedInstanceState.containsKey("ReceiverState")) {
            mReceiverRegistered = savedInstanceState.getBoolean("ReceiverState");
        }

        if (!mReceiverRegistered) {
            //Receive data from MediaPlayerService
            receiver = new MediaReceiver();
            registerReceiver(receiver, new IntentFilter("MEDIA_PLAYER_INFO"));
            mReceiverRegistered = true;
        }

        if (savedInstanceState.containsKey(STATE_AUDIO)) {
            mSelectedAudioUri = savedInstanceState.getString(STATE_AUDIO);
        }
    }

    private void playMedia(String uri) {
        if (!serviceBound) {
            // Start new service and play audio
            Intent playerIntent = new Intent(this, MediaService.class);
            playerIntent.putExtra(AUDIO_URI, uri);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            broadcastIntent.putExtra(AUDIO_URI, uri);
            sendBroadcast(broadcastIntent);
        }
    }

    private void resumeMedia() {
        Intent broadcastIntent = new Intent(Broadcast_RESUME_AUDIO);
        sendBroadcast(broadcastIntent);
        serviceAudioPaused = false;
    }

    private void seekMediaTo(int pos) {
        Intent broadcastIntent = new Intent(Broadcast_SEEK_AUDIO);
        broadcastIntent.putExtra(SEEK_POSITION, pos);
        sendBroadcast(broadcastIntent);
    }

    private void pauseMedia() {
        Intent broadcastIntent = new Intent(Broadcast_PAUSE_AUDIO);
        sendBroadcast(broadcastIntent);
        serviceAudioPaused = true;
    }

    private void displayViews() {
        if (mNote != null) {
            mEditTextNoteText.setText(mNote.getText());
            mTextViewNoteTitle.setText(mNote.getTitle());
            mDateView.setText(mNote.getFullDate());
            new NoteMediaTask(this).execute(mNote);

            mAdapter = new MediaAdapter(this);
            mAdapter.setOnImageButtonClickListener(new MediaAdapter.OnItemClickListener() {
                @Override
                public void onImageButtonClickListener(int position) {
                    if(mMediaList == null) return;
                    Media media = mMediaList.get(position);
                    Intent intent = new Intent(NewNoteActivity.this, ImageViewActivity.class);
                    intent.putExtra(SELECTED_NOTE_ID, mNote.getId());
                    intent.putExtra(SELECTED_IMAGE_URL, media.getUrl());
                    startActivityForResult(intent, NOTE_ID_REQUEST_CODE);
                }

                @Override
                public void onAudioButtonClickListener(String uri, int position) {
                    mSelectedAudioUri = uri;
                    openPlayerDialog();
                }
            });
            mMediaRecyclerView.setAdapter(mAdapter);
            mMediaRecyclerView.setLayoutManager(new LinearLayoutManager(
                    this, LinearLayoutManager.HORIZONTAL, false));
        }
    }

    private void initViews() {
        mEditTextNoteText = findViewById(R.id.editTextNoteText);
        mTextViewNoteTitle = findViewById(R.id.editTextNoteTitle);
        mDateView = findViewById(R.id.textViewDate);
        mCalendar = Calendar.getInstance();
        mMediaRecyclerView = findViewById(R.id.media_recycler_view);

        if (mNote != null) {
            mEditTextNoteText.setText(mNote.getText());
            mTextViewNoteTitle.setText(mNote.getTitle());
            mDateView.setText(mNote.getFullDate());
            new NoteMediaTask(this).execute(mNote);
        }
    }

    @Override
    protected void onPause() {
        if (!mIsNoteCancel) {
            saveNote();
        }
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_AUDIO, mSelectedAudioUri);
        outState.putBoolean("ServiceState", serviceBound);
        outState.putBoolean("ReceiverState", mReceiverRegistered);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            mMediaService.stopSelf();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mReceiverRegistered) {
            unregisterReceiver(receiver);
            mReceiverRegistered = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new MediaReceiver();
        registerReceiver(receiver, new IntentFilter(MEDIA_SERVICE_INFO));
        mReceiverRegistered = true;
    }

    private void saveNote() {
        String title = mTextViewNoteTitle.getText().toString();
        String text = mEditTextNoteText.getText().toString();

        if (title.isEmpty() || text.isEmpty()) {
            deleteMyFile(NewNoteActivity.this,
                    new File(APP_FOLDER.getAbsolutePath() + "/"
                            + mRecordingFileName).getAbsolutePath());
            return;
        }

        mNote.setTitle(title);
        mNote.setText(text);

        if (isNewNote) {
            mNote.setDate(Calendar.getInstance().getTimeInMillis());
            mDataManager.addNote(mNote);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_save:
                saveNote();
                return true;
            case R.id.menu_cancel:
                mIsNoteCancel = true;
                onBackPressed();
                return true;
            case R.id.menu_attachment:
                showAttachmentOptionsSnackBar();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAttachmentOptionsSnackBar() {
        if (isSnackShowing) {
            mSnackbar.dismiss();
            isSnackShowing = false;
            return;
        }
        View custom = LayoutInflater.from(this).inflate(R.layout.custom_snackbar, null);
        mSnackbar = Snackbar.make(mDateView, "", Snackbar.LENGTH_INDEFINITE);
        mSnackbar.getView().setPadding(0, 0, 0, 0);
        ((ViewGroup) mSnackbar.getView()).removeAllViews();
        ((ViewGroup) mSnackbar.getView()).addView(custom);

        View imageAttachment = custom.findViewById(R.id.image_attachment);
        View recordAttachment = custom.findViewById(R.id.record_attachment);
        View paintAttachment = custom.findViewById(R.id.paint_attachment);

        imageAttachment.setOnClickListener(imageAttachmentListener);
        recordAttachment.setOnClickListener(recordAttachmentListener);
        paintAttachment.setOnClickListener(paintAttachmentListener);

        mSnackbar.show();
        isSnackShowing = true;
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.string_select_picture)), SELECT_PICTURE_REQUEST_CODE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_note, menu);
        return true;
    }

    View.OnClickListener imageAttachmentListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openImagePicker();
            mSnackbar.dismiss();
        }
    };


    View.OnClickListener recordAttachmentListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openRecordingDialog();
            mSnackbar.dismiss();
        }
    };

    View.OnClickListener paintAttachmentListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(NewNoteActivity.this, PaintActivity.class);
            intent.putExtra(SELECTED_NOTE_ID, mNote.getId());
            startActivityForResult(intent, NOTE_ID_REQUEST_CODE);
            mSnackbar.dismiss();
        }
    };

    private void openRecordingDialog() {
        View customLayout = LayoutInflater.from(this).inflate(R.layout.recording_dialog, null);
        final TextView textViewStatus = customLayout.findViewById(R.id.textViewStatus);
        final ImageView statusView = customLayout.findViewById(R.id.play_pause);
        final Chronometer mChronometer = customLayout.findViewById(R.id.chronometer);
        final ImageButton deleteButton = customLayout.findViewById(R.id.deleteButton);
        final ImageButton saveButton = customLayout.findViewById(R.id.saveButton);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteMyFile(NewNoteActivity.this,
                        new File(APP_FOLDER.getAbsolutePath() + "/"
                                + mRecordingFileName).getAbsolutePath());
                mRecordingDialog.dismiss();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (mAudioRecorder != null)
                        mAudioRecorder.stop();
                } catch (IOException e) {
                    Log.d("HRD", "Error saving");
                    e.printStackTrace();
                }
                mChronometer.stop();
                recreate();
            }
        });

        statusView.setOnClickListener(new View.OnClickListener() {
            boolean isRecording = false;
            boolean isPaused = false;
            boolean canPauseResume = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
            long timeWhenStopped = 0;

            @Override
            public void onClick(View v) {
                if (canPauseResume) {
                    if (!isRecording) {
                        if (isPaused) {
                            resumeRecording();
                        } else {
                            startRecording();
                        }
                        statusView.setImageResource(R.drawable.ic_pause);
                        isRecording = true;
                    } else {
                        pauseRecording();
                        statusView.setImageResource(R.drawable.ic_play);
                        isRecording = false;
                        isPaused = true;
                    }
                } else {
                    if (!isRecording) {
                        startRecording();
                        statusView.setImageResource(R.drawable.ic_stop);
                        isRecording = true;
                    } else {
                        stopRecording();
                        statusView.setImageResource(R.drawable.ic_mic);
                        isRecording = false;
                    }
                }
            }

            private void startRecording() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissionsDenied()) {
                    requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS_CODE);
                }
                textViewStatus.setText("");
                mRecordingFileName = mNote.getId() + "_" + Calendar.getInstance().getTimeInMillis() + ".mp3";
                mAudioRecorder = new Utils.AudioRecorder(mRecordingFileName);
                try {
                    mAudioRecorder.start();
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                } catch (IOException e) {
//                    Log.d("HRD", "IOException");
                    e.printStackTrace();
                }
            }

            private void pauseRecording() {
                textViewStatus.setText("Tap to continue.");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mAudioRecorder.pause();
                }
                timeWhenStopped = mChronometer.getBase() - SystemClock.elapsedRealtime();
                mChronometer.stop();
            }

            private void resumeRecording() {
                textViewStatus.setText("");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mAudioRecorder.resume();
                }
                mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
                mChronometer.start();
            }

            private void stopRecording() {
                textViewStatus.setText("Tap to start.");
                try {
                    mAudioRecorder.stop();
                    mChronometer.stop();
                } catch (IOException e) {
//                    Log.d("HRD", "STOP ERROR");
                    e.printStackTrace();
                }
                timeWhenStopped = 0;
                Toast.makeText(mMediaService, "Saved", Toast.LENGTH_SHORT).show();
            }
        });

        mRecordingDialog = new AlertDialog.Builder(this, R.style.DialogTheme).create();
        mRecordingDialog.setCancelable(false);
        mRecordingDialog.setView(customLayout);
        mRecordingDialog.show();
        ;
    }

    private void openPlayerDialog() {
        View customLayout = LayoutInflater.from(this).inflate(R.layout.player_dialog, null);
        mTextViewDuration = customLayout.findViewById(R.id.textViewDuration);
        mPlayerPlayPause = customLayout.findViewById(R.id.play_pause);
        mPlayerSeekBar = customLayout.findViewById(R.id.playerSeekBar);
        final ImageButton deleteButton = customLayout.findViewById(R.id.deleteButton);
        final TextView cancel = customLayout.findViewById(R.id.textViewCancel);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteMyFile(NewNoteActivity.this,
                        new File(mSelectedAudioUri).getAbsolutePath());
                recreate();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayerDialog.dismiss();
                pauseMedia();
            }
        });

        mPlayerPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceAudioPaused) {
                    resumeMedia();
                } else if (!mPlayPressed) {
                    playMedia(mSelectedAudioUri);
                } else {
                    pauseMedia();
                }
                mPlayPressed = !mPlayPressed;
            }
        });

        mPlayerDialog = new AlertDialog.Builder(this, R.style.DialogTheme).create();
        mPlayerDialog.setCancelable(false);
        mPlayerDialog.setView(customLayout);
        mPlayerDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == NOTE_ID_REQUEST_CODE) {
                long previousNoteId = data.getLongExtra(SELECTED_NOTE_ID, -1L);
                // Log.d("HRD", "previousNoteId "+previousNoteId);

                if (previousNoteId > -1) {
                    mNote = mDataManager.getNote(previousNoteId);
                    displayViews();
                }
            }

            if (requestCode == SELECT_PICTURE_REQUEST_CODE) {
                Uri selectedImageUri = data.getData();
                String selectedImagePath = getPath(selectedImageUri);
                String selectedImageExtension = selectedImagePath.split("\\.")[1];
                String fileName = mNote.getId() + "_"
                        + Calendar.getInstance().getTimeInMillis()
                        + "." + selectedImageExtension;

                File destinationFile = new File(APP_FOLDER.getAbsolutePath() + "/" + fileName);
                // Log.d("HRD",  "DESTINATION "+destinationFile.getAbsolutePath());

                try {
                    copyFile(new File(selectedImagePath), destinationFile);
                    recreate();
                } catch (IOException e) {
                    Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
    }

    private String getPath(Uri uri) {
        if (uri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        // this is our fallback here
        return uri.getPath();
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


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaService.LocalBinder binder = (MediaService.LocalBinder) service;
            mMediaService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private class NoteMediaTask extends AsyncTask<Note, Void, List<Media>> {

        private final WeakReference<NewNoteActivity> mActivityReference;
        private Note mNote;

        NoteMediaTask(NewNoteActivity context) {
            mActivityReference = new WeakReference<>(context);
        }

        @Override
        protected List<Media> doInBackground(Note... notes) {
            mNote = notes[0];
            DataManager dataManager = DataManager.getInstance();
            return dataManager.getMediaList(mNote);
        }

        @Override
        protected void onPostExecute(final List<Media> mediaList) {
            mAdapter.setMediaList(mediaList);
        }
    }

    class MediaReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("HRD", "NewNoteActivity: onReceive");
            if (Objects.equals(intent.getAction(), MEDIA_SERVICE_INFO) && mPlayerPlayPause != null) {
                String playerState = intent.getStringExtra(PLAYER_STATE);
                int duration = intent.getIntExtra(MEDIA_DURATION, 0);
                int pos = intent.getIntExtra(MEDIA_POSITION, 0);

                String posText = padLeftZeros(String.valueOf(pos / 60), 2) + ":"
                        + padLeftZeros(String.valueOf(pos % 60), 2);

                if (playerState.equals(PlayerState.PLAYING.name())) {
                    mPlayerSeekBar.setMax(duration);
                    mTextViewDuration.setText(posText);
                    mPlayerPlayPause.setImageResource(R.drawable.ic_pause);
                    mPlayerSeekBar.setProgress(pos);

                } else if (playerState.equals(PlayerState.PAUSED.name())) {
                    mPlayerPlayPause.setImageResource(R.drawable.ic_play);
                } else if (playerState.equals(PlayerState.STOPPED.name())) {
                    mPlayPressed = false;
                    mPlayerPlayPause.setImageResource(R.drawable.ic_play);
                }
            }
        }

    }
}
