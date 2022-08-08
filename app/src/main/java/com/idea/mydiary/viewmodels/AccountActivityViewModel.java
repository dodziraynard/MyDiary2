package com.idea.mydiary.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;
import com.idea.mydiary.repositories.MyDiaryRepository;

import java.util.List;

public class AccountActivityViewModel extends AndroidViewModel {
    private final MyDiaryRepository mRepository;

    private final MutableLiveData<Boolean> isProcessing = new MutableLiveData<>();


    public AccountActivityViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MyDiaryRepository(application);
        isProcessing.postValue(false);
    }

    public LiveData<Boolean> isProcessing() {
        return isProcessing;
    }

    public void setProcessing(boolean processing) {
        isProcessing.setValue(processing);
    }

    public LiveData<List<Note>> getBackedUpNotes(boolean isBackedUp) {
        return mRepository.getBackedUpNotes(isBackedUp);
    }

    public LiveData<List<Media>> getBackedUpMedia(boolean isBackedUp) {
        return mRepository.getBackedUpMedia(isBackedUp);
    }

    public void updateNote(Note note) {
        mRepository.updateNote(note);
    }

    public void updateMedia(Media media) {
        mRepository.updateMedia(media);
    }

    public void insertNote(Note note) {
        mRepository.insertNoteAndReturnId(note);
    }

    public void insertMedia(Media media) {
        mRepository.insertMedia(media);
    }
}
