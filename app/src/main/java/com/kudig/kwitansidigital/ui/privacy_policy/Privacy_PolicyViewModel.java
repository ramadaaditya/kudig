package com.kudig.kwitansidigital.ui.privacy_policy;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class Privacy_PolicyViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public Privacy_PolicyViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("");
    }

    public LiveData<String> getText() {
        return mText;
    }
}