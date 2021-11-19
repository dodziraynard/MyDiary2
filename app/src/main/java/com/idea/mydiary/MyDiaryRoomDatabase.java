package com.idea.mydiary;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.idea.mydiary.DAOs.MediaDao;
import com.idea.mydiary.DAOs.NoteDao;
import com.idea.mydiary.models.DataManager;
import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Note.class, Media.class}, version = 6, exportSchema = true)
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
            synchronized (MyDiaryRoomDatabase.class) {
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

    //    //!!!!! START WITH FRESH DATA ON EACH LAUNCH
    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            databaseWriteExecutor.execute(() -> {
                NoteDao dao = INSTANCE.mNoteDao();
                MediaDao mediaDao = INSTANCE.mMediaDao();
                mediaDao.deleteAll();
                dao.deleteAll();

                List<Note> notes = DataManager.getInstance().getNotes();
                for (Note note : notes) {
                    note.setDate(Calendar.getInstance().getTimeInMillis());
                    dao.insertNote(note);
                }
            });
        }
    };
}
