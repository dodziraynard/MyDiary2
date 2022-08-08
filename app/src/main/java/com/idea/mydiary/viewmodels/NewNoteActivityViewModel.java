package com.idea.mydiary.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;
import com.idea.mydiary.repositories.MyDiaryRepository;

import java.util.List;

public class NewNoteActivityViewModel extends AndroidViewModel {
    private final MyDiaryRepository mRepository;
    private boolean isNewNote = false;
    private long currentNoteId = -1;

    public NewNoteActivityViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MyDiaryRepository(application);
    }

    public LiveData<Note> getNote(long id) {
        return mRepository.getNote(id);
    }

    public long insertNote(Note note) {
        return mRepository.insertNoteAndReturnId(note);
    }

    public void updateNote(Note note) {
        mRepository.updateNote(note);
    }

    public void deleteNote(Note note) {
        mRepository.deleteNote(note);
    }

    public LiveData<List<Media>> getNoteMedia(long noteId) {
        return mRepository.getNoteMedia(noteId);
    }

    public void insertMedia(Media media) {
        mRepository.insertMedia(media);
    }

    public void deleteMedia(Media media) {
        mRepository.deleteMedia(media);
    }

    public boolean currentNoteIsNew() {
        return isNewNote;
    }

    public void setCurrentNoteIsNew(boolean newNote) {
        isNewNote = newNote;
    }

    public long getCurrentNoteId() {
        return currentNoteId;
    }

    public void setCurrentNoteId(long id) {
        currentNoteId = id;
    }
}
