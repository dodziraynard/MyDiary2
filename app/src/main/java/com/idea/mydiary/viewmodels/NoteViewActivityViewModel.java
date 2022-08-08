package com.idea.mydiary.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;
import com.idea.mydiary.repositories.MyDiaryRepository;

import java.util.List;

public class NoteViewActivityViewModel extends AndroidViewModel {

    private final MyDiaryRepository mRepository;

    public NoteViewActivityViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MyDiaryRepository(application);
    }

    public LiveData<Note> getNote(long id) {
        return mRepository.getNote(id);
    }

    public LiveData<List<Media>> getNoteMedia(long noteId) {
        return mRepository.getNoteMedia(noteId);
    }
}
