package com.kudig.kwitansidigital.ui.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.kudig.kwitansidigital.db.KwitansiEntity;
import com.kudig.kwitansidigital.R;
import com.kudig.kwitansidigital.db.KwitansiDAO;
import com.kudig.kwitansidigital.db.KwitansiDB;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class KwitansiAdapter extends RecyclerView.Adapter<KwitansiAdapter.MyViewHolder> {

    private Context context;
    private KwitansiDB kwitansiDB;
    private KwitansiDAO kwitansiDAO;
    private KwitansiEntity kwitansiEntity;
    private List<KwitansiEntity> KwitansiList;

    private int selectedPosition = RecyclerView.NO_POSITION;

    public void clear() {
        KwitansiList.clear();
        notifyDataSetChanged();
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

    public void deleteItem(int position) {
        KwitansiEntity kwitansi = KwitansiList.get(position);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                KwitansiDB.getInstance(context).getKwitansiDAO().deleteKwitansi(kwitansi);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                KwitansiList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, KwitansiList.size());
                Toast.makeText(context, "Data berhasil dihapus", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    public KwitansiAdapter(Context context) {
        this.context = context;
        KwitansiList = new ArrayList<>();
    }

    public void addKwitansi(KwitansiEntity Kwitansi) {
        int nextId = KwitansiList.size() + 1; // Nomor urut berdasarkan ukuran daftar saat ini
        String nomor = String.format("KW%04d", nextId); // Format nomor urut sesuai kebutuhan Anda
        Kwitansi.setNomor(nomor); // Set nomor urut pada KwitansiEntity

        KwitansiList.add(Kwitansi);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_view_design, parent, false);
        return new MyViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        KwitansiEntity Kwitansi = KwitansiList.get(position);

        holder.nomer_Kwitansi.setText(Kwitansi.getNomor());
        holder.nama_pengirim.setText(Kwitansi.getNama());
        holder.nama_penerima.setText(Kwitansi.getNama_penerima());

        double nominalValue = Double.parseDouble(Kwitansi.getNominal());

        Locale indonesianLocale = new Locale("id", "ID");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(indonesianLocale);
        String formattedNominal = currencyFormatter.format(nominalValue);

        // Menghapus digit desimal (",00") di belakang nominal
        if (formattedNominal.endsWith(",00")) {
            formattedNominal = formattedNominal.substring(0, formattedNominal.length() - 3);
        }

        formattedNominal = formattedNominal.replace("Rp", "Rp ");

        holder.nominal_kwitansi.setText(formattedNominal);
        holder.deskripsi.setText(Kwitansi.getDeskripsi());

        holder.ListKwitansi.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {

                final String[] action = {"Edit", "Hapus"};
                AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
                alert.setItems(action, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        switch (i) {
                            case 0:
                                kwitansiDB = Room.databaseBuilder(context, KwitansiDB.class, "KwitansiDB").allowMainThreadQueries().build();
                                kwitansiDB = KwitansiDB.getInstance(context);
                                kwitansiDAO = kwitansiDB.getKwitansiDAO();

                                View editPopupView = LayoutInflater.from(context).inflate(R.layout.popup_edit_data, null);

                                EditText editTextDataNamaPengirim = editPopupView.findViewById(R.id.edit_text_DataNamaPengirim);
                                EditText editTextDataNamaPenerima = editPopupView.findViewById(R.id.edit_text_DataNamaPenerima);
                                EditText editTextDataNominal = editPopupView.findViewById(R.id.edit_text_DataNominal);
                                EditText editTextDataDeskripsi = editPopupView.findViewById(R.id.edit_text_DataDeskripsi);

                                // Mengisi nilai awal EditText dengan data yang ada di ViewHolder
                                editTextDataNamaPengirim.setText(KwitansiList.get(holder.getAdapterPosition()).getNama());
                                editTextDataNamaPenerima.setText(KwitansiList.get(holder.getAdapterPosition()).getNama_penerima());
                                editTextDataNominal.setText(KwitansiList.get(holder.getAdapterPosition()).getNominal());
                                editTextDataDeskripsi.setText(KwitansiList.get(holder.getAdapterPosition()).getDeskripsi());

                                AlertDialog.Builder editPopupBuilder = new AlertDialog.Builder(context);
                                editPopupBuilder.setTitle("Edit Data");
                                editPopupBuilder.setView(editPopupView);
                                editPopupBuilder.setPositiveButton("Simpan", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Mendapatkan data yang diubah dari EditText
                                        String editedDataNama = editTextDataNamaPengirim.getText().toString();
                                        String editedDataNamaPenerima = editTextDataNamaPenerima.getText().toString();
                                        String editedDataNominal = editTextDataNominal.getText().toString();
                                        String editedDataDeskripsi = editTextDataDeskripsi.getText().toString();

                                        editedDataNama = capitalizeWords(editedDataNama);
                                        editedDataNamaPenerima = capitalizeWords(editedDataNamaPenerima);

                                        // Mengubah nilai data di dalam objek KwitansiEntity
                                        Kwitansi.setNama(editedDataNama);
                                        Kwitansi.setNama_penerima(editedDataNamaPenerima);
                                        Kwitansi.setNominal(editedDataNominal);
                                        Kwitansi.setDeskripsi(editedDataDeskripsi);

                                        // Melakukan update data KwitansiEntity di database
                                        new UpdateKwitansiAsyncTask(kwitansiDAO, Kwitansi).execute();

                                        notifyDataSetChanged();
                                        Toast.makeText(context, "Data berhasil diedit", Toast.LENGTH_SHORT).show();
                                    }

                                });
                                editPopupBuilder.setNegativeButton("Batal", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });

                                AlertDialog editPopupDialog = editPopupBuilder.create();
                                editPopupDialog.show();
                                break;

                            case 1:
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                builder.setTitle("Konfirmasi");
                                builder.setMessage("Apakah Anda yakin ingin menghapus data ini?");
                                builder.setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        int position = holder.getAdapterPosition();
                                        if (position != RecyclerView.NO_POSITION) {
                                            deleteItem(position);
                                            Toast.makeText(context, "Data berhasil dihapus", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                                builder.setNegativeButton("Tidak", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                AlertDialog alertDialog = builder.create();
                                alertDialog.show();
                                break;

                        }
                    }
                });
                alert.create();
                alert.show();
                return true;
            }
        });

        holder.ListKwitansi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    KwitansiEntity kwitansi = KwitansiList.get(position);

                    Bundle bundle = new Bundle();
                    bundle.putString("DataNomor", KwitansiList.get(position).getNomor());
                    bundle.putString("DataNamaPengirim", KwitansiList.get(position).getNama());
                    bundle.putString("DataNamaPenerima", KwitansiList.get(position).getNama_penerima());
                    bundle.putString("DataNominal", KwitansiList.get(position).getNominal());
                    bundle.putString("DataDeskripsi", KwitansiList.get(position).getDeskripsi());

                    NavController navController = Navigation.findNavController(v);
                    navController.navigate(R.id.navigation_preview, bundle);
                }
            }
        });

    }

    private static class UpdateKwitansiAsyncTask extends AsyncTask<Void, Void, Void> {
        private KwitansiDAO kwitansiDAO;
        private KwitansiEntity kwitansiEntity;

        public UpdateKwitansiAsyncTask(KwitansiDAO kwitansiDAO, KwitansiEntity kwitansiEntity) {
            this.kwitansiDAO = kwitansiDAO;
            this.kwitansiEntity = kwitansiEntity;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            kwitansiDAO.updateKwitansi(kwitansiEntity);
            return null;
        }
    }


    @Override
    public int getItemCount() {
        return KwitansiList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView nomer_Kwitansi, nominal_kwitansi, nama_pengirim, nama_penerima, deskripsi;
        private LinearLayout ListKwitansi;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            nomer_Kwitansi = itemView.findViewById(R.id.no_kwitansi);
            nominal_kwitansi = itemView.findViewById(R.id.nominal_kwitansi);
            nama_pengirim = itemView.findViewById(R.id.nama_pengirim);
            nama_penerima = itemView.findViewById(R.id.nama_penerima);
            deskripsi = itemView.findViewById(R.id.deskripsi);
            ListKwitansi = itemView.findViewById(R.id.list_item);
        }
    }
}
