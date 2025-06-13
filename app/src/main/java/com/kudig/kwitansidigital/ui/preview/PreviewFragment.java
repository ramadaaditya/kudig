package com.kudig.kwitansidigital.ui.preview;


import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.pdf.PdfDocument;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.room.Room;

import java.io.IOException;

import com.dantsu.escposprinter.EscPosPrinterCommands;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.kudig.kwitansidigital.utils.BluetoothPrintersConnections;
import com.kudig.kwitansidigital.db.HistoryDAO;
import com.kudig.kwitansidigital.db.HistoryEntity;
import com.kudig.kwitansidigital.db.KwitansiDAO;
import com.kudig.kwitansidigital.db.KwitansiDB;
import com.kudig.kwitansidigital.db.KwitansiEntity;
import com.kudig.kwitansidigital.R;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class PreviewFragment extends Fragment {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private TextView textNomor, textNamaPengirim, textNamaPenerima, textNominal, textDeskripsi, textTerbilang, texttanggal, texttempat;
    private PreviewViewModel viewModel;
    private Button print;
    private Context context;
    private KwitansiDB kwitansiDB;
    private KwitansiDAO kwitansiDAO;
    private KwitansiEntity kwitansiEntity;

    Bitmap bitmap;
    LinearLayout linearLayout;
    private static final int STORAGE_PERMISSION_CODE = 100;

    public PreviewFragment() {
    }

    public static final int PERMISSION_BLUETOOTH = 1;

    private final Locale locale = new Locale("id", "ID");
    private final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a", locale);
    private final NumberFormat nf = NumberFormat.getCurrencyInstance(locale);


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preview, container, false);

        checkLocationPermission();
        kwitansiDB = KwitansiDB.getInstance(requireContext());
        kwitansiDAO = kwitansiDB.getKwitansiDAO();

        setHasOptionsMenu(true);

        textNomor = view.findViewById(R.id.text_nomor);
        textNamaPengirim = view.findViewById(R.id.text_nama_pengirim);
        textNamaPenerima = view.findViewById(R.id.text_nama_penerima);
        textNominal = view.findViewById(R.id.text_nominal);
        textDeskripsi = view.findViewById(R.id.text_deskripsi);
        textTerbilang = view.findViewById(R.id.text_terbilang);
        texttanggal = view.findViewById(R.id.tanggal_sekarang);
        texttempat = view.findViewById(R.id.tempat_sekarang);

        viewModel = new ViewModelProvider(this).get(PreviewViewModel.class);



        Bundle bundle = getArguments();
        if (bundle != null) {
            kwitansiEntity = (KwitansiEntity) bundle.getSerializable("KwitansiEntity");
            String nomor = bundle.getString("DataNomor");
            String namaPengirim = bundle.getString("DataNamaPengirim");
            String namaPenerima = bundle.getString("DataNamaPenerima");
            String nominal = bundle.getString("DataNominal");
            String deskripsi = bundle.getString("DataDeskripsi");

            double nominalValue = Double.parseDouble(nominal);

            Locale indonesianLocale = new Locale("id", "ID");
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(indonesianLocale);
            String formattedNominal = currencyFormatter.format(nominalValue);

            // Menghapus digit desimal (",00") di belakang nominal
            if (formattedNominal.endsWith(",00")) {
                formattedNominal = formattedNominal.substring(0, formattedNominal.length() - 3);
            }

            formattedNominal = formattedNominal.replace("Rp", "");
            String nominalTerbilang = convertToTerbilang(nominalValue) + " RUPIAH";

            textNomor.setText(nomor);
            textNamaPengirim.setText(namaPengirim);
            textNamaPenerima.setText(namaPenerima);
            textNominal.setText(formattedNominal);
            textDeskripsi.setText(deskripsi);
            textTerbilang.setText(nominalTerbilang);

            // Mendapatkan tanggal saat ini
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            String tanggalSekarang = dateFormat.format(calendar.getTime());

            texttanggal.setText(tanggalSekarang);

            LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null) {
                        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (!addresses.isEmpty()) {
                                String kecamatan = addresses.get(0).getSubAdminArea();
                                kecamatan = kecamatan.replace("Kabupaten ", ""); // Menghapus "Kabupaten " dari string
                                texttempat.setText(kecamatan);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
        return view;
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Location permission is not granted, request permission.
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, you can perform location-related actions.
            } else {
                // Tampilkan AlertDialog
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Informasi");
                builder.setMessage("Tolong Aktifkan Izin Lokasi Pada Aplikasi Ini");
                builder.setPositiveButton("Mengerti", null);
                builder.show();
            }
        }
    }

    private String convertToTerbilang(double nominal) {
        String[] angka = {"", "SATU", "DUA", "TIGA", "EMPAT", "LIMA", "ENAM", "TUJUH", "DELAPAN", "SEMBILAN", "SEPULUH", "SEBELAS"};

        if (nominal < 12) {
            return angka[(int) nominal];
        } else if (nominal < 20) {
            return angka[(int) nominal % 10] + " BELAS";
        } else if (nominal < 100) {
            return angka[(int) nominal / 10] + " PULUH " + angka[(int) nominal % 10];
        } else if (nominal < 200) {
            return "SERATUS " + convertToTerbilang(nominal % 100);
        } else if (nominal < 1000) {
            return angka[(int) nominal / 100] + " RATUS " + convertToTerbilang(nominal % 100);
        } else if (nominal < 2000) {
            return "SERIBU " + convertToTerbilang(nominal % 1000);
        } else if (nominal < 1000000) {
            return convertToTerbilang(nominal / 1000) + " RIBU " + convertToTerbilang(nominal % 1000);
        } else if (nominal < 1000000000) {
            return convertToTerbilang(nominal / 1000000) + " JUTA " + convertToTerbilang(nominal % 1000000);
        } else if (nominal < 1000000000000L) {
            return convertToTerbilang(nominal / 1000000000) + " MILIAR " + convertToTerbilang(nominal % 1000000000);
        }

        return "";
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

    private Bitmap loadBitmapFromView(LinearLayout linearLayout, int width, int height) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        linearLayout.draw(canvas);
        return bitmap;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_preview, menu);
        MenuItem printMenuItem = menu.findItem(R.id.menu_item_print);
        printMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

                if (bondedDevices != null && !bondedDevices.isEmpty()) {
                    // Ada perangkat yang terhubung, langsung cetak
                    doPrint();


                } else {
                    // Tidak ada perangkat yang terhubung, arahkan ke fragment DeviceBluetooth
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigate(R.id.navigation_device_bluetooth);
                }
                return true;
            }
        });

        MenuItem locationMenuItem = menu.findItem(R.id.menu_item_info_location);
        locationMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Tampilkan AlertDialog
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Informasi");
                builder.setMessage("Jika Lokasi pada kwitansi kosong, dimohon aktifkan izin lokasi pada aplikasi.");
                builder.setPositiveButton("Mengerti", null);
                builder.show();
                return false;
            }
        });

            MenuItem saveMenuItem = menu.findItem(R.id.menu_item_save);
        saveMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                createPdf();
                return true;
            }
        });

        MenuItem shareMenuItem = menu.findItem(R.id.menu_item_share);
        shareMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                shareImage();
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    private class PrintTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            KwitansiDB kwitansiDB = Room.databaseBuilder(getContext(), KwitansiDB.class, "kwitansi-db").build();
            HistoryDAO historyDAO = kwitansiDB.getHistoryDAO();

            String history_nomor = textNomor.getText().toString();
            String history_pengirim = textNamaPengirim.getText().toString();
            String history_penerima = textNamaPenerima.getText().toString();
            String history_nominal = textNominal.getText().toString();
            String history_deskripsi = textDeskripsi.getText().toString();

            // Mendapatkan tanggal dan jam saat ini
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            String tanggal = dateFormat.format(new Date());
            String jam = timeFormat.format(new Date());

            // Membuat objek HistoryEntity dari data yang ada di PreviewFragment
            HistoryEntity history = new HistoryEntity(history_nomor, history_pengirim, history_penerima, history_nominal, history_deskripsi, tanggal, jam);

            // Menyimpan data ke dalam tabel history
            historyDAO.insertHistory(history);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
        }
    }

    public void doPrint() {
        try {
            // Menghubungkan dengan printer Bluetooth
            BluetoothPrintersConnections printers = new BluetoothPrintersConnections();
            BluetoothConnection[] connections = printers.getList();

            if (connections != null && connections.length > 0) {
                // Menggunakan koneksi Bluetooth pertama
                BluetoothConnection connection = connections[0];
                EscPosPrinterCommands printerCommands = new EscPosPrinterCommands(connection);

                View view = getActivity().findViewById(R.id.area_layout_print);
                int viewWidth = view.getWidth();
                int viewHeight = view.getHeight();
                int printWidth = viewWidth;
                int printHeight = viewHeight;

                Bitmap originalBitmap = Bitmap.createBitmap(printWidth, printHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(originalBitmap);
                canvas.drawColor(Color.WHITE);
                view.draw(canvas);

                // Rotasi bitmap menjadi potret
                Matrix matrix = new Matrix();
                matrix.postRotate(90); // Rotasi 90 derajat searah jarum jam
                Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);

                int targetWidth = 400;
                Bitmap rescaledBitmap = Bitmap.createScaledBitmap(
                        rotatedBitmap,
                        targetWidth,
                        Math.round(((float) rotatedBitmap.getHeight()) * ((float) targetWidth) / ((float) rotatedBitmap.getWidth())),
                        true
                );

                printerCommands.connect();
                printerCommands.reset();
                printerCommands.printImage(EscPosPrinterCommands.bitmapToBytes(rescaledBitmap, false));
                printerCommands.feedPaper(125);
                printerCommands.cutPaper();
                printerCommands.disconnect();

                Toast.makeText(getContext(), "Berhasil mencetak!", Toast.LENGTH_SHORT).show();
                new PrintTask().execute();

            } else {
                Toast.makeText(getContext(), "Tidak ada printer yang terhubung!", Toast.LENGTH_SHORT).show();
            }
        } catch (EscPosConnectionException e) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Something went wrong")
                    .setMessage(e.getMessage())
                    .show();
        } catch (Exception e) {
            Log.e("APP", "Tidak dapat mencetak", e);
        }
    }



    private String saveBitmapToStorage(Bitmap bitmap) {
        String filePath = ""; // Path file penyimpanan gambar
        try {
            // Mengubah orientasi gambar menjadi potret
            Matrix matrix = new Matrix();
            matrix.postRotate(90); // Mengatur rotasi 90 derajat (potret)
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            // Menyimpan gambar ke penyimpanan
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Kwitansi Digital");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = "print_image.jpg";
            File file = new File(dir, fileName);

            FileOutputStream outputStream = new FileOutputStream(file);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            filePath = file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath;
    }

    private void shareImage() {
        try {
            // Mengambil tangkapan layar dari layout
            View view = getActivity().findViewById(R.id.area_layout_print);
            view.setDrawingCacheEnabled(true);
            view.measure(View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(view.getHeight(), View.MeasureSpec.EXACTLY));
            view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

            view.buildDrawingCache(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);

            // Simpan gambar sementara ke penyimpanan eksternal
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

            // Bagikan gambar menggunakan FileProvider
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_item_edit) {

            kwitansiDB = Room.databaseBuilder(context, KwitansiDB.class, "KwitansiDB").allowMainThreadQueries().build();
            kwitansiDB = KwitansiDB.getInstance(context);
            kwitansiDAO = kwitansiDB.getKwitansiDAO();

            View popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_edit_data, null);

            EditText editTextNamaPengirim = popupView.findViewById(R.id.edit_text_DataNamaPengirim);
            EditText editTextNamaPenerima = popupView.findViewById(R.id.edit_text_DataNamaPenerima);
            EditText editTextNominal = popupView.findViewById(R.id.edit_text_DataNominal);
            EditText editTextDeskripsi = popupView.findViewById(R.id.edit_text_DataDeskripsi);

            Bundle bundle = getArguments();
            if (bundle != null) {
                String namaPengirim = bundle.getString("DataNamaPengirim");
                String namaPenerima = bundle.getString("DataNamaPenerima");
                String nominal = bundle.getString("DataNominal");
                String deskripsi = bundle.getString("DataDeskripsi");

                editTextNamaPengirim.setText(namaPengirim);
                editTextNamaPenerima.setText(namaPenerima);
                editTextNominal.setText(nominal);
                editTextDeskripsi.setText(deskripsi);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Edit Data").setView(popupView).setPositiveButton("Simpan", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {


                }
            }).setNegativeButton("Batal", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).show();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}