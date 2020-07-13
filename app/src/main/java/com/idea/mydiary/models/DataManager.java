package com.idea.mydiary.models;

import android.os.Environment;

import com.idea.mydiary.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    public static final String[] IMAGE_EXTENSIONS = new String[]{"png", "jpg", "jpeg"};
    public static final String[] AUDIO_EXTENSIONS = new String[]{"mp3", "wave"};
    private static DataManager ourInstance = null;
    private List<Note> mNotes = new ArrayList<>();
    private List<Media> mMediaList;

    public static DataManager getInstance() {
        if (ourInstance == null) {
            ourInstance = new DataManager();
            ourInstance.initializeExampleNotes();
        }
        return ourInstance;
    }

    private void initializeExampleNotes() {
        for (int i = 0; i < 20; i++) {
            String title = "Note " + i;
            String text = "Some really realy long text as the content of the note";
            Note note = new Note(title, 1322018752992l, text);
            note.setId(i);
            mNotes.add(note);
        }
    }

    public Note getNote(long id) {
        return mNotes.get((int) id);
    }

    public void addNote(Note note) {
        mNotes.add(note);
    }

    public List<Note> getNotes() {
        return mNotes;
    }

    public List<Media> getMediaList(Note note) {
        mMediaList = new ArrayList<>();
        long noteId = note.getId();

        File folder = new File(
                Environment.getExternalStorageDirectory().toString() + "/MyDiary");

        File f = new File(folder.getPath());
        File[] files = f.listFiles();
        if (files != null) {
            for (File file : files) {
                String[] nameParts = file.getName().split("/");
                String fileExtension = nameParts[nameParts.length - 1].split("\\.")[1];
                String noteIdFromName = nameParts[nameParts.length - 1].split("_")[0];

                try {
                    if (noteId == Long.parseLong(noteIdFromName)) {
                        boolean contains = Utils.contains(IMAGE_EXTENSIONS, fileExtension);
                        Media media;
                        if (contains) {
                            media = new Media(file.getAbsolutePath(), MediaType.IMAGE);
                        } else {
                            media = new Media(file.getAbsolutePath(), MediaType.AUDIO);
                        }
                        mMediaList.add(media);
                    }
                } catch (NumberFormatException e) {
                    // Images are saved using the format NOTEID_TIMEINMILL.png
                    // So if the name can't be split using '_' as a deliminator,
                    // then it is not saved by the application
                    e.printStackTrace();
                }

            }
        }

        return mMediaList;
    }
}
