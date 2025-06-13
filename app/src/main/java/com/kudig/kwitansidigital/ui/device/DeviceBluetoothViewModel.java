package com.kudig.kwitansidigital.ui.device;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kudig.kwitansidigital.data.repository.BluetoothRepository;

import java.util.List;

public class DeviceBluetoothViewModel extends AndroidViewModel {
    private BluetoothRepository bluetoothRepository;
    private final MutableLiveData<List<BluetoothDevice>> _discoveredDevices = new MutableLiveData<>();

    public LiveData<List<BluetoothDevice>> getDiscoveredDevices() {
        return _discoveredDevices;
    }

    private final MutableLiveData<Boolean> _bluetoothEnabledStatus = new MutableLiveData<>();

    public LiveData<Boolean> getBluetoothEnabledStatus() {
        return _bluetoothEnabledStatus;
    }

    private final MutableLiveData<String> _pairingStatus = new MutableLiveData<>();

    public LiveData<String> getPairingStatus() {
        return _pairingStatus;
    }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    public DeviceBluetoothViewModel(@NonNull Application application) {
        super(application);
        bluetoothRepository = new BluetoothRepository(application);

        // Menggunakan postValue karena mungkin dipanggil dari background thread
        bluetoothRepository.getDiscoveredDevices().observeForever(_discoveredDevices::postValue);

        bluetoothRepository.getBluetoothEnabledStatus().observeForever(_bluetoothEnabledStatus::postValue);

        bluetoothRepository.getPairingStatus().observeForever(_pairingStatus::postValue);

        bluetoothRepository.getErrorMessage().observeForever(_errorMessage::postValue);
    }

    // Metode untuk memulai pemindaian Bluetooth, delegasikan ke Repository
    public void startBluetoothScan() {
        bluetoothRepository.startDiscovery();
    }

    // Metode untuk memicu pairing perangkat, delegasikan ke Repository
    public void pairDevice(BluetoothDevice device) {
        bluetoothRepository.pairDevice(device);
    }

    // Metode untuk memulai pengelolaan Bluetooth (daftar receiver, dll.) dari Repository
    public void startBluetoothManagement() {
        bluetoothRepository.registerBluetoothReceiver();
        // Ketika manajemen dimulai, perbarui status Bluetooth saat ini
        _bluetoothEnabledStatus.postValue(bluetoothRepository.isBluetoothEnabled()); // Menggunakan postValue untuk konsistensi
    }

    // Metode untuk menghentikan pengelolaan Bluetooth dari Repository
    public void stopBluetoothManagement() {
        bluetoothRepository.cancelDiscovery(); // Hentikan pemindaian jika masih berjalan
        bluetoothRepository.unregisterBluetoothReceiver();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopBluetoothManagement();
    }
}
