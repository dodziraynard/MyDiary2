package com.idea.mydiary.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;

@Entity(tableName = Note.NOTE_TABLE)
public class Note {
    @Ignore
    public static final String NOTE_TABLE = "note_table";

    @Ignore
    private SimpleDateFormat mSimpleDateFormat;

    @Ignore
    private static final String PATTERN = "dd MMM yyyy";

    @Ignore
    private static final String FULL_DATE_PATTERN = "E, dd MMM yyyy";

    @Ignore
    private Date mDateObj;

    @Ignore
    private String mDateString;

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String title;
    private String text;

    @ColumnInfo(name = "account_id")
    private String accountId;

    public long getDate() {
        return date;
    }

    private long date;

    public void setBackedUp(boolean backedUp) {
        isBackedUp = backedUp;
    }

    @ColumnInfo(name = "is_backed_up")
    private boolean isBackedUp;

    {
        mDateObj = new Date();
    }

    @Ignore
    public Note(String title, long date, String text) {
        this.title = title;
        this.date = date;
        this.text = text;
        this.id = date;
        this.isBackedUp = false;
    }

    public Note() {
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private void initDate() {
        mDateObj.setTime(date);
        mSimpleDateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
        mSimpleDateFormat.applyPattern(PATTERN);
        mDateString = mSimpleDateFormat.format(mDateObj);
    }

    public String getFullDate() {
        mDateObj.setTime(date);
        mSimpleDateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
        mSimpleDateFormat.applyPattern(FULL_DATE_PATTERN);
        return mSimpleDateFormat.format(mDateObj);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public boolean getIsBackedUp() {
        return isBackedUp;
    }

    public String getDay() {
        initDate();
        return mDateString.split(" ")[0];
    }

    public String getMonth() {
        initDate();
        return mDateString.split(" ")[1];
    }

    public String getYear() {
        initDate();
        return mDateString.split(" ")[2];
    }
}
