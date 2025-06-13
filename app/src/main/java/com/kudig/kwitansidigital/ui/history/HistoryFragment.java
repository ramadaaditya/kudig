package com.kudig.kwitansidigital.ui.history;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.kudig.kwitansidigital.HistoryAdapter;
import com.kudig.kwitansidigital.HistoryDAO;
import com.kudig.kwitansidigital.HistoryEntity;
import com.kudig.kwitansidigital.KwitansiAdapter;
import com.kudig.kwitansidigital.KwitansiDAO;
import com.kudig.kwitansidigital.KwitansiDB;
import com.kudig.kwitansidigital.KwitansiEntity;
import com.kudig.kwitansidigital.R;
import com.kudig.kwitansidigital.databinding.FragmentHomeBinding;
import com.kudig.kwitansidigital.ui.add.AddFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryFragment extends Fragment {
    private FragmentHomeBinding binding;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private HistoryAdapter historyAdapter;
    private List<HistoryEntity> historyList;
    private HistoryDAO historyDAO;
    private KwitansiDB kwitansiDB;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshHistory);
        recyclerView = view.findViewById(R.id.recycler_history_fragment);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setHasOptionsMenu(true);

        historyList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(historyList);
        recyclerView.setAdapter(historyAdapter);

        kwitansiDB = Room.databaseBuilder(getContext(), KwitansiDB.class, "kwitansi-db")
                .build();
        historyDAO = kwitansiDB.getHistoryDAO();

        // Mengambil data history dari database dan menampilkannya dalam RecyclerView
        loadHistory();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshHistory();
            }
        });

        return view;
    }

    private void loadHistory() {
        new LoadHistoryTask().execute();
    }

    private void refreshHistory() {
        new RefreshHistoryTask().execute();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_history, menu);
        MenuItem aboutMenuItem = menu.findItem(R.id.menu_item_about_history);
//        MenuItem privacyMenuItem = menu.findItem(R.id.menu_item_privacy);
        MenuItem deleteallMenuItem = menu.findItem(R.id.menu_item_delete);

        aboutMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                NavController navController = NavHostFragment.findNavController(HistoryFragment.this);
                navController.navigate(R.id.navigation_about);
                return false;
            }
        });
//        privacyMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
//                NavController navController = NavHostFragment.findNavController(HistoryFragment.this);
//                // Melakukan navigasi ke destination dengan ID R.id.navigation_about
//                navController.navigate(R.id.navigation_privacy_policy);
//                return false;
//            }
//        });
        deleteallMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Konfirmasi");
                builder.setMessage("Apakah Anda yakin ingin menghapus semua history?");
                builder.setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new DeleteHistoryTask().execute();
                    }
                });
                builder.setNegativeButton("Tidak", null);
                builder.show();
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    private class DeleteHistoryTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            historyDAO.deleteAllHistory();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            loadHistory(); // Memuat ulang data history setelah menghapus semua history
            Toast.makeText(getContext(), "Semua history telah dihapus", Toast.LENGTH_SHORT).show();
        }
    }

    private class LoadHistoryTask extends AsyncTask<Void, Void, List<HistoryEntity>> {
        @Override
        protected List<HistoryEntity> doInBackground(Void... voids) {
            return historyDAO.getAllHistory();
        }

        @Override
        protected void onPostExecute(List<HistoryEntity> historyList) {
            historyAdapter.setHistoryList(historyList);
        }
    }

    private class RefreshHistoryTask extends AsyncTask<Void, Void, List<HistoryEntity>> {
        @Override
        protected List<HistoryEntity> doInBackground(Void... voids) {
            // Lakukan operasi refresh data history di sini
            // Misalnya, mengambil data terbaru dari server atau sumber data lainnya
            List<HistoryEntity> refreshedHistoryList = new ArrayList<>();
            // Isi dengan operasi yang sesuai untuk memperbarui data history

            return refreshedHistoryList;
        }

        @Override
        protected void onPostExecute(List<HistoryEntity> refreshedHistoryList) {
            swipeRefreshLayout.setRefreshing(false); // Menghentikan indikator loading
            historyList.clear();
            historyList.addAll(refreshedHistoryList);
            historyAdapter.notifyDataSetChanged();
        }
    }
}


