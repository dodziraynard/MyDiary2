package com.idea.mydiary.activities;

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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.snackbar.Snackbar;
import com.idea.mydiary.R;
import com.idea.mydiary.Utils;
import com.idea.mydiary.adapters.MediaAdapter;
import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;
import com.idea.mydiary.services.MediaService;
import com.idea.mydiary.types.MediaType;
import com.idea.mydiary.types.PlayerState;
import com.idea.mydiary.viewmodels.NewNoteActivityViewModel;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import static com.idea.mydiary.Utils.copyFile;
import static com.idea.mydiary.Utils.deleteMyFile;
import static com.idea.mydiary.Utils.enableStrictModeAll;
import static com.idea.mydiary.Utils.getPathFromUri;
import static com.idea.mydiary.Utils.hideKeyboard;
import static com.idea.mydiary.Utils.padLeftZeros;
import static com.idea.mydiary.activities.MainActivity.SELECTED_NOTE_ID;
import static com.idea.mydiary.activities.PaintActivity.PAINTING_URL;
import static com.idea.mydiary.services.MediaService.MEDIA_DURATION;
import static com.idea.mydiary.services.MediaService.MEDIA_POSITION;
import static com.idea.mydiary.services.MediaService.PLAYER_STATE;

public class NewNoteActivity extends AppCompatActivity {

    public static final String SELECTED_IMAGE_URL = "SELECTED_IMAGE_URL";
    public static final String AUDIO_URI = "AUDIO_URI";
    public static final String SEEK_POSITION = "SEEK_POSITION";
    public static final String MEDIA_SERVICE_INFO = "MEDIA_SERVICE_INFO";
    public static final int REQUEST_CODE_PAINT_ACTIVITY = 2;
    public static final String SELECTED_MEDIA_ID = "MEDIA_ID";
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.idea.mydiary.PLAY";
    public static final String Broadcast_RESUME_AUDIO = "com.idea.mydiary.RESUME";
    public static final String Broadcast_PAUSE_AUDIO = "com.idea.mydiary.PAUSE";
    public static final String Broadcast_SEEK_AUDIO = "com.idea.mydiary.SEEK";
    public static final String Broadcast_STOP_AUDIO = "com.idea.mydiary.STOP";
    private static final int SELECT_PICTURE_REQUEST_CODE = 1;
    private static final String[] PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    private static final int REQUEST_PERMISSIONS_CODE = 0;
    private static final int PERMISSIONS_COUNT = PERMISSIONS.length;
    public static File APP_FOLDER;
    boolean mReceiverRegistered = false;
    private Note mNote = null;
    private EditText mEditTextNoteText;
    private EditText mTextViewNoteTitle;
    private TextView mDateView;
    private boolean mIsNoteCancel = false;
    private Calendar mCalendar;
    private Snackbar mSnackbar;
    private boolean isSnackShowing = false;
    private RewardedAd rewardedAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getString(R.string.string_edit_note));
        enableStrictModeAll();
        initViews();

        MobileAds.initialize(this);

        rewardedAd = new RewardedAd(this,
                getString(R.string.reward_ad_unit_id));
        RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
            @Override
            public void onRewardedAdLoaded() {
            }

            @Override
            public void onRewardedAdFailedToLoad(LoadAdError loadAdError) {
                super.onRewardedAdFailedToLoad(loadAdError);
                Log.d("HRD", "onRewardedAdFailedToLoad " + loadAdError.getMessage());
            }
        };
        rewardedAd.loadAd(new AdRequest.Builder().build(), adLoadCallback);

        APP_FOLDER = new Utils(this).getAppFolder();

        if (savedInstanceState != null)
            restoreSavedState(savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(NewNoteActivityViewModel.class);

        Intent intent = getIntent();
        long selectedNoteId = intent.getLongExtra(SELECTED_NOTE_ID, -1);
        if (selectedNoteId != -1) {
            mViewModel.setCurrentNoteId(selectedNoteId);
        }

        if (mViewModel.getCurrentNoteId() == -1) {
            mNote = new Note("", Calendar.getInstance().getTimeInMillis(), "");
            mViewModel.setCurrentNoteIsNew(true);
            displayViews();
        } else {
            mViewModel.getNote(mViewModel.getCurrentNoteId()).observe(this, note -> {
                mNote = note;
                displayViews();
            });
        }

        final DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, month);
            mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            mNote.setDate(mCalendar.getTimeInMillis());
            displayViews();
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

    View.OnClickListener imageAttachmentListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openImagePicker();
            mSnackbar.dismiss();
            isSnackShowing = false;
        }
    };
    View.OnClickListener paintAttachmentListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(NewNoteActivity.this, PaintActivity.class);
            intent.putExtra(SELECTED_NOTE_ID, mNote.getId());
            startActivityForResult(intent, REQUEST_CODE_PAINT_ACTIVITY);
            mSnackbar.dismiss();
            isSnackShowing = false;
        }
    };
    private RecyclerView mMediaRecyclerView;
    private boolean serviceBound = false;
    private MediaService mMediaService;
    private MediaReceiver receiver;
    private boolean mPlayerPaused = false;
    private Utils.AudioRecorder mAudioRecorder;
    private String mRecordingFileName;
    private AlertDialog mRecordingDialog;
    private AlertDialog mPlayerDialog;
    private Media mSelectedMedia;
    private TextView mTextViewDuration;
    private ImageView mPlayerPlayPause;
    private SeekBar mPlayerSeekBar;
    private boolean mPlayerStopped = true;
    private MediaAdapter mAdapter;
    private NewNoteActivityViewModel mViewModel;
    View.OnClickListener recordAttachmentListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openRecordingDialog();
            mSnackbar.dismiss();
            isSnackShowing = false;
        }
    };
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaService.LocalBinder binder = (MediaService.LocalBinder) service;
            mMediaService = binder.getService();
            serviceBound = true;
            Log.d("HRD", "Service registered");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void restoreSavedState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey("ReceiverState")) {
            mReceiverRegistered = savedInstanceState.getBoolean("ReceiverState");
        }

        if (!mReceiverRegistered) {
            receiver = new MediaReceiver();
            registerReceiver(receiver, new IntentFilter("MEDIA_PLAYER_INFO"));
            mReceiverRegistered = true;
        }
        if (!serviceBound) {
            Intent playerIntent = new Intent(this, MediaService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void playMedia(String uri) {
        if (!serviceBound) {
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
    }

    private void seekMediaTo(int pos) {
        Intent broadcastIntent = new Intent(Broadcast_SEEK_AUDIO);
        broadcastIntent.putExtra(SEEK_POSITION, pos);
        sendBroadcast(broadcastIntent);
    }

    private void pauseMedia() {
        Intent broadcastIntent = new Intent(Broadcast_PAUSE_AUDIO);
        sendBroadcast(broadcastIntent);
    }

    private void stopMedia() {
        Intent broadcastIntent = new Intent(Broadcast_STOP_AUDIO);
        sendBroadcast(broadcastIntent);
    }

    private void displayViews() {
        if (mNote == null) return;
        mEditTextNoteText.setText(mNote.getText());
        mTextViewNoteTitle.setText(mNote.getTitle());
        mDateView.setText(mNote.getFullDate());

        mMediaRecyclerView.setAdapter(mAdapter);
        mMediaRecyclerView.setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false));
        mAdapter.setOnImageButtonClickListener(new MediaAdapter.OnItemClickListener() {
            @Override
            public void onImageButtonClickListener(Media media) {
                Intent intent = new Intent(NewNoteActivity.this, ImageViewActivity.class);
                intent.putExtra(SELECTED_MEDIA_ID, media.getId());
                intent.putExtra(SELECTED_IMAGE_URL, media.getUrl());
                startActivity(intent);
            }

            @Override
            public void onAudioButtonClickListener(Media media) {
                mSelectedMedia = media;
                openPlayerDialog();
            }
        });

        if (!mViewModel.currentNoteIsNew()) {
            mViewModel.getNoteMedia(mViewModel.getCurrentNoteId()).observe(this, new Observer<List<Media>>() {
                @Override
                public void onChanged(List<Media> media) {
                    mAdapter.setMediaList(media);
                }
            });
        }
    }

    private void initViews() {
        mEditTextNoteText = findViewById(R.id.editTextNoteText);
        mTextViewNoteTitle = findViewById(R.id.editTextNoteTitle);
        mDateView = findViewById(R.id.textViewDate);
        mCalendar = Calendar.getInstance();
        mMediaRecyclerView = findViewById(R.id.media_recycler_view);
        mAdapter = new MediaAdapter(this);

        if (mNote != null) {
            mEditTextNoteText.setText(mNote.getText());
            mTextViewNoteTitle.setText(mNote.getTitle());
            mDateView.setText(mNote.getFullDate());
        }
    }

    @Override
    protected void onPause() {
        saveNote();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
//        outState.putString(STATE_AUDIO, mSelectedMedia);
        outState.putBoolean("ServiceState", serviceBound);
        outState.putBoolean("ReceiverState", mReceiverRegistered);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissionsDenied()) {
            mViewModel.setCanShowAd(false);
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        }
    }

    private boolean saveNote() {
        if (mIsNoteCancel) {
            mViewModel.deleteNote(mNote);
            return false;
        }
        String title = mTextViewNoteTitle.getText().toString();
        String text = mEditTextNoteText.getText().toString();

        if (title.isEmpty() || text.isEmpty()) {
            mViewModel.deleteNote(mNote);
            Toast.makeText(this, "Please enter note title and input",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        mNote.setTitle(title);
        mNote.setText(text);
        mNote.setBackedUp(false);
        if (mViewModel.currentNoteIsNew()) {
            long id = mViewModel.insertNote(mNote);
            mNote.setId(id);
            mViewModel.setCurrentNoteIsNew(false);
            mViewModel.setCurrentNoteId(id);
        } else {
            mViewModel.updateNote(mNote);
        }
        return true;
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
            case R.id.menu_delete:
                mIsNoteCancel = true;
                onBackPressed();
                return true;
            case R.id.menu_attachment:
                hideKeyboard(this);
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

        if (!saveNote()) {
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
                deleteMyFile(NewNoteActivity.this, mRecordingFileName);
                mRecordingDialog.dismiss();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    if (mAudioRecorder != null && mRecordingFileName != null && !mRecordingFileName.isEmpty()) {
                        mAudioRecorder.stop();
                        mSelectedMedia = new Media(MediaType.AUDIO.name(), mRecordingFileName,
                                mViewModel.getCurrentNoteId());
                        mViewModel.insertMedia(mSelectedMedia);
                    }
                } catch (IOException | IllegalStateException e) {
                    Log.d("HRD", "Error saving");
                    e.printStackTrace();
                }
                mChronometer.stop();
                mAudioRecorder = null;
                mRecordingDialog.dismiss();
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
                mRecordingFileName = APP_FOLDER.getAbsolutePath() + "/"
                        + Calendar.getInstance().getTimeInMillis() + ".mp3";
                mAudioRecorder = new Utils.AudioRecorder(mRecordingFileName);
                try {
                    mAudioRecorder.start();
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                } catch (IOException e) {
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
    }

    private void openPlayerDialog() {
        View customLayout = LayoutInflater.from(this).inflate(R.layout.player_dialog_delete, null);
        mTextViewDuration = customLayout.findViewById(R.id.textViewDuration);
        mPlayerPlayPause = customLayout.findViewById(R.id.play_pause);
        mPlayerSeekBar = customLayout.findViewById(R.id.playerSeekBar);
        final ImageButton deleteButton = customLayout.findViewById(R.id.deleteButton);
        final TextView cancel = customLayout.findViewById(R.id.textViewCancel);

        mPlayerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    seekMediaTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMedia();
                mViewModel.deleteMedia(mSelectedMedia);
                mPlayerDialog.dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMedia();
                mPlayerDialog.dismiss();
            }
        });

        mPlayerPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerPaused) {
                    resumeMedia();
                    Log.d("HRD", "RESUME");
                } else if (mPlayerStopped) {
                    playMedia(mSelectedMedia.getUrl());
                    Log.d("HRD", "playMedia " + mSelectedMedia.getUrl());
                } else {
                    pauseMedia();
                    Log.d("HRD", "pauseMedia");
                }
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
            if (requestCode == REQUEST_CODE_PAINT_ACTIVITY) {
                String url = data.getStringExtra(PAINTING_URL);
                if (url != null && !url.isEmpty()) {
                    Media media = new Media(MediaType.IMAGE.name(), url, mViewModel.getCurrentNoteId());
                    mViewModel.insertMedia(media);
                }
            } else if (requestCode == SELECT_PICTURE_REQUEST_CODE) {
                Uri selectedImageUri = data.getData();
                new HandleSelectedImageAsyncTask(this).execute(selectedImageUri);
            }
            displayViews();
        }
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
            mViewModel.setCanShowAd(true);
            onResume();
        }
    }

    private static class HandleSelectedImageAsyncTask extends AsyncTask<Uri, Void, String> {
        private final WeakReference<NewNoteActivity> mActivityReference;

        private HandleSelectedImageAsyncTask(NewNoteActivity context) {
            mActivityReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(Uri... params) {
            NewNoteActivity activity = mActivityReference.get();

            Uri selectedImageUri = params[0];
            String selectedImagePath = getPathFromUri(activity, selectedImageUri);
            File destinationFile = new File(APP_FOLDER.getAbsolutePath() + "/" + selectedImagePath);
            try {
                copyFile(new File(selectedImagePath), destinationFile);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("HRD", e.getMessage());
                return null;
            }
            return destinationFile.getAbsolutePath();
        }

        @Override
        protected void onPostExecute(String url) {
            if (url != null && !url.isEmpty()) {
                NewNoteActivity activity = mActivityReference.get();
                Media media = new Media(MediaType.IMAGE.name(), url,
                        activity.mViewModel.getCurrentNoteId());
                activity.mViewModel.insertMedia(media);
            }

        }
    }

    @Override
    public void onBackPressed() {
        showAd();
        super.onBackPressed();
    }

    private void showAd() {
        if (rewardedAd.isLoaded() && mViewModel.isCanShowAd()) {
            Activity activityContext = NewNoteActivity.this;
            RewardedAdCallback adCallback = new RewardedAdCallback() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem reward) {
                    Toast.makeText(activityContext, "Congratulation !", Toast.LENGTH_SHORT).show();
                }
            };
            rewardedAd.show(activityContext, adCallback);
        }
    }

    class MediaReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("HRD", "RECEIVE");
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
                    mPlayerPaused = false;
                    mPlayerStopped = false;
                } else if (playerState.equals(PlayerState.PAUSED.name())) {
                    mPlayerPaused = true;
                    mPlayerStopped = false;
                    mPlayerPlayPause.setImageResource(R.drawable.ic_play);
                } else if (playerState.equals(PlayerState.STOPPED.name())) {
                    mPlayerStopped = true;
                    mPlayerPaused = false;
                    mPlayerPlayPause.setImageResource(R.drawable.ic_play);
                }
            }
        }
    }
}
