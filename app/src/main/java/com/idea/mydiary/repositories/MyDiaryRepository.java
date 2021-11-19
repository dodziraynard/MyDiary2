package com.idea.mydiary.repositories;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.idea.mydiary.DAOs.MediaDao;
import com.idea.mydiary.DAOs.NoteDao;
import com.idea.mydiary.MyDiaryRoomDatabase;
import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.idea.mydiary.Utils.deleteMyFile;

public class MyDiaryRepository {

    public static final String TAG = "MyDiaryRepository";
    private NoteDao mNoteDao;
    private MediaDao mMediaDao;

    private LiveData<List<Note>> mNotes;

    Application mApplication;
    private final MyDiaryRoomDatabase mDb;

    public MyDiaryRepository(Application application) {
        mDb = MyDiaryRoomDatabase.getDatabase(application);
        mNoteDao = mDb.mNoteDao();
        mNotes = mNoteDao.getAllNotes();

        mMediaDao = mDb.mMediaDao();
        mApplication = application;
    }

    public LiveData<List<Note>> getAllNotes() {
        Log.d(TAG, "getAllNotes");
        return mNotes;
    }

    public LiveData<List<Note>> getBackedUpNotes(boolean isBackedUp) {
        Log.d(TAG, "getBackedUpNotes");
        return mNoteDao.getBackedUpNotes(isBackedUp);
    }

    public LiveData<Note> getNote(long id) {
        Log.d(TAG, "getNote " + id);
        return mNoteDao.getNote(id);
    }

    public void deleteNote(Note note) {
        new DeleteNoteAsyncTask(mDb).execute(note);
    }

    public void updateNote(Note note) {
        new UpdateNoteAsyncTask(mDb).execute(note);
        Log.d(TAG, "updateNote");
    }

    public LiveData<List<Media>> getNoteMedia(long noteId) {
        Log.d(TAG, "getNoteMedia");
        return mMediaDao.getNoteMedia(noteId);
    }

    public void insertNote(Note note) {
        mNoteDao.insertNote(note);
    }

    public long insertNoteAndReturnId(Note note) {
        Callable<Long> insertCallable = new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return mNoteDao.insertNote(note);
            }
        };
        long rowId = 0;

        Future<Long> future = MyDiaryRoomDatabase.databaseWriteExecutor.submit(insertCallable);
        try {
            rowId = future.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
        }
        Log.d(TAG, "NOTE INSERTED " + rowId);
        return rowId;
    }

    public void insertMedia(Media media) {
        new InsertMediaAsyncTask(mDb).execute(media);
    }

    public void deleteMedia(Media media) {
        new DeleteMediaAsyncTask(mDb, mApplication).execute(media);
    }

    public LiveData<Media> getMedia(long id) {
        return mMediaDao.getMedia(id);
    }

    public LiveData<List<Media>> getBackedUpMedia(boolean isBackedUp) {
        return mMediaDao.getBackedUpMedia(isBackedUp);
    }

    public void updateMedia(Media media) {
        new UpdateMediaAsyncTask(mDb).execute(media);
        Log.d(TAG, "updateMedia");
    }

    public List<Media> getAllMedia() {
        Callable<List<Media>> getAllMediaCallable = new Callable<List<Media>>() {
            @Override
            public List<Media> call() throws Exception {
                return mMediaDao.getAllMedia();
            }
        };
        List<Media> mMediaList = null;

        Future<List<Media>> future = MyDiaryRoomDatabase.databaseWriteExecutor.submit(getAllMediaCallable);
        try {
            mMediaList = future.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
        }
        return mMediaList;
    }

    private static class DeleteMediaAsyncTask extends AsyncTask<Media, Void, Void> {

        private MyDiaryRoomDatabase mDb;
        private Application mApplication;

        DeleteMediaAsyncTask(MyDiaryRoomDatabase db, Application application) {
            mDb = db;
            mApplication = application;
        }

        @Override
        protected Void doInBackground(final Media... params) {
            Media media = params[0];
            mDb.mMediaDao().deleteMedia(media);
            deleteMyFile(mApplication, media.getUrl());
            Log.d(TAG, "MEDIA DELETED");
            return null;
        }
    }

    private static class DeleteNoteAsyncTask extends AsyncTask<Note, Void, Void> {

        private MyDiaryRoomDatabase mDb;

        DeleteNoteAsyncTask(MyDiaryRoomDatabase db) {
            mDb = db;
        }

        @Override
        protected Void doInBackground(final Note... params) {
            Note note = params[0];
            if (note == null) return null;
            List<Media> mediaList = mDb.mMediaDao().getNoteMediaNoLiveData(note.getId());
            mDb.mMediaDao().deleteMedia(mediaList);
            mDb.mNoteDao().deleteNote(note);

            Log.d(TAG, "Media Deleted");
            Log.d(TAG, "Note Deleted " + note.getId());
            return null;
        }
    }

    private static class InsertMediaAsyncTask extends AsyncTask<Media, Void, Void> {

        private MyDiaryRoomDatabase mDb;

        InsertMediaAsyncTask(MyDiaryRoomDatabase db) {
            mDb = db;
        }

        @Override
        protected Void doInBackground(final Media... params) {
            mDb.mMediaDao().insertMedia(params[0]);
            return null;
        }
    }

    private static class UpdateNoteAsyncTask extends AsyncTask<Note, Void, Void> {

        private MyDiaryRoomDatabase mDb;

        UpdateNoteAsyncTask(MyDiaryRoomDatabase db) {
            mDb = db;
        }

        @Override
        protected Void doInBackground(final Note... params) {
            mDb.mNoteDao().updateNote(params[0]);
            return null;
        }
    }

    private static class UpdateMediaAsyncTask extends AsyncTask<Media, Void, Void> {

        private MyDiaryRoomDatabase mDb;

        UpdateMediaAsyncTask(MyDiaryRoomDatabase db) {
            mDb = db;
        }

        @Override
        protected Void doInBackground(final Media... params) {
            mDb.mMediaDao().updateMedia(params[0]);
            return null;
        }
    }
}
