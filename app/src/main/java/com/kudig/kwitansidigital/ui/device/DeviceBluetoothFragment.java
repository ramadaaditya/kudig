package com.kudig.kwitansidigital.ui.device;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.kudig.kwitansidigital.R;
import com.kudig.kwitansidigital.databinding.FragmentDeviceBluetoothBinding;

import java.util.ArrayList;

public class DeviceBluetoothFragment extends Fragment {
    private DeviceBluetoothViewModel viewModel;
    private ArrayList<BluetoothDevice> deviceList;
    private ArrayAdapter<BluetoothDevice> adapter;
    private ActivityResultLauncher<String[]> requestBluetoothPermissionsLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestBluetoothPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean granted;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        granted = Boolean.TRUE.equals(permissions.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false)) &&
                                Boolean.TRUE.equals(permissions.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false));
                    } else {
                        granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                    }

                    if (granted) {
                        viewModel.startBluetoothScan();
                    } else {
                        Toast.makeText(requireContext(), "Izin Bluetooth diperlukan untuk fungsionalitas ini. ", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentDeviceBluetoothBinding binding;
        binding = FragmentDeviceBluetoothBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Inisialisasi ViewModel
        viewModel = new ViewModelProvider(this).get(DeviceBluetoothViewModel.class);

        deviceList = new ArrayList<>(); // Inisialisasi daftar perangkat

        // Mengatur ArrayAdapter untuk ListView
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, deviceList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                BluetoothDevice device = deviceList.get(position);
                String deviceDisplay; // Default value jika terjadi SecurityException
                try {
                    // Akses nama perangkat, memerlukan izin BLUETOOTH_CONNECT
                    String deviceName = device.getName();
                    deviceDisplay = (deviceName != null && !deviceName.isEmpty()) ? deviceName : device.getAddress();
                } catch (SecurityException e) {
                    // Tangani SecurityException jika izin BLUETOOTH_CONNECT tidak diberikan
                    // Tampilkan alamat perangkat sebagai fallback atau pesan error
                    deviceDisplay = device.getAddress() != null ? device.getAddress() + " (Izin Ditolak)" : "Perangkat Tidak Dikenal";
                    Toast.makeText(requireContext(), "Tidak dapat menampilkan nama perangkat: Izin Bluetooth ditolak.", Toast.LENGTH_SHORT).show();
                }
                textView.setText(deviceDisplay);
                return view;
            }
        };
        binding.deviceList.setAdapter(adapter); // Menggunakan binding untuk mengakses ListView

        // Mengatur OnItemClickListener untuk ListView
        binding.deviceList.setOnItemClickListener((parent, view1, position, id) -> {
            BluetoothDevice selectedDevice = deviceList.get(position);
            // Memicu permintaan pairing melalui ViewModel
            viewModel.pairDevice(selectedDevice);
        });

        // --- Observasi LiveData dari ViewModel ---

        // Mengamati daftar perangkat yang ditemukan dari ViewModel
        viewModel.getDiscoveredDevices().observe(getViewLifecycleOwner(), devices -> {
            deviceList.clear();
            deviceList.addAll(devices);
            adapter.notifyDataSetChanged(); // Memperbarui tampilan daftar
        });

        // Mengamati status Bluetooth (aktif/tidak) dari ViewModel
        viewModel.getBluetoothEnabledStatus().observe(getViewLifecycleOwner(), isEnabled -> {
            if (!isEnabled) {
                // Jika Bluetooth tidak aktif, tampilkan pesan dan minta untuk mengaktifkannya
                Toast.makeText(requireContext(), "Bluetooth tidak aktif. Mohon aktifkan.", Toast.LENGTH_LONG).show();
                // Opsional: Buka pengaturan Bluetooth secara otomatis
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
            }
        });

        // Mengamati status pairing dari ViewModel
        viewModel.getPairingStatus().observe(getViewLifecycleOwner(), status -> {
            if (status.equals("BONDED")) {
                Toast.makeText(requireContext(), "Perangkat Berhasil Tersambung", Toast.LENGTH_SHORT).show();
                // Navigasi kembali setelah berhasil pairing
                if (requireActivity() != null) {
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main); // Sesuaikan ID nav_host_fragment Anda
                    navController.popBackStack();
                }
            } else if (status.equals("NONE")) {
                Toast.makeText(requireContext(), "Pairing Gagal", Toast.LENGTH_SHORT).show();
            } else if (status.equals("PAIRING")) {
                Toast.makeText(requireContext(), "Sedang mencoba pairing...", Toast.LENGTH_SHORT).show();
            }
        });

        // Mengamati pesan error dari ViewModel
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        checkAndRequestBluetoothPermissions();
        viewModel.startBluetoothManagement();
    }

    @Override
    public void onStop() {
        super.onStop();
        viewModel.stopBluetoothManagement();
    }


    // Metode untuk memeriksa dan meminta izin Bluetooth
    private void checkAndRequestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Untuk Android 12 (API 31) dan yang lebih baru
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermissionsLauncher.launch(new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                });
            } else {
                // Izin sudah ada, mulai pemindaian
                viewModel.startBluetoothScan();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Untuk Android 6 (API 23) hingga 11 (API 30)
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermissionsLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
            } else {
                // Izin sudah ada, mulai pemindaian
                viewModel.startBluetoothScan();
            }
        } else {
            // Untuk Android di bawah Marshmallow (API 23), izin dianggap sudah diberikan di manifest.
            // Langsung mulai pemindaian.
            viewModel.startBluetoothScan();
        }
    }
}