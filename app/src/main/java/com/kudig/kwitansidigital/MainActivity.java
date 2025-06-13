package com.kudig.kwitansidigital;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

//import com.google.android.gms.ads.MobileAds;
//import com.google.android.gms.ads.initialization.InitializationStatus;
//import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kudig.kwitansidigital.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Button Home,History;
    private FloatingActionButton Add;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 2;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String FIRST_INSTALL_KEY = "first_install";
    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 1;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 1;
    public static final int PERMISSION_BLUETOOTH_SCAN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Home = findViewById(R.id.btn_home);
        Add = findViewById(R.id.add_btn);
        History = findViewById(R.id.btn_history);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home,R.id.navigation_add,R.id.navigation_device_bluetooth, R.id.navigation_preview, R.id.navigation_about, R.id.navigation_history, R.id.navigation_privacy_policy)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Check if the app is installed for the first time
        boolean firstInstall = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(FIRST_INSTALL_KEY, true);

        if (firstInstall) {
            showFirstInstallAlertDialog();
            // Update the status of first install
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(FIRST_INSTALL_KEY, false).apply();
        }

        Home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.navigation_home);
            }
        });
        Add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.navigation_add);
            }
        });
        History.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.navigation_history);
            }
        });


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Izin lokasi perlu dijelaskan kepada pengguna.

                Toast.makeText(this, "Tolong Aktifkan Izin Lokasi", Toast.LENGTH_SHORT).show();

                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                // Izin lokasi belum diberikan, tampilkan dialog permintaan izin.
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Izin penyimpanan perlu dijelaskan kepada pengguna.
                Toast.makeText(this, "Tolong Aktifkan Izin Penyimpanan", Toast.LENGTH_SHORT).show();
            } else {
                // Izin penyimpanan belum diberikan, tampilkan dialog permintaan izin.
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, STORAGE_PERMISSION_REQUEST_CODE);
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