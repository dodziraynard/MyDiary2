package com.idea.mydiary;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class Utils {
    private final File APP_FOLDER;

    public Utils(Context context) {
        APP_FOLDER = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "/MyDiary");
    }

    public static <T> boolean contains(final T[] array, final T t) {
        for (final T e : array)
            if (e == t || t.equals(e))
                return true;
        return false;
    }

    public File getAppFolder() {
        return APP_FOLDER;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (destFile.getParentFile() != null && !destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = new FileInputStream(sourceFile).getChannel();
        FileChannel destination = new FileOutputStream(destFile).getChannel();
        destination.transferFrom(source, 0, source.size());
    }

    // Ensure that expensive tasks are not being run on the main
    // thread by displaying an alert while in DEBUG mode.
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

    public static boolean deleteMyFile(Context context, String path) {
        File file = new File(path);
        boolean deleted = file.delete();
        if (deleted) return true;
        if (file.exists()) {
            try {
                boolean d = file.getCanonicalFile().delete();
                if (d) return true;
            } catch (IOException | RuntimeException e) {
                e.printStackTrace();
            }
            if (file.exists()) {
                // The file is a private file associated with the context.
                return context.deleteFile(file.getName());
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


    /*
        Source: https://stackoverflow.com/questions/47260845/call-function-from-activity-to-close-the-soft-keyboard-android/47264942
     */
    public static void hideKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        // Find the currently focused view,
        // so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        // If no view currently has focus, create a new one,
        // just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static class AudioRecorder {
        final MediaRecorder recorder = new MediaRecorder();
        public final String fileName;

        public AudioRecorder(String fileName) {
            this.fileName = fileName;
        }

        public void start() throws IOException {
            String state = android.os.Environment.getExternalStorageState();
            if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
                throw new IOException("SD Card is not mounted.  It is " + state
                        + ".");
            }

            // make sure the directory we plan to store the recording in exists
            File directory = new File(fileName).getParentFile();
            if (directory == null || !directory.exists() && !directory.mkdirs()) {
                throw new IOException("Path to file could not be created.");
            }

            // File is found, record audio
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
    }

    public static String getPathFromUri(Context context, Uri uri) {
        if (uri == null) {
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show();
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor
                    .getColumnIndex(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        // this is our fallback here
        return uri.getPath();
    }
}
