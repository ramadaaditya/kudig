package com.kudig.kwitansidigital;

import static com.kudig.kwitansidigital.utils.ConstantsKt.FIRST_INSTALL_KEY;
import static com.kudig.kwitansidigital.utils.ConstantsKt.LOCATION_PERMISSION_REQUEST_CODE;
import static com.kudig.kwitansidigital.utils.ConstantsKt.PREFS_NAME;
import static com.kudig.kwitansidigital.utils.ConstantsKt.STORAGE_PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.kudig.kwitansidigital.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding;
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_add, R.id.navigation_device_bluetooth, R.id.navigation_preview, R.id.navigation_about, R.id.navigation_history, R.id.navigation_privacy_policy)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        boolean firstInstall = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(FIRST_INSTALL_KEY, true);

        if (firstInstall) {
            showFirstInstallAlertDialog();
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(FIRST_INSTALL_KEY, false).apply();
        }

        binding.btnHome.setOnClickListener(v -> navController.navigate(R.id.navigation_home));
        binding.addBtn.setOnClickListener(v -> navController.navigate(R.id.navigation_add));
        binding.btnHistory.setOnClickListener(v -> navController.navigate(R.id.navigation_history));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Izin lokasi perlu dijelaskan kepada pengguna.

                Toast.makeText(this, "Tolong Aktifkan Izin Lokasi", Toast.LENGTH_SHORT).show();

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                // Izin lokasi belum diberikan, tampilkan dialog permintaan izin.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Izin penyimpanan perlu dijelaskan kepada pengguna.
                Toast.makeText(this, "Tolong Aktifkan Izin Penyimpanan", Toast.LENGTH_SHORT).show();
            } else {
                // Izin penyimpanan belum diberikan, tampilkan dialog permintaan izin.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void showFirstInstallAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setTitle("Selamat Datang")
                .setMessage("Terima kasih telah menginstal aplikasi ini. Jangan lupa untuk memberikan izin lokasi dan penyimpanan untuk fitur-fitur yang optimal. Izin lokasi berguna untuk memberikan lokasi pada kwitansi")
                .setPositiveButton("Mengerti", null)
                .show();
    }
}