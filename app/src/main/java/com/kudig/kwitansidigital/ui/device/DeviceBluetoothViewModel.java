package com.kudig.kwitansidigital.ui.device;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DeviceBluetoothViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public DeviceBluetoothViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("");
    }

    public LiveData<String> getText() {
        return mText;
    }
}