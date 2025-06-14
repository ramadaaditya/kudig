package com.kudig.kwitansidigital.utils;

import android.Manifest;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnections;

import java.util.ArrayList;
import java.util.List;

public class BluetoothPrintersConnections extends BluetoothConnections {


    /**
     * Mendapatkan daftar printer yang sudah di-pair sebagai List.
     * Metode ini membutuhkan izin BLUETOOTH_CONNECT pada Android 12+.
     *
     * @return List dari BluetoothConnection yang merupakan printer, atau list kosong jika tidak ada.
     * @throws SecurityException jika izin BLUETOOTH_CONNECT tidak diberikan.
     */
    @NonNull
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public List<BluetoothConnection> getPrinterList() throws SecurityException { // Renamed method
        BluetoothConnection[] bluetoothDevicesList = super.getList(); // Calls the original array-returning method

        if (bluetoothDevicesList == null) {
            return new ArrayList<>();
        }

        ArrayList<BluetoothConnection> printers = new ArrayList<>();
        for (BluetoothConnection bluetoothConnection : bluetoothDevicesList) {
            if (bluetoothConnection == null || bluetoothConnection.getDevice() == null) { // Added null check for safety
                continue;
            }
            BluetoothDevice device = bluetoothConnection.getDevice();
            // It's good practice to check if getBluetoothClass() is null before calling methods on it
            if (device.getBluetoothClass() == null) {
                continue;
            }

            int majorDeviceClass = device.getBluetoothClass().getMajorDeviceClass();
            int deviceClass = device.getBluetoothClass().getDeviceClass();

            if (majorDeviceClass == BluetoothClass.Device.Major.IMAGING &&
                    (deviceClass == 0x0680 || deviceClass == BluetoothClass.Device.Major.IMAGING)) {
                // Consider if you need a new BluetoothConnection or if bluetoothConnection is already suitable
                // If the library ensures bluetoothConnection is valid and for the correct device, you might not need `new BluetoothConnection(device)`
                // However, if your filtering logic implies you need a fresh instance, keep it.
                // For now, assuming your original logic was intentional:
                printers.add(new BluetoothConnection(device));
            }
        }
        return printers;
    }

    // ... (rest of your class)
    @Deprecated
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public static BluetoothConnection selectFirstPaired() {
        return null;
    }
}