package com.kudig.kwitansidigital.data.repository;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// BluetoothRepository bertanggung jawab untuk semua interaksi langsung dengan API Bluetooth.
// Ia mengisolasi detail implementasi dari ViewModel.
public class BluetoothRepository {

    private static final String TAG = "BluetoothRepository";
    private static final UUID PRINTER_SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // UUID standar untuk SPP

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final ExecutorService executorService; // Untuk operasi I/O di background thread
    private final Handler mainHandler; // Untuk posting hasil kembali ke UI thread

    private final MutableLiveData<List<BluetoothDevice>> _discoveredDevices = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<BluetoothDevice>> getDiscoveredDevices() {
        return _discoveredDevices;
    }

    private final MutableLiveData<Boolean> _bluetoothEnabledStatus = new MutableLiveData<>();

    public LiveData<Boolean> getBluetoothEnabledStatus() {
        return _bluetoothEnabledStatus;
    }

    private final MutableLiveData<String> _pairingStatus = new MutableLiveData<>(); // "PAIRING", "BONDED", "NONE"

    public LiveData<String> getPairingStatus() {
        return _pairingStatus;
    }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    // Receiver untuk menerima event Bluetooth
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // ACTION_FOUND seharusnya hanya terjadi jika izin scan sudah ada
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    List<BluetoothDevice> currentList = _discoveredDevices.getValue();
                    if (currentList == null) {
                        currentList = new ArrayList<>();
                    }
                    if (!currentList.contains(device)) {
                        currentList.add(device);
                        _discoveredDevices.postValue(currentList); // Post value to LiveData
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "Discovery finished.");
                // Setelah penemuan selesai, tambahkan perangkat yang sudah dipasangkan
                loadPairedDevices(); // Panggil ulang untuk memastikan daftar diperbarui dengan perangkat terpasang
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                // Izin BLUETOOTH_CONNECT diperlukan untuk mendapatkan perangkat
                if (!hasBluetoothConnectPermission()) {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not granted for BOND_STATE_CHANGED action.");
                    return;
                }
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (device != null) {
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        _pairingStatus.postValue("BONDED");
                        // Pairing berhasil, perangkat sekarang ada di daftar perangkat yang sudah terpasangkan
                        loadPairedDevices();
                    } else if (bondState == BluetoothDevice.BOND_BONDING) {
                        _pairingStatus.postValue("PAIRING");
                    } else if (bondState == BluetoothDevice.BOND_NONE) {
                        if (previousBondState == BluetoothDevice.BOND_BONDING) {
                            // Pairing gagal (misal: user menolak PIN)
                            _pairingStatus.postValue("NONE");
                            _errorMessage.postValue("Pairing gagal atau dibatalkan.");
                        } else {
                            // Perangkat ter-unpaired
                            _pairingStatus.postValue("NONE");
                            // Muat ulang daftar perangkat karena mungkin ada yang di-unpair
                            loadPairedDevices();
                        }
                    }
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    _bluetoothEnabledStatus.postValue(true);
                    startDiscovery(); // Mulai penemuan jika Bluetooth diaktifkan
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    _bluetoothEnabledStatus.postValue(false);
                    _discoveredDevices.postValue(new ArrayList<>()); // Kosongkan daftar jika Bluetooth mati
                }
            }
        }
    };

    public BluetoothRepository(Context context) {
        this.context = context.getApplicationContext(); // Gunakan application context untuk mencegah memory leak
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.executorService = Executors.newSingleThreadExecutor(); // Menggunakan satu thread untuk operasi Bluetooth
        this.mainHandler = new Handler(Looper.getMainLooper()); // Handler untuk post ke UI thread

        // Memuat perangkat yang sudah dipasangkan saat inisialisasi
        loadPairedDevices();
    }

    // Memeriksa apakah Bluetooth aktif
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    // Helper method untuk memeriksa izin BLUETOOTH_SCAN
    private boolean hasBluetoothScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Pada API < S, BLUETOOTH_ADMIN dan ACCESS_FINE_LOCATION diperlukan untuk discovery
            // Namun, karena ini ada di Repository, kita asumsikan Fragment sudah meminta ACCESS_FINE_LOCATION
            // Cukup periksa BLUETOOTH (jika diperlukan eksplisit)
            return true; // Biasanya, izin BLUETOOTH sudah diberikan oleh sistem pada instalasi jika di manifest
        }
    }

    // Helper method untuk memeriksa izin BLUETOOTH_CONNECT
    private boolean hasBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true; // Pada API < S, BLUETOOTH_CONNECT tidak ada, izin BLUETOOTH cukup
        }
    }

    // Memuat perangkat yang sudah dipasangkan
    private void loadPairedDevices() {
        if (bluetoothAdapter == null) {
            _errorMessage.postValue("Bluetooth tidak tersedia di perangkat ini.");
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            _errorMessage.postValue("Bluetooth tidak aktif.");
            _bluetoothEnabledStatus.postValue(false);
            return;
        }
        // Periksa izin BLUETOOTH_CONNECT sebelum memanggil getBondedDevices()
        if (!hasBluetoothConnectPermission()) {
            _errorMessage.postValue("Izin BLUETOOTH_CONNECT tidak diberikan. Tidak dapat memuat perangkat terpasang.");
            _discoveredDevices.postValue(new ArrayList<>()); // Pastikan daftar dikosongkan
            return;
        }

        Set<BluetoothDevice> pairedDevices;
        try {
            // Panggilan yang memerlukan izin, dibungkus dalam try-catch
            pairedDevices = bluetoothAdapter.getBondedDevices();
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while getting bonded devices: " + e.getMessage());
            _errorMessage.postValue("Izin Bluetooth ditolak saat memuat perangkat terpasang: " + e.getMessage());
            _discoveredDevices.postValue(new ArrayList<>()); // Kosongkan daftar jika terjadi error izin
            return; // Penting: keluar dari fungsi jika ada SecurityException
        }

        List<BluetoothDevice> currentList = new ArrayList<>();
        if (pairedDevices != null && pairedDevices.size() > 0) {
            currentList.addAll(pairedDevices);
        }
        _discoveredDevices.postValue(currentList); // Update LiveData dengan perangkat yang sudah dipasangkan
    }

    // Memulai proses penemuan perangkat Bluetooth
    public void startDiscovery() {
        if (bluetoothAdapter == null) {
            _errorMessage.postValue("Perangkat tidak mendukung Bluetooth.");
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            _errorMessage.postValue("Bluetooth tidak aktif. Mohon aktifkan.");
            _bluetoothEnabledStatus.postValue(false);
            return;
        }
        // Periksa izin BLUETOOTH_SCAN sebelum memanggil startDiscovery()
        if (!hasBluetoothScanPermission()) {
            _errorMessage.postValue("Izin BLUETOOTH_SCAN tidak diberikan. Tidak dapat memindai perangkat.");
            return;
        }

        // Hentikan penemuan yang sedang berjalan sebelum memulai yang baru
        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            _discoveredDevices.postValue(new ArrayList<>()); // Kosongkan daftar sebelum pemindaian baru
            Log.d(TAG, "Starting discovery...");
            bluetoothAdapter.startDiscovery();
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while starting discovery: " + e.getMessage());
            _errorMessage.postValue("Izin Bluetooth ditolak saat memulai pemindaian: " + e.getMessage());
            // Penting: Jangan lanjutkan proses jika izin ditolak
        }
    }

    // Menghentikan proses penemuan Bluetooth
    public void cancelDiscovery() {
        // Periksa izin BLUETOOTH_SCAN atau BLUETOOTH_CONNECT (karena cancelDiscovery kadang bisa memicu error jika izin connect tidak ada saat adapter sibuk)
        if (!hasBluetoothScanPermission() && !hasBluetoothConnectPermission()) {
            Log.w(TAG, "Permissions not granted to cancel discovery. Skipping.");
            return;
        }
        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "Discovery cancelled.");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while cancelling discovery: " + e.getMessage());
            _errorMessage.postValue("Izin Bluetooth ditolak saat membatalkan pemindaian: " + e.getMessage());
        }
    }

    // Mendaftarkan BroadcastReceiver untuk event Bluetooth
    public void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND); // Perangkat ditemukan (membutuhkan BLUETOOTH_SCAN)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); // Penemuan selesai
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); // Status pairing berubah (membutuhkan BLUETOOTH_CONNECT untuk mendapatkan device)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); // Status Bluetooth adapter berubah
        context.registerReceiver(bluetoothReceiver, filter);
        Log.d(TAG, "Bluetooth receiver registered.");
    }

    // Membatalkan pendaftaran BroadcastReceiver
    public void unregisterBluetoothReceiver() {
        try {
            context.unregisterReceiver(bluetoothReceiver);
            Log.d(TAG, "Bluetooth receiver unregistered.");
        } catch (IllegalArgumentException e) {
            // Receiver mungkin sudah tidak terdaftar, tangani exception jika terjadi
            Log.e(TAG, "Receiver already unregistered or not registered: " + e.getMessage());
        }
    }

    // Metode untuk memicu pairing perangkat
    public void pairDevice(BluetoothDevice device) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            _errorMessage.postValue("Bluetooth tidak aktif atau tidak tersedia.");
            return;
        }
        // Periksa izin BLUETOOTH_CONNECT sebelum memanggil createBond()
        if (!hasBluetoothConnectPermission()) {
            _errorMessage.postValue("Izin BLUETOOTH_CONNECT tidak diberikan. Tidak dapat melakukan pairing.");
            return;
        }

        executorService.execute(() -> {
            try {
                // Hentikan penemuan jika sedang berjalan sebelum pairing
                cancelDiscovery();
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "Attempting to pair with: " + device.getName() + " (" + device.getAddress() + ")");
                    boolean success = device.createBond(); // Metode standar untuk pairing
                    if (!success) {
                        mainHandler.post(() -> _errorMessage.postValue("Gagal memulai proses pairing."));
                    }
                } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    mainHandler.post(() -> {
                        _pairingStatus.postValue("BONDED");
                        _errorMessage.postValue("Perangkat sudah ter-pair.");
                        // Langsung coba koneksi printer jika sudah ter-pair
                        getPrinterInformation(device);
                    });
                }
            } catch (SecurityException e) { // Tangani SecurityException secara eksplisit
                Log.e(TAG, "SecurityException during pairing: " + e.getMessage());
                mainHandler.post(() -> _errorMessage.postValue("Izin Bluetooth ditolak: " + e.getMessage()));
            } catch (Exception e) {
                Log.e(TAG, "Error during pairing: " + e.getMessage());
                mainHandler.post(() -> _errorMessage.postValue("Error saat pairing: " + e.getMessage()));
            }
        });
    }

    // Metode ini sekarang dipanggil di background thread oleh pairDevice
    // dan hanya jika perangkat sudah BOND_BONDED.
    public void getPrinterInformation(BluetoothDevice device) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            _errorMessage.postValue("Bluetooth tidak aktif atau tidak tersedia.");
            return;
        }
        if (!hasBluetoothConnectPermission()) {
            _errorMessage.postValue("Izin BLUETOOTH_CONNECT tidak diberikan. Tidak dapat mendapatkan informasi printer.");
            return;
        }

        int bondState;
        try {
            bondState = device.getBondState();
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while getting bond state: " + e.getMessage());
            _errorMessage.postValue("Izin Bluetooth ditolak saat memeriksa status pairing: " + e.getMessage());
            return; // Penting: keluar jika tidak bisa mendapatkan status pairing
        }

        if (bondState == BluetoothDevice.BOND_BONDED) {
            executorService.execute(() -> {
                try {
                    // Contoh: Dapatkan lebar dan DPI printer (simulasi karena perintah ESC/P mungkin tidak universal)
                    String width = getPrinterWidthMM(device);
                    int dpi = getPrinterDpi(device);

                    // Setelah mendapatkan informasi, Anda bisa posting hasilnya ke LiveData lain
                    // atau ke log, atau meneruskan ke ViewModel untuk ditampilkan di UI.
                    Log.d(TAG, "Printer Info - Width: " + width + "mm, DPI: " + dpi);
                    if (width.isEmpty() && dpi == 0) {
                        mainHandler.post(() -> _errorMessage.postValue("Gagal mendapatkan info printer. Pastikan ini printer Bluetooth."));
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException getting printer info: " + e.getMessage());
                    mainHandler.post(() -> _errorMessage.postValue("Izin Bluetooth ditolak saat mengakses printer: " + e.getMessage()));
                } catch (Exception e) {
                    Log.e(TAG, "Error getting printer info: " + e.getMessage());
                    mainHandler.post(() -> _errorMessage.postValue("Error mendapatkan info printer: " + e.getMessage()));
                }
            });
        } else {
            // Jika perangkat belum ter-pair, beri pesan error
            _errorMessage.postValue("Perangkat belum ter-pair. Tidak dapat mendapatkan informasi printer.");
        }
    }

    // Metode untuk mendapatkan lebar printer (di-refactor ke Repository)
    // Berjalan di background thread
    private String getPrinterWidthMM(BluetoothDevice device) {
        String printerWidthMM = "";
        BluetoothSocket socket = null;
        // Periksa izin BLUETOOTH_CONNECT sebelum membuat/menghubungkan soket
        if (!hasBluetoothConnectPermission()) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for getPrinterWidthMM.");
            return printerWidthMM;
        }
        try {
            socket = device.createRfcommSocketToServiceRecord(PRINTER_SERVICE_UUID);
            socket.connect(); // Koneksi di background thread

            OutputStream outputStream = socket.getOutputStream();
            // Perintah ESC/P untuk lebar mungkin tidak universal atau memerlukan protokol spesifik
            // Ini hanyalah contoh. Perlu dokumentasi printer yang akurat.
            outputStream.write(new byte[]{27, 87});
            outputStream.flush();

            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[64];
            int bytesRead = inputStream.read(buffer);

            if (bytesRead > 0) {
                printerWidthMM = new String(buffer, 0, bytesRead).trim();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException connecting to printer for width: " + e.getMessage());
            mainHandler.post(() -> _errorMessage.postValue("Izin Bluetooth ditolak saat koneksi printer (lebar): " + e.getMessage()));
        } catch (IOException e) {
            Log.e(TAG, "Error getting printer width: " + e.getMessage());
            mainHandler.post(() -> _errorMessage.postValue("Error mendapatkan lebar printer: " + e.getMessage()));
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing socket: " + e.getMessage());
                }
            }
        }
        return printerWidthMM;
    }

    // Metode untuk mendapatkan DPI printer (di-refactor ke Repository)
    // Berjalan di background thread
    private int getPrinterDpi(BluetoothDevice device) {
        int printerDpi = 0;
        BluetoothSocket socket = null;
        // Periksa izin BLUETOOTH_CONNECT sebelum membuat/menghubungkan soket
        if (!hasBluetoothConnectPermission()) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for getPrinterDpi.");
            return printerDpi;
        }
        try {
            socket = device.createRfcommSocketToServiceRecord(PRINTER_SERVICE_UUID);
            socket.connect(); // Koneksi di background thread

            OutputStream outputStream = socket.getOutputStream();
            // Perintah ESC/P untuk DPI mungkin tidak universal
            outputStream.write(new byte[]{27, 99});
            outputStream.flush();

            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[64];
            int bytesRead = inputStream.read(buffer);

            if (bytesRead > 0) {
                try {
                    printerDpi = Integer.parseInt(new String(buffer, 0, bytesRead).trim());
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Failed to parse DPI: " + e.getMessage());
                    mainHandler.post(() -> _errorMessage.postValue("Gagal memparsing DPI printer."));
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException connecting to printer for DPI: " + e.getMessage());
            mainHandler.post(() -> _errorMessage.postValue("Izin Bluetooth ditolak saat koneksi printer (DPI): " + e.getMessage()));
        } catch (IOException e) {
            Log.e(TAG, "Error getting printer DPI: " + e.getMessage());
            mainHandler.post(() -> _errorMessage.postValue("Error mendapatkan DPI printer: " + e.getMessage()));
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing socket: " + e.getMessage());
                }
            }
        }
        return printerDpi;
    }
}
