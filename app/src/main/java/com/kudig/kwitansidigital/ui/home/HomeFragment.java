package com.kudig.kwitansidigital.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.kudig.kwitansidigital.R;
import com.kudig.kwitansidigital.databinding.FragmentHomeBinding;
import com.kudig.kwitansidigital.db.KwitansiDAO;
import com.kudig.kwitansidigital.db.KwitansiDB;
import com.kudig.kwitansidigital.db.KwitansiEntity;
import com.kudig.kwitansidigital.ui.adapter.KwitansiAdapter;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    EditText namaET, nominalET, deskripsiET;
    KwitansiDB KwitansiDB;
    List<KwitansiEntity> KwitansiList;
    ListView list;
    KwitansiDAO kwitansiDAO;
    RecyclerView myRecycler;
    KwitansiAdapter kwitansiAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        setHasOptionsMenu(true);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        myRecycler = root.findViewById(R.id.recycler_fragment);
        kwitansiAdapter = new KwitansiAdapter(requireContext());
        myRecycler.setAdapter(kwitansiAdapter);
        myRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        RoomDatabase.Callback myCallBack = new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);
            }

            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
            }
        };

        KwitansiDB = Room.databaseBuilder(getContext(), KwitansiDB.class,
                "KwitansiDB").addCallback(myCallBack).build();

        kwitansiDAO = KwitansiDB.getKwitansiDAO();

        KwitansiDB = KwitansiDB.getInstance(getContext());

        SwipeRefreshLayout swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Panggil metode fetchData() untuk memuat ulang data
                fetchData();

                // Berhenti mengindikasikan proses refresh
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        return root;

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_home, menu);
        MenuItem importDBMenuItem = menu.findItem(R.id.menu_item_import_data);
        importDBMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                importCSV();
                return false;
            }
        });
        MenuItem exportDBMenuItem = menu.findItem(R.id.menu_item_export_data);
        exportDBMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                performExportCSV();
                return false;
            }
        });
        MenuItem helpMenuItem = menu.findItem(R.id.menu_item_info);
        helpMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                // Tampilkan AlertDialog
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Informasi");
                builder.setMessage("Tekan Lama  = Menu Edit dan Hapus data.\nTekan Sekali = Melihat Tampilan Kwitansi.");
                builder.setPositiveButton("OK", null);
                builder.show();
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void importCSV() {
        String filePathAndName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + "Kwitansi Digital" + "/" + "RoomDB_Backup.csv";
        File csvFile = new File(filePathAndName);

        if (csvFile.exists()) {
            try {
                CSVReader csvReader = new CSVReader(new FileReader(csvFile.getAbsoluteFile()));
                String[] nextLine;
                while ((nextLine = csvReader.readNext()) != null) {
                    String nomor = nextLine[0];
                    String nama = nextLine[1];
                    String nama_penerima = nextLine[2];
                    String nominal = nextLine[3];
                    String deskripsi = nextLine[4];

                    KwitansiDB db = Room.databaseBuilder(getContext(), KwitansiDB.class, "KwitansiDB").allowMainThreadQueries().build();
                    KwitansiDAO kwitansiDAO = db.getKwitansiDAO();
                    kwitansiDAO.addKwitansi(new KwitansiEntity(nomor, nama, nama_penerima, nominal, deskripsi));
                }
                Toast.makeText(getContext(), "Backup Restored", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Berhasil " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Tidak Ada Backup Ditemukan", Toast.LENGTH_SHORT).show();
        }
    }

    private void performExportCSV() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File backupDir = new File(downloadsDir, "Kwitansi Digital");

        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                Toast.makeText(getContext(), "Failed to create backup directory", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Specify the file path and name
        String csvFileName = "RoomDB_Backup.csv";
        File file = new File(backupDir, csvFileName);
        String filePathAndName = file.getAbsolutePath();

        ArrayList<KwitansiEntity> recordsList = new ArrayList<>();
        recordsList.clear();

        KwitansiDB db = Room.databaseBuilder(getContext(), KwitansiDB.class, "KwitansiDB")
                .allowMainThreadQueries().build();

        KwitansiDAO kwitansiDAO = db.getKwitansiDAO();
        List<KwitansiEntity> kwitansiList = kwitansiDAO.getAllKwitansi();
        recordsList = new ArrayList<>(kwitansiList);

        try {
            FileWriter fileWriter = new FileWriter(filePathAndName);
            for (int i = 0; i < recordsList.size(); i++) {
                fileWriter.append("" + recordsList.get(i).getNomor());
                fileWriter.append(",");
                fileWriter.append("" + recordsList.get(i).getNama());
                fileWriter.append(",");
                fileWriter.append("" + recordsList.get(i).getNama_penerima());
                fileWriter.append(",");
                fileWriter.append("" + recordsList.get(i).getNominal());
                fileWriter.append(",");
                fileWriter.append("" + recordsList.get(i).getDeskripsi());
                fileWriter.append("\n");
            }
            fileWriter.flush();
            fileWriter.close();
            Toast.makeText(getContext(), "Backup Berhasil di Export ke " + filePathAndName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void fetchData() {
        SwipeRefreshLayout swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setRefreshing(true);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executorService.execute(() -> {
            List<KwitansiEntity> kwitansiList = kwitansiDAO.getAllKwitansi();

            handler.post(() -> {
                kwitansiAdapter.clear();

                for (KwitansiEntity kwitansi : kwitansiList) {
                    kwitansiAdapter.addKwitansi(kwitansi);
                }

                if (kwitansiList.isEmpty()) {
                    binding.emptyTextView.setVisibility(View.VISIBLE);
                } else {
                    binding.emptyTextView.setVisibility(View.GONE);
                }
                swipeRefreshLayout.setRefreshing(false);
            });
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        fetchData();
    }

}