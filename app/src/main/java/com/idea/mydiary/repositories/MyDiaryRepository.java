package com.idea.mydiary.repositories;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.idea.mydiary.dao.MediaDao;
import com.idea.mydiary.dao.NoteDao;
import com.idea.mydiary.MyDiaryRoomDatabase;
import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.idea.mydiary.Utils.deleteMyFile;

public class MyDiaryRepository {
    private final NoteDao mNoteDao;
    private final MediaDao mMediaDao;
    private final LiveData<List<Note>> mNotes;
    Application mApplication;

    public MyDiaryRepository(Application application) {
        MyDiaryRoomDatabase mDb = MyDiaryRoomDatabase.getDatabase(application);
        mNoteDao = mDb.mNoteDao();
        mNotes = mNoteDao.getAllNotes();
        mMediaDao = mDb.mMediaDao();
        mApplication = application;
    }

    public LiveData<List<Note>> getAllNotes() {
        return mNotes;
    }

    public LiveData<List<Note>> getBackedUpNotes(boolean isBackedUp) {
        return mNoteDao.getBackedUpNotes(isBackedUp);
    }

    public LiveData<Note> getNote(long id) {
        return mNoteDao.getNote(id);
    }

    public void deleteNote(Note note) {
        Callable<Void> deleteCallable = () -> {
            if (note == null) return null;
            List<Media> mediaList = mMediaDao.getNoteMediaNoLiveData(note.getId());
            mMediaDao.deleteMedia(mediaList);
            mNoteDao.deleteNote(note);
            return null;
        };
        MyDiaryRoomDatabase.databaseWriteExecutor.submit(deleteCallable);
    }

    public void updateNote(Note note) {
        Callable<Void> updateCallable = () -> {
            mNoteDao.updateNote(note);
            return null;
        };
        MyDiaryRoomDatabase.databaseWriteExecutor.submit(updateCallable);
    }

    public LiveData<List<Media>> getNoteMedia(long noteId) {
        return mMediaDao.getNoteMedia(noteId);
    }

    public long insertNoteAndReturnId(Note note) {
        Callable<Long> insertCallable = () -> mNoteDao.insertNote(note);
        long rowId = 0;
        Future<Long> future = MyDiaryRoomDatabase.databaseWriteExecutor.submit(insertCallable);
        try {
            rowId = future.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
        }
        return rowId;
    }

    public void insertMedia(Media media) {
        Callable<Long> insertCallable = () -> mMediaDao.insertMedia(media);
        MyDiaryRoomDatabase.databaseWriteExecutor.submit(insertCallable);
    }

    public void deleteMedia(Media media) {
        Callable<Void> deleteCallable = () -> {
            mMediaDao.deleteMedia(media);
            return null;
        };
        MyDiaryRoomDatabase.databaseWriteExecutor.submit(deleteCallable);
    }

    public LiveData<Media> getMedia(long id) {
        return mMediaDao.getMedia(id);
    }

    public LiveData<List<Media>> getBackedUpMedia(boolean isBackedUp) {
        return mMediaDao.getBackedUpMedia(isBackedUp);
    }

    public void updateMedia(Media media) {
        Callable<Void> updateCallable = () -> {
            mMediaDao.updateMedia(media);
            return null;
        };
        MyDiaryRoomDatabase.databaseWriteExecutor.submit(updateCallable);
    }

    public List<Media> getAllMedia() {
        Callable<List<Media>> getAllMediaCallable = mMediaDao::getAllMedia;
        List<Media> mMediaList = null;
        Future<List<Media>> future =
                MyDiaryRoomDatabase.databaseWriteExecutor.submit(getAllMediaCallable);
        try {
            mMediaList = future.get();
        } catch (InterruptedException | ExecutionException e1) {
            e1.printStackTrace();
        }
        return mMediaList;
    }
}
