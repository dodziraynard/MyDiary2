package com.idea.mydiary.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;
import com.idea.mydiary.repositories.MyDiaryRepository;

import java.util.ArrayList;
import java.util.List;

public class MainActivityViewModel extends AndroidViewModel {

    private final MyDiaryRepository mRepository;
    private final LiveData<List<Note>> mNotes;
    private final List<Media> mMediaList;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MyDiaryRepository(application);
        mNotes = mRepository.getAllNotes();
        mMediaList = mRepository.getAllMedia();
    }

    public LiveData<List<Note>> getAllNotes() {
        return mNotes;
    }

    public void deleteNote(Note note) {
        mRepository.deleteNote(note);
    }

    public List<Media> getNotesMedia(long id) {
        List<Media> mediaList = new ArrayList<>();
        for (Media media : mMediaList) {
            if (media.getNoteId() == id) {
                mediaList.add(media);
            }
        }
        return mediaList;
    }
}
