package com.idea.mydiary.models;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Note {
    private static final String PATTERN = "dd MMM yyyy";
    private static final String FULL_DATE_PATTERN = "E, dd MMM yyyy";
    private String mTitle;
    private String mText;
    private long mId;
    private long mDate = Calendar.getInstance().getTimeInMillis();
    private int isBackedUp;
    private SimpleDateFormat mSimpleDateFormat;
    private Date mDateObj;
    private String mDateString;

    //    TODO: REMOVE THIS
    {
        mId = 1;
    }
//

    {
        mDateObj = new Date();
    }

    public Note(String title, long date, String text) {
        mTitle = title;
        mDate = date;
        mText = text;
    }

    public Note() {
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    private void initDate() {
        mDateObj.setTime(mDate);
        mSimpleDateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
        mSimpleDateFormat.applyPattern(PATTERN);
        mDateString = mSimpleDateFormat.format(mDateObj);
    }

    @Override
    public String toString() {
        return "Note{" +
                "mTitle='" + mTitle + '\'' +
                ", mText='" + mText + '\'' +
                ", mDate=" + mDate +
                ", isBackedUp=" + isBackedUp +
                '}';
    }

    public String getFullDate() {
        mDateObj.setTime(mDate);
        mSimpleDateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
        mSimpleDateFormat.applyPattern(FULL_DATE_PATTERN);
        return mSimpleDateFormat.format(mDateObj);
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public long getDate() {
        return mDate;
    }

    public void setDate(long date) {
        mDate = date;
    }

    public boolean getIsBackedUp() {
        return isBackedUp == 1;
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
