package com.kudig.kwitansidigital.ui.add;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.kudig.kwitansidigital.db.KwitansiEntity;
import com.kudig.kwitansidigital.R;
import com.kudig.kwitansidigital.databinding.FragmentAddBinding;
import com.kudig.kwitansidigital.db.KwitansiDAO;
import com.kudig.kwitansidigital.db.KwitansiDB;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddFragment extends Fragment {

    EditText nomerET, namaET, nama1ET, nominalET, deskripsiET;
    Button save;
    KwitansiDB KwitansiDB;
    ListView list;
    KwitansiDAO kwitansiDAO;
    private FragmentAddBinding binding;

    public static final int PERMISSION_BLUETOOTH = 1;
    private final Locale locale = new Locale("id", "ID");
    private final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a", locale);
    private final NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
    private int counter = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        namaET = view.findViewById(R.id.input_nama_terimadari);
        nama1ET = view.findViewById(R.id.input_nama_penerima);
        nominalET = view.findViewById(R.id.input_nominal);
        deskripsiET = view.findViewById(R.id.input_deskripsi);
        nomerET = view.findViewById(R.id.input_id);
        save = view.findViewById(R.id.simpan);

        setHasOptionsMenu(true);

        String nama = namaET.getText().toString();
        String nominal = nominalET.getText().toString();
        String deskripsi = deskripsiET.getText().toString();
        String nama_penerima = nama1ET.getText().toString();
        String nomor = generateNomer();
        nomerET.setText(nomor);

        KwitansiEntity kwitansi = new KwitansiEntity(nomor, nama, nama_penerima, nominal, deskripsi);

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

        KwitansiDB = Room.databaseBuilder(getContext(), KwitansiDB.class, getContext().getExternalFilesDir(null) + "/databases/KwitansiDB").addCallback(myCallBack).build();
        KwitansiDB = KwitansiDB.getInstance(getContext());
        kwitansiDAO = KwitansiDB.getKwitansiDAO();

        save.setOnClickListener(views -> {
            String nama1 = namaET.getText().toString();
            String nama_penerima1 = nama1ET.getText().toString();
            String nominal1 = nominalET.getText().toString();
            String deskripsi1 = deskripsiET.getText().toString();
            String nomor1 = nomerET.getText().toString();

            nama1 = capitalizeWords(nama1);
            nama_penerima1 = capitalizeWords(nama_penerima1);

            if (nama1.isEmpty() || nama_penerima1.isEmpty() || nominal1.isEmpty() || deskripsi1.isEmpty()) {
                Toast.makeText(requireContext(), "Harap isi semua data", Toast.LENGTH_SHORT).show();
            } else {
                KwitansiEntity k1 = new KwitansiEntity(nomor1, nama1, nama_penerima1, nominal1, deskripsi1);
                addKwitansiInBackground(k1);

                namaET.setText("");
                nama1ET.setText("");
                nominalET.setText("");
                deskripsiET.setText("");
                nomerET.setText("");
            }
        });

        binding = FragmentAddBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private String capitalizeWords(String text) {
        if (TextUtils.isEmpty(text)) {
            return text;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder capitalizedText = new StringBuilder();
        for (String word : words) {
            if (!TextUtils.isEmpty(word)) {
                String capitalizedWord = Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
                capitalizedText.append(capitalizedWord).append(" ");
            }
        }
        return capitalizedText.toString().trim();
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_about, menu);
        MenuItem helpMenuItem = menu.findItem(R.id.menu_item_about_history);
        helpMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                NavController navController = NavHostFragment.findNavController(AddFragment.this);
                navController.navigate(R.id.navigation_about);
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    public void addKwitansiInBackground(KwitansiEntity kwitansi) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executorService.execute(() -> {
            int id = kwitansiDAO.getNextId();
            kwitansi.setId(id);
            KwitansiDB.getKwitansiDAO().addKwitansi(kwitansi);
            handler.post(() -> Toast.makeText(getContext(), "Kwitansi Berhasil Dibuat", Toast.LENGTH_SHORT).show());
        });
    }

    private String generateNomer() {
        counter++;
        return String.format("%d", counter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}