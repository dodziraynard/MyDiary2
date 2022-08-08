package com.idea.mydiary.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.idea.mydiary.models.Media;
import com.idea.mydiary.repositories.MyDiaryRepository;

public class ImageViewActivityViewModel extends AndroidViewModel {
    private final MyDiaryRepository mRepository;

    public ImageViewActivityViewModel(@NonNull Application application) {
        super(application);
        mRepository = new MyDiaryRepository(application);
    }

    public void deleteMedia(Media media) {
        mRepository.deleteMedia(media);
    }

    public LiveData<Media> getMedia(long id) {
        return mRepository.getMedia(id);
    }
}
