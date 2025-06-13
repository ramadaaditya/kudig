package com.kudig.kwitansidigital.ui.device;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.kudig.kwitansidigital.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DeviceBluetoothFragment extends Fragment {

    private BluetoothAdapter bluetoothAdapter;
    private List<BluetoothDevice> deviceList;
    private ArrayAdapter<BluetoothDevice> adapter;

    private static final String ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
    private static final String ACTION_BOND_STATE_CHANGED = "android.bluetooth.device.action.BOND_STATE_CHANGED";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_bluetooth, container, false);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList<>();

        ListView listView = view.findViewById(R.id.device_list);
        adapter = new ArrayAdapter<BluetoothDevice>(requireContext(), android.R.layout.simple_list_item_1, deviceList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                BluetoothDevice device = deviceList.get(position);
                String deviceName = device.getName();
                textView.setText(deviceName != null ? deviceName : device.getAddress());
                return view;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice selectedDevice = deviceList.get(position);
                // Pair perangkat yang dipilih
                pairDevice(selectedDevice);
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Lakukan pemindaian perangkat Bluetooth saat fragment ditampilkan
        scanDevices();
    }

    private void scanDevices() {
        deviceList.clear();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Hentikan pemindaian perangkat Bluetooth saat fragment tidak aktif
        bluetoothAdapter.cancelDiscovery();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Hentikan pemindaian perangkat Bluetooth saat fragment dihancurkan
        bluetoothAdapter.cancelDiscovery();
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !deviceList.contains(device)) {
                    deviceList.add(device);
                    adapter.notifyDataSetChanged(); // Menyampaikan perubahan ke adapter untuk memperbarui tampilan ListView
                }
            } else if (ACTION_PAIRING_REQUEST.equals(action)) {
                // Proses pairing dimulai
                // ...
            } else if (ACTION_BOND_STATE_CHANGED.equals(action)) {
                // Proses pairing selesai atau gagal
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (bondState == BluetoothDevice.BOND_BONDED) {
                    // Pairing berhasil, kembali ke FragmentPreview
                    FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                    fragmentManager.popBackStack();
                    Toast.makeText(context, "Perangkat Berhasil Tersambung", Toast.LENGTH_SHORT).show();
                } else if (bondState == BluetoothDevice.BOND_NONE) {
                    // Pairing gagal, tambahkan penanganan sesuai kebutuhan
                }
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        // Daftarkan penerima siaran untuk menerima hasil pemindaian perangkat Bluetooth
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(ACTION_PAIRING_REQUEST);
        filter.addAction(ACTION_BOND_STATE_CHANGED);
        requireContext().registerReceiver(bluetoothReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Hapus penerima siaran saat fragment tidak aktif
        requireContext().unregisterReceiver(bluetoothReceiver);
    }

    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
            // Setelah perangkat terhubung, panggil metode untuk mendapatkan informasi printer
            getPrinterInformation(device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getPrinterInformation(BluetoothDevice device) {
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
        }
    }

    private String getPrinterWidthMM(BluetoothDevice device) {
        String printerWidthMM = "";

        // Mendapatkan UUID dari servis printer Bluetooth
        UUID printerServiceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try {
            // Membuat socket Bluetooth
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(printerServiceUUID);

            // Menghubungkan socket Bluetooth
            socket.connect();

            // Mengirim perintah ke printer untuk mendapatkan lebar
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(new byte[]{27, 87});
            outputStream.flush();

            // Menerima respons dari printer
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[64];
            int bytesRead = inputStream.read(buffer);

            // Mengambil lebar dari respons
            if (bytesRead > 0) {
                printerWidthMM = new String(buffer, 0, bytesRead).trim();
            }

            // Menutup koneksi socket Bluetooth
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return printerWidthMM;
    }

    private int getPrinterDpi(BluetoothDevice device) {
        int printerDpi = 0;

        // Mendapatkan UUID dari servis printer Bluetooth
        UUID printerServiceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try {
            // Membuat socket Bluetooth
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(printerServiceUUID);

            // Menghubungkan socket Bluetooth
            socket.connect();

            // Mengirim perintah ke printer untuk mendapatkan DPI
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(new byte[]{27, 99});
            outputStream.flush();

            // Menerima respons dari printer
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[64];
            int bytesRead = inputStream.read(buffer);

            // Mengambil DPI dari respons
            if (bytesRead > 0) {
                printerDpi = Integer.parseInt(new String(buffer, 0, bytesRead).trim());
            }

            // Menutup koneksi socket Bluetooth
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return printerDpi;
    }



}