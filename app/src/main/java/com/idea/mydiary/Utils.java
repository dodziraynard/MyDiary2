package com.idea.mydiary;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class Utils {
    public static final File APP_FOLDER = new File(
            Environment.getExternalStorageDirectory().toString() + "/MyDiary");

    public static <T> boolean contains(final T[] array, final T v) {
        if (v == null) {
            for (final T e : array)
                if (e == null)
                    return true;
        } else {
            for (final T e : array)
                if (e == v || v.equals(e))
                    return true;
        }

        return false;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try (
                FileChannel source = new FileInputStream(sourceFile).getChannel();
                FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
        }
    }

    public static void enableStrictModeAll() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
                    .penaltyLog()
                    .build());
        }
    }

    public static boolean deleteMyFile(Context context, String url){
        File file = new File(url);
        boolean deleted = file.delete();
        if(deleted) return  true;
        if(file.exists()){
            try {
                boolean d = file.getCanonicalFile().delete();
                if(d) return  true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(file.exists()){
                context.deleteFile(file.getName());
            }
        }
        return false;
    }

    public static String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
    }

    public static class AudioRecorder {

        final MediaRecorder recorder = new MediaRecorder();
        public final String fileName;

        public AudioRecorder(String fileName) {
            this.fileName = sanitizePath(fileName);
        }

        private String sanitizePath(String fileName) {
            if (!fileName.startsWith("/")) {
                fileName = "/" + fileName;
            }
            if (!fileName.contains(".")) {
                fileName += ".mp3";
            }
            return APP_FOLDER.getAbsolutePath() + fileName;
        }

        public void start() throws IOException {
            String state = android.os.Environment.getExternalStorageState();
            if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
                throw new IOException("SD Card is not mounted.  It is " + state
                        + ".");
            }

            // make sure the directory we plan to store the recording in exists
            File directory = new File(fileName).getParentFile();
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Path to file could not be created.");
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(fileName);
            recorder.prepare();
            recorder.start();
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        public void pause() {
            recorder.pause();
        }

        public void resume() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recorder.resume();
            }
        }

        public void stop() throws IOException {
            recorder.stop();
            recorder.release();
        }

        public static void playRecording(String path) throws IOException {
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(path);
            mp.prepare();
            mp.start();
//            mp.setVolume(10, 10);
        }
    }
}
