package com.kudig.kwitansidigital.ui.preview;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PreviewViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public PreviewViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("");
    }

    public LiveData<String> getText() {
        return mText;
    }
}