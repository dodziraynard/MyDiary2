package com.idea.mydiary.views;

import static com.idea.mydiary.Utils.padLeftZeros;
import static com.idea.mydiary.services.MediaService.MEDIA_DURATION;
import static com.idea.mydiary.services.MediaService.MEDIA_POSITION;
import static com.idea.mydiary.services.MediaService.PLAYER_STATE;
import static com.idea.mydiary.views.MainActivity.SELECTED_NOTE_ID;
import static com.idea.mydiary.views.NewNoteActivity.AUDIO_URI;
import static com.idea.mydiary.views.NewNoteActivity.Broadcast_PAUSE_AUDIO;
import static com.idea.mydiary.views.NewNoteActivity.Broadcast_PLAY_NEW_AUDIO;
import static com.idea.mydiary.views.NewNoteActivity.Broadcast_RESUME_AUDIO;
import static com.idea.mydiary.views.NewNoteActivity.Broadcast_SEEK_AUDIO;
import static com.idea.mydiary.views.NewNoteActivity.Broadcast_STOP_AUDIO;
import static com.idea.mydiary.views.NewNoteActivity.MEDIA_SERVICE_INFO;
import static com.idea.mydiary.views.NewNoteActivity.SEEK_POSITION;
import static com.idea.mydiary.views.NewNoteActivity.SELECTED_IMAGE_URL;
import static com.idea.mydiary.views.NewNoteActivity.SELECTED_MEDIA_ID;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.idea.mydiary.R;
import com.idea.mydiary.adapters.MediaAdapter;
import com.idea.mydiary.databinding.ActivityNoteViewBinding;
import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;
import com.idea.mydiary.services.MediaService;
import com.idea.mydiary.types.PlayerState;
import com.idea.mydiary.viewmodels.NoteViewActivityViewModel;

import java.util.List;
import java.util.Objects;

public class NoteViewActivity extends AppCompatActivity {
    private boolean mPlayerStopped = true;
    private boolean mPlayerPaused = false;
    private boolean serviceBound = false;
    private boolean mReceiverRegistered = false;
    private Note mNote;
    private List<Media> mMediaList;
    private MediaAdapter mAdapter;
    private AlertDialog mPlayerDialog;
    private Media mSelectedMedia;
    private TextView mTextViewDuration;
    private ImageView mPlayerPlayPause;
    private SeekBar mPlayerSeekBar;
    private MediaService mMediaService;
    private NoteViewActivity.MediaReceiver receiver;
    private TextView mNoteTextView;
    private ActivityNoteViewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
            restoreSavedState(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_note_view);
        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initView();

        NoteViewActivityViewModel mViewModel =
                new ViewModelProvider(this).get(NoteViewActivityViewModel.class);

        Intent intent = getIntent();
        long selectedNoteId = intent.getLongExtra(SELECTED_NOTE_ID, -1);
        if (selectedNoteId != -1) {
            mViewModel.getNote(selectedNoteId).observe(this, note -> {
                mNote = note;
                TextView date = findViewById(R.id.textViewDate);
                mNoteTextView.setText(mNote.getText());
                date.setText(mNote.getFullDate());
                binding.toolbarLayout.setTitle(mNote.getTitle());
            });
        }

        mViewModel.getNoteMedia(selectedNoteId).observe(this, mediaList -> {
            mMediaList = mediaList;
            mAdapter.setMediaList(mMediaList);
        });

        binding.fab.setOnClickListener(view -> {
            Intent i = new Intent(NoteViewActivity.this, NewNoteActivity.class);
            i.putExtra(SELECTED_NOTE_ID, mNote.getId());
            startActivity(i);
        });

        // Retrieve preferred font size from shared preference.
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String fontSizeString = pref.getString("fontSize", "14");
        float fontSize = Float.parseFloat(fontSizeString);
        mNoteTextView.setTextSize(fontSize);
    }

    private void initView() {
        mAdapter = new MediaAdapter(this);
        mNoteTextView = findViewById(R.id.noteTextView);

        RecyclerView recyclerView = findViewById(R.id.mediaRecyclerview);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false));

        mAdapter.setOnImageButtonClickListener(new MediaAdapter.OnItemClickListener() {
            @Override
            public void onImageButtonClickListener(Media media) {
                Intent intent = new Intent(NoteViewActivity.this, ImageViewActivity.class);
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
    }

    private void restoreSavedState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey("ReceiverState")) {
            mReceiverRegistered = savedInstanceState.getBoolean("ReceiverState");
        }

        if (!mReceiverRegistered) {
            receiver = new NoteViewActivity.MediaReceiver();
            registerReceiver(receiver, new IntentFilter("MEDIA_PLAYER_INFO"));
            mReceiverRegistered = true;
        }

        if (!serviceBound) {
            Intent playerIntent = new Intent(this, MediaService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void openPlayerDialog() {
        View customLayout = LayoutInflater.from(this).inflate(R.layout.player_dialog, null);
        mTextViewDuration = customLayout.findViewById(R.id.textViewDuration);
        mPlayerPlayPause = customLayout.findViewById(R.id.play_pause);
        mPlayerSeekBar = customLayout.findViewById(R.id.playerSeekBar);
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

        cancel.setOnClickListener(v -> {
            stopMedia();
            mPlayerDialog.dismiss();
        });

        mPlayerPlayPause.setOnClickListener(v -> {
            if (mPlayerPaused) {
                resumeMedia();
            } else if (mPlayerStopped) {
                playMedia(mSelectedMedia.getUrl());
            } else {
                pauseMedia();
            }
        });

        mPlayerDialog = new AlertDialog.Builder(this, R.style.DialogTheme).create();
        mPlayerDialog.setCancelable(false);
        mPlayerDialog.setView(customLayout);
        mPlayerDialog.show();
    }

    // Play audio in the background using services.
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

    // Communications with the media service.
    private void resumeMedia() {
        sendBroadcast(new Intent(Broadcast_RESUME_AUDIO));
    }
    private void seekMediaTo(int pos) {
        Intent broadcastIntent = new Intent(Broadcast_SEEK_AUDIO);
        broadcastIntent.putExtra(SEEK_POSITION, pos);
        sendBroadcast(broadcastIntent);
    }

    private void pauseMedia() {
        sendBroadcast(new Intent(Broadcast_PAUSE_AUDIO));
    }

    private void stopMedia() {
        sendBroadcast(new Intent(Broadcast_STOP_AUDIO));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
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
        receiver = new NoteViewActivity.MediaReceiver();
        registerReceiver(receiver, new IntentFilter(MEDIA_SERVICE_INFO));
        mReceiverRegistered = true;
    }

    // Service connection to the background audio player service.
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService,
            // cast the IBinder and get LocalService instance
            MediaService.LocalBinder binder = (MediaService.LocalBinder) service;
            mMediaService = binder.getService();
            serviceBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    class MediaReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
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