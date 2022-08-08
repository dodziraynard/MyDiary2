package com.idea.mydiary.services;

import static com.idea.mydiary.views.NewNoteActivity.AUDIO_URI;
import static com.idea.mydiary.views.NewNoteActivity.MEDIA_SERVICE_INFO;
import static com.idea.mydiary.views.NewNoteActivity.SEEK_POSITION;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.idea.mydiary.types.PlayerState;
import com.idea.mydiary.views.NewNoteActivity;

import java.io.IOException;


public class MediaService extends IntentService implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener,
        AudioManager.OnAudioFocusChangeListener {
    public static final String PLAYER_STATE = "PLAYER_STATE";
    public static final String MEDIA_DURATION = "MEDIA_DURATION";
    public static final String MEDIA_POSITION = "MEDIA_POSITION";
    private int resumePosition = 0;
    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private Intent mSendMessageIntent;
    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;
    private String mAudioFile;

    public MediaService() {
        super("MediaService");
    }

    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();

        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.reset();

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        if (mAudioFile != null && !mAudioFile.isEmpty()) {
            try {
                mMediaPlayer.setDataSource(mAudioFile);
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
                stopSelf();
            }
            mMediaPlayer.prepareAsync();
        }
    }

    private void playMedia() {
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
        sendPlayerStateToActivity(PlayerState.PLAYING);
    }

    private void stopMedia() {
        sendPlayerStateToActivity(PlayerState.STOPPED);
        if (mMediaPlayer == null) return;
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
    }

    private void pauseMedia() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                resumePosition = mMediaPlayer.getCurrentPosition();
                sendPlayerStateToActivity(PlayerState.PAUSED);
            }
        }
    }

    private void resumeMedia() {
        if (mMediaPlayer != null) {
            if (!mMediaPlayer.isPlaying()) {
                mMediaPlayer.seekTo(resumePosition);
                mMediaPlayer.start();
                sendPlayerStateToActivity(PlayerState.PLAYING);
            }
        }
    }

    private boolean requestAudioFocus() {
        mAudioManager = (AudioManager) getSystemService(getApplication().AUDIO_SERVICE);
        int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void removeAudioFocus() {
        mAudioManager.abandonAudioFocus(this);
    }

    private final BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mAudioFile = intent.getStringExtra(AUDIO_URI);
            // A PLAY_NEW_AUDIO action received
            // Reset mediaPlayer to play the new Audio
            if (mMediaPlayer != null) {
                stopMedia();
                mMediaPlayer.reset();
            }
            initMediaPlayer();
        }
    };

    // Pause audio on ACTION_AUDIO_BECOMING_NOISY
    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseMedia();
        }
    };

    private final BroadcastReceiver pauseAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseMedia();
        }
    };

    private final BroadcastReceiver stopAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopMedia();
        }
    };

    private final BroadcastReceiver resumeAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            resumeMedia();
        }
    };

    private final BroadcastReceiver seekAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int position = intent.getIntExtra(SEEK_POSITION, -1);
            resumePosition = position * 1000;
            mMediaPlayer.seekTo(resumePosition);
        }
    };

    private void registerBroadCastReceivers() {
        IntentFilter filterBecomingNoisy = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        IntentFilter filterPlay = new IntentFilter(NewNoteActivity.Broadcast_PLAY_NEW_AUDIO);
        IntentFilter filterPause = new IntentFilter(NewNoteActivity.Broadcast_PAUSE_AUDIO);
        IntentFilter filterResume = new IntentFilter(NewNoteActivity.Broadcast_RESUME_AUDIO);
        IntentFilter filterSeek = new IntentFilter(NewNoteActivity.Broadcast_SEEK_AUDIO);
        IntentFilter filterStop = new IntentFilter(NewNoteActivity.Broadcast_STOP_AUDIO);

        registerReceiver(playNewAudio, filterPlay);
        registerReceiver(resumeAudio, filterResume);
        registerReceiver(pauseAudio, filterPause);
        registerReceiver(seekAudio, filterSeek);
        registerReceiver(stopAudio, filterStop);
        registerReceiver(becomingNoisyReceiver, filterBecomingNoisy);
    }

    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    // if at least one call exists or the phone is ringing
                    // pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mMediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle
                        // Start playing.
                        if (mMediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    // Binder given to clients
    private final IBinder iBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        while (mMediaPlayer != null) {
            try {
                int pos = mMediaPlayer.getCurrentPosition() / 1000 + 1;
                if (mMediaPlayer.isPlaying()) {
                    sendMediaPositionToActivity(pos);
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendPlayerStateToActivity(PlayerState playerState) {
        if (mMediaPlayer == null) return;
        mSendMessageIntent.putExtra(PLAYER_STATE, playerState.name());
        mSendMessageIntent.putExtra(MEDIA_DURATION, mMediaPlayer.getDuration() / 1000);
        sendBroadcast(mSendMessageIntent);
    }

    private void sendMediaPositionToActivity(int position) {
        mSendMessageIntent.putExtra(PLAYER_STATE, PlayerState.PLAYING.name());
        mSendMessageIntent.putExtra(MEDIA_POSITION, position);
        sendBroadcast(mSendMessageIntent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopMedia();
        stopSelf();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation.
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mMediaPlayer == null) initMediaPlayer();
                else if (!mMediaPlayer.isPlaying()) mMediaPlayer.start();
                mMediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mMediaPlayer.isPlaying()) mMediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Intent for sending message to the activity
        mSendMessageIntent = new Intent();
        mSendMessageIntent.setAction(MEDIA_SERVICE_INFO);

        if (!requestAudioFocus()) {
            //Could not gain focus
            stopSelf();
        }
        if (mMediaPlayer == null)
            initMediaPlayer();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            stopMedia();
            mMediaPlayer.release();
        }

        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        // Unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);
        unregisterReceiver(resumeAudio);
        unregisterReceiver(pauseAudio);
        unregisterReceiver(seekAudio);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        callStateListener();
        registerBroadCastReceivers();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
    }

    public class LocalBinder extends Binder {
        public MediaService getService() {
            return MediaService.this;
        }
    }
}
