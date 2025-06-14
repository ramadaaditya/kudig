package com.kudig.kwitansidigital.ui.preview;


import static com.kudig.kwitansidigital.utils.ConstantsKt.LOCATION_PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.kudig.kwitansidigital.R;
import com.kudig.kwitansidigital.databinding.FragmentPreviewBinding;
import com.kudig.kwitansidigital.db.KwitansiEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PreviewFragment extends Fragment {


    private FragmentPreviewBinding binding;
    private PreviewViewModel viewModel;
    private NavController navController;

    // BARU: Launcher modern untuk meminta izin Bluetooth Connect
    private final ActivityResultLauncher<String> requestBluetoothPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Jika pengguna memberikan izin, coba lagi untuk mencari printer
                    viewModel.findBluetoothPrinters();
                } else {
                    // Pengguna menolak izin
                    Toast.makeText(requireContext(), "Izin Bluetooth diperlukan untuk mencetak.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPreviewBinding.inflate(inflater, container, false);
        setHasOptionsMenu(true);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        viewModel = new ViewModelProvider(this).get(PreviewViewModel.class);

        checkLocationPermission();
        if (getArguments() != null) {
            KwitansiEntity kwitansiEntity = (KwitansiEntity) getArguments().getSerializable("KwitansiEntity");
            if (kwitansiEntity != null) {
                viewModel.loadKwitansiData(kwitansiEntity);
            }
        }
        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.kwitansiData.observe(getViewLifecycleOwner(), kwitansi -> {
            if (kwitansi != null) {
                binding.textNomor.setText(kwitansi.getNomor());
                binding.textNamaPengirim.setText(kwitansi.getNamaPengirim());
                binding.textNamaPenerima.setText(kwitansi.getNamaPenerima());
                binding.textDeskripsi.setText(kwitansi.getDeskripsi());
            }
        });

        viewModel.formattedNominal.observe(getViewLifecycleOwner(), nominal -> binding.textNominal.setText(nominal));
        viewModel.nominalTerbilang.observe(getViewLifecycleOwner(), terbilang -> binding.textTerbilang.setText(terbilang));
        viewModel.currentDate.observe(getViewLifecycleOwner(), date -> binding.tanggalSekarang.setText(date));
        viewModel.currentLocation.observe(getViewLifecycleOwner(), location -> binding.tempatSekarang.setText(location));


        // Observer BARU untuk daftar printer
        viewModel.printerList.observe(getViewLifecycleOwner(), printers -> {
            if (printers == null || getView() == null) return;
            try {
                if (printers.size() == 1) {
                    String printerName = printers.get(0).getDevice().getName();

                    Toast.makeText(getContext(), "Mencetak ke: " + printerName, Toast.LENGTH_SHORT).show();
                    viewModel.doPrint(getViewBitmap(binding.areaLayoutPrint), printers.get(0));
                } else {
                    showPrinterSelectionDialog(printers);
                }
            } catch (SecurityException e) {
                // Fallback jika terjadi hal yang tidak terduga
                Log.e("PreviewFragment", "SecurityException tidak terduga!", e);
                Toast.makeText(getContext(), "Error Izin Bluetooth. Pastikan izin telah diberikan.", Toast.LENGTH_LONG).show();
            }
        });

    }

    private void startPrintProcess() {
        // Hanya berlaku untuk Android 12 (API 31) ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                // Izin sudah ada, langsung cari printer
                viewModel.findBluetoothPrinters();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)) {
                // Opsional: Tampilkan dialog penjelasan mengapa Anda butuh izin ini
                new AlertDialog.Builder(requireContext())
                        .setTitle("Izin Diperlukan")
                        .setMessage("Aplikasi ini memerlukan izin Bluetooth untuk dapat menemukan dan mencetak ke printer.")
                        .setPositiveButton("Mengerti", (dialog, which) -> {
                            requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                        })
                        .show();
            } else {
                // Izin belum ada, minta izin
                requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else {
            // Untuk versi Android di bawah 12, izin BLUETOOTH sudah cukup (biasanya ada di Manifest)
            viewModel.findBluetoothPrinters();
        }
    }

    // Metode helper untuk menampilkan dialog
    private void showPrinterSelectionDialog(List<BluetoothConnection> printers) {
        String[] printerNames = new String[printers.size()];
        try {
            for (int i = 0; i < printers.size(); i++) {
                printerNames[i] = printers.get(i).getDevice().getName();
            }
        } catch (SecurityException e) {
            Log.e("PreviewFragment", "SecurityException saat membuat daftar nama printer", e);
            Toast.makeText(getContext(), "Gagal mendapatkan nama printer karena masalah izin.", Toast.LENGTH_LONG).show();
            return; // Hentikan jika terjadi error
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Pilih Printer")
                .setItems(printerNames, (dialog, which) -> {
                    // Pengguna memilih printer, panggil doPrint dengan printer yang dipilih
                    BluetoothConnection selectedPrinter = printers.get(which);
                    viewModel.doPrint(getViewBitmap(binding.areaLayoutPrint), selectedPrinter);
                })
                .show();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.fetchCurrentLocation(); // Refresh location after permission granted
            } else {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Informasi")
                        .setMessage("Tolong Aktifkan Izin Lokasi Pada Aplikasi Ini untuk menampilkan lokasi pada kwitansi.")
                        .setPositiveButton("Mengerti", null)
                        .show();
            }
        }
    }

    private Bitmap getViewBitmap(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private void createPdf() {
        View view = getActivity().findViewById(R.id.area_layout_print);

        int width = view.getWidth();
        int height = view.getHeight();

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        view.draw(canvas);

        document.finishPage(page);

        Calendar instance = Calendar.getInstance();
        String format = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(instance.getTime());
        String format2 = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(instance.getTime());
        String[] split = format.split("/");
        String[] split2 = format2.split(":");
        String fileName = (split[0] + split[1] + split[2]) + (split2[0] + split2[1] + split2[2]) + "_Kwitansi.pdf";

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File backupDir = new File(downloadsDir, "Kwitansi Digital");

        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                Toast.makeText(getContext(), "Failed to create backup directory", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        File file = new File(backupDir, fileName);

        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(getContext(), "PDF saved: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
        }

        // Menutup dokumen
        document.close();

        // Menampilkan progress dialog
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Saving..." + file.getAbsolutePath());
        progressDialog.show();

        // Menutup progress dialog setelah 500 milidetik
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressDialog.cancel();
            }
        }, 500);
    }

    private void shareImage() {
        try {
            View view = getActivity().findViewById(R.id.area_layout_print);
            view.setDrawingCacheEnabled(true);
            view.measure(View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(view.getHeight(), View.MeasureSpec.EXACTLY));
            view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

            view.buildDrawingCache(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);

            String fileName = "temp_image.png";
            File directory = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (directory != null && !directory.exists()) {
                directory.mkdirs();
            }
            File file = new File(directory, fileName);

            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            Uri imageUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Image"));
        } catch (Exception e) {
            Log.e("APP", "Tidak dapat berbagi gambar", e);
            Toast.makeText(getContext(), "Berbagi gagal, silakan coba lagi", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_item_print) {
            startPrintProcess();
            return true;
        } else if (itemId == R.id.menu_item_save) {
            createPdf(); // Panggil method yang ada di fragment
            return true;
        } else if (itemId == R.id.menu_item_share) {
            shareImage(); // Panggil method yang ada di fragment
            return true;
        } else if (itemId == R.id.menu_item_info_location) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Informasi")
                    .setMessage("Jika Lokasi pada kwitansi kosong, mohon aktifkan izin lokasi pada aplikasi.")
                    .setPositiveButton("Mengerti", null)
                    .show();
            return true;
        }
        // Logic untuk edit bisa ditambahkan di sini
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_preview, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
}