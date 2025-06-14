package com.kudig.kwitansidigital.ui.preview;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dantsu.escposprinter.EscPosPrinterCommands;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.kudig.kwitansidigital.data.repository.KwitansiRepository;
import com.kudig.kwitansidigital.db.KwitansiEntity;
import com.kudig.kwitansidigital.utils.BluetoothPrintersConnections;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PreviewViewModel extends AndroidViewModel {

    private final KwitansiRepository repository;

    // --- LiveData untuk Data Kwitansi ---
    private final MutableLiveData<KwitansiEntity> _kwitansiData = new MutableLiveData<>();
    public final LiveData<KwitansiEntity> kwitansiData = _kwitansiData;

    private final MutableLiveData<String> _formattedNominal = new MutableLiveData<>();
    public final LiveData<String> formattedNominal = _formattedNominal;

    private final MutableLiveData<String> _nominalTerbilang = new MutableLiveData<>();
    public final LiveData<String> nominalTerbilang = _nominalTerbilang;

    private final MutableLiveData<String> _currentDate = new MutableLiveData<>();
    public final LiveData<String> currentDate = _currentDate;

    private final MutableLiveData<String> _currentLocation = new MutableLiveData<>();
    public final LiveData<String> currentLocation = _currentLocation;

    // --- LiveData untuk Event & Aksi UI ---
    private final MutableLiveData<Event<String>> _toastMessage = new MutableLiveData<>();
    public final LiveData<Event<String>> toastMessage = _toastMessage;

    // BARU: LiveData untuk menampung daftar printer yang ditemukan
    private final MutableLiveData<List<BluetoothConnection>> _printerList = new MutableLiveData<>();
    public final LiveData<List<BluetoothConnection>> printerList = _printerList;


    public PreviewViewModel(@NonNull Application application) {
        super(application);
        repository = new KwitansiRepository(application);
    }

    public void loadKwitansiData(KwitansiEntity entity) {
        _kwitansiData.setValue(entity);
        // DIPERBAIKI: Mengirim 'long' dari entity yang sudah diperbaiki
        formatData(entity.getNominal());
        setCurrentDate();
        fetchCurrentLocation();
    }

    // DIPERBAIKI: Mengambil 'long' sebagai argumen
    private void formatData(long nominal) {
        Locale indonesianLocale = new Locale("id", "ID");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(indonesianLocale);
        String formatted = currencyFormatter.format(nominal);
        if (formatted.endsWith(",00")) {
            formatted = formatted.substring(0, formatted.length() - 3);
        }
        _formattedNominal.setValue(formatted.replace("Rp", "").trim());
        _nominalTerbilang.setValue(convertToTerbilang(nominal).trim() + " RUPIAH");
    }

    private void setCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        _currentDate.setValue(dateFormat.format(calendar.getTime()));
    }

    /**
     * DIPERBAIKI: Fungsi ini sekarang memanggil repository dengan argumen yang benar.
     */
    public void fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            _toastMessage.setValue(new Event("Izin lokasi tidak diberikan."));
            return;
        }

        LocationManager locationManager = (LocationManager) getApplication().getSystemService(Context.LOCATION_SERVICE);
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                // INI PERBAIKANNYA: Kirim 'location' sebagai argumen ke repository
                String address = repository.getAddressFromLocation(location);
                _currentLocation.setValue(address);
            } else {
                _currentLocation.setValue("Lokasi tidak ditemukan");
            }
        } catch (SecurityException e) {
            Log.e("ViewModel", "SecurityException on fetchCurrentLocation", e);
            _toastMessage.setValue(new Event("Gagal mengambil lokasi karena masalah izin."));
        }
    }

    // DIPERBAIKI: Mengambil 'long' sebagai argumen
    private String convertToTerbilang(long nominal) {
        String[] angka = {"", "SATU", "DUA", "TIGA", "EMPAT", "LIMA", "ENAM", "TUJUH", "DELAPAN", "SEMBILAN", "SEPULUH", "SEBELAS"};
        if (nominal < 12) return angka[(int) nominal];
        if (nominal < 20) return convertToTerbilang(nominal - 10) + " BELAS";
        if (nominal < 100)
            return (angka[(int) (nominal / 10)] + " PULUH " + convertToTerbilang(nominal % 10)).trim();
        if (nominal < 200) return ("SERATUS " + convertToTerbilang(nominal - 100)).trim();
        if (nominal < 1000)
            return (convertToTerbilang(nominal / 100) + " RATUS " + convertToTerbilang(nominal % 100)).trim();
        if (nominal < 2000) return ("SERIBU " + convertToTerbilang(nominal % 1000)).trim();
        if (nominal < 1_000_000)
            return (convertToTerbilang(nominal / 1000) + " RIBU " + convertToTerbilang(nominal % 1000)).trim();
        if (nominal < 1_000_000_000)
            return (convertToTerbilang(nominal / 1_000_000) + " JUTA " + convertToTerbilang(nominal % 1_000_000)).trim();
        return "";
    }

    /**
     * BARU: Metode untuk mencari printer. Ini akan dipanggil oleh Fragment.
     */
    public void findBluetoothPrinters() {
        try {
            BluetoothPrintersConnections printersConnections = new BluetoothPrintersConnections();
            List<BluetoothConnection> printers = printersConnections.getList();
            _printerList.postValue(printers);
        } catch (SecurityException e) {
            _toastMessage.postValue(new Event("Error: Izin Bluetooth Connect tidak diberikan."));
            Log.e("ViewModel", "SecurityException saat mencari printer", e);
        }
    }

    /**
     * BARU: Logika cetak yang lebih baik. Menerima printer yang sudah dipilih oleh pengguna.
     */
    public void doPrint(Bitmap bitmap, BluetoothConnection selectedPrinter) {
        if (selectedPrinter == null) {
            _toastMessage.setValue(new Event("Tidak ada printer yang dipilih."));
            return;
        }

        // Jalankan di background thread agar tidak memblokir UI
        new Thread(() -> {
            try {
                // ... (Logika rotasi dan skala bitmap bisa ditambahkan di sini) ...

                selectedPrinter.connect();
                EscPosPrinterCommands printerCommands = new EscPosPrinterCommands(selectedPrinter);
                printerCommands.reset();
                printerCommands.printImage(EscPosPrinterCommands.bitmapToBytes(bitmap, false));
                printerCommands.feedPaper(125);
                printerCommands.cutPaper();

                _toastMessage.postValue(new Event("Berhasil mencetak!"));
                if (_kwitansiData.getValue() != null) {
                    repository.insertHistory(_kwitansiData.getValue());
                }

            } catch (EscPosConnectionException e) {
                _toastMessage.setValue(new Event<>("Connection Error: " + e.getMessage()));
            } catch (Exception e) {
                Log.e("ViewModel", "Tidak dapat mencetak", e);
                _toastMessage.setValue(new Event<>("Gagal mencetak, silakan coba lagi."));
            }
        });
    }


    // Helper class for events
    public static class Event<T> {
        private T content;
        private boolean hasBeenHandled = false;

        public Event(T content) {
            this.content = content;
        }

        public T getContentIfNotHandled() {
            if (hasBeenHandled) {
                return null;
            } else {
                hasBeenHandled = true;
                return content;
            }
        }
    }
}
