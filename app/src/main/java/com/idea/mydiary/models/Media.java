package com.idea.mydiary.models;

import static com.idea.mydiary.types.MediaType.AUDIO;
import static com.idea.mydiary.types.MediaType.IMAGE;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.idea.mydiary.types.MediaType;

import java.util.Calendar;

@Entity(tableName = Media.MEDIA_TABLE)
public class Media {
    @Ignore
    public static final String MEDIA_TABLE = "media_table";

    public Media() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @PrimaryKey(autoGenerate = true)
    private long id;

    public void setMediaType(String mediaType) {
        mMediaType = mediaType;
    }

    @ColumnInfo(name = "media_type")
    private String mMediaType;

    public boolean isBackedUp() {
        return isBackedUp;
    }

    public void setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    @ColumnInfo(name = "download_url")
    private String downloadURL;

    public void setBackedUp(boolean backedUp) {
        isBackedUp = backedUp;
    }

    @ColumnInfo(name = "is_backed_up")
    private boolean isBackedUp = false;

    @Ignore
    public Media(String mediaType, String url, long noteId) {
        mMediaType = mediaType;
        mUrl = url;
        this.noteId = noteId;
        id = Calendar.getInstance().getTimeInMillis();
        isBackedUp = false;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }

    @ColumnInfo(name = "account_id")
    private String accountId;

    public void setUrl(String url) {
        mUrl = url;
    }

    @ColumnInfo(name = "url")
    private String mUrl;

    public void setNoteId(long noteId) {
        this.noteId = noteId;
    }

    @ColumnInfo(name = "note_id")
    private long noteId;

    public Media(String url, MediaType mediaType) {
        mMediaType = mediaType.name();
        mUrl = url;
    }

    public long getNoteId() {
        return noteId;
    }

    public String getMediaType() {
        return mMediaType;
    }

    public MediaType getMediaTypeEnum() {
        if (mMediaType.equals(AUDIO.name())) return AUDIO;
        return IMAGE;
    }

    public String getUrl() {
        return mUrl;
    }
}
