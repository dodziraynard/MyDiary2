package com.idea.mydiary;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.idea.mydiary.dao.MediaDao;
import com.idea.mydiary.dao.NoteDao;
import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Note.class, Media.class}, version = 7, exportSchema = true)
public abstract class MyDiaryRoomDatabase extends RoomDatabase {

    public static final String MY_DIARY_DATABASE = "my_diary_database";

    public abstract NoteDao mNoteDao();

    public abstract MediaDao mMediaDao();

    private static volatile MyDiaryRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static MyDiaryRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized  (MyDiaryRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            MyDiaryRoomDatabase.class, MY_DIARY_DATABASE)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
