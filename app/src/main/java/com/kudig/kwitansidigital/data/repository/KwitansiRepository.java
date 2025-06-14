package com.kudig.kwitansidigital.data.repository;

import android.app.Application;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import com.kudig.kwitansidigital.db.HistoryDAO;
import com.kudig.kwitansidigital.db.HistoryEntity;
import com.kudig.kwitansidigital.db.KwitansiDAO;
import com.kudig.kwitansidigital.db.KwitansiDB;
import com.kudig.kwitansidigital.db.KwitansiEntity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KwitansiRepository {
    private final KwitansiDAO kwitansiDAO;
    private final HistoryDAO historyDAO;
    private final ExecutorService executorService;
    private final Context context;

    public KwitansiRepository(Application application) {
        this.context = application.getApplicationContext();
        KwitansiDB database = KwitansiDB.getInstance(application);
        kwitansiDAO = database.getKwitansiDAO();
        historyDAO = database.getHistoryDAO();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insertHistory(KwitansiEntity kwitansi) {
        executorService.execute(() -> {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String tanggal = dateFormat.format(new Date());
                    String jam = timeFormat.format(new Date());

                    HistoryEntity history = new HistoryEntity(
                            kwitansi.getNomor(),
                            kwitansi.getNamaPenerima(),
                            kwitansi.getNamaPengirim(),
                            String.valueOf(kwitansi.getNominal()),
                            kwitansi.getDeskripsi(),
                            tanggal,
                            jam
                    );
                    historyDAO.insertHistory(history);
                }

        );
    }

    public void updateKwitansi(KwitansiEntity kwitansi) {
        executorService.execute(() -> kwitansiDAO.updateKwitansi(kwitansi));
    }

    public String getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            assert addresses != null;
            if (!addresses.isEmpty()) {
                String kecamatan = addresses.get(0).getSubAdminArea();
                return kecamatan != null ? kecamatan.replace("Kabupaten ", "") : "Unknown Location";
            }
        } catch (IOException e) {
            Log.e("Repository", "Error getting address from location", e);
        }
        return "Unknown Location";
    }
}
