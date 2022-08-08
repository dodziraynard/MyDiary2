package com.idea.mydiary.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.idea.mydiary.models.Note;

import java.util.List;

import static com.idea.mydiary.models.Note.NOTE_TABLE;

@Dao
public interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNote(Note note);

    @Delete
    void deleteNote(Note note);

    @Update
    void updateNote(Note note);

    @Query("SELECT * from " + NOTE_TABLE + " ORDER BY id ASC")
    LiveData<List<Note>> getAllNotes();

    @Query("SELECT * from " + NOTE_TABLE + " WHERE is_backed_up = :isBackedUp " + " ORDER BY id ASC")
    LiveData<List<Note>> getBackedUpNotes(boolean isBackedUp);

    @Query("SELECT * FROM " + NOTE_TABLE + " WHERE id = :id")
    LiveData<Note> getNote(long id);
}




