package com.idea.mydiary.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.idea.mydiary.models.Media;

import java.util.List;

import static com.idea.mydiary.models.Media.MEDIA_TABLE;

@Dao
public interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertMedia(Media media);

    @Delete
    void deleteMedia(List<Media> mediaList);

    @Delete
    void deleteMedia(Media media);

    @Query("SELECT * from " + MEDIA_TABLE + " WHERE is_backed_up = :isBackedUp " + " ORDER BY id ASC")
    LiveData<List<Media>> getBackedUpMedia(boolean isBackedUp);

    @Query("SELECT * from " + MEDIA_TABLE + " WHERE note_id == :noteId ORDER BY id ASC")
    LiveData<List<Media>> getNoteMedia(long noteId);

    @Query("SELECT * from " + MEDIA_TABLE + " WHERE note_id == :noteId ORDER BY id ASC")
    List<Media> getNoteMediaNoLiveData(long noteId);

    @Query("SELECT * FROM " + MEDIA_TABLE + " WHERE id == :id")
    LiveData<Media> getMedia(long id);

    @Update
    void updateMedia(Media media);

    @Query("SELECT * FROM " + MEDIA_TABLE)
    List<Media> getAllMedia();
}
