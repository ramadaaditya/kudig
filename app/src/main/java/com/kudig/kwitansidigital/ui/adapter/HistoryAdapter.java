package com.kudig.kwitansidigital.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kudig.kwitansidigital.db.HistoryEntity;
import com.kudig.kwitansidigital.R;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<HistoryEntity> historyList;

    public HistoryAdapter(List<HistoryEntity> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryEntity kwitansi = historyList.get(position);

        holder.nomorTextView.setText(kwitansi.getHistory_nomor());
        holder.pengirimTextView.setText(kwitansi.getHistory_pengirim());
        holder.penerimaTextView.setText(kwitansi.getHistory_penerima());
        String nominal = "Rp " + String.valueOf(kwitansi.getHistory_nominal());
        holder.nominalTextView.setText(nominal);
        holder.deskripsiTextView.setText(kwitansi.getHistory_deskripsi());
        holder.tanggalTextView.setText(kwitansi.getHistory_tanggal());
        holder.jamTextView.setText(kwitansi.getHistory_jam());
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void setHistoryList(List<HistoryEntity> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nomorTextView, deskripsiTextView, pengirimTextView, penerimaTextView, nominalTextView, tanggalTextView, jamTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            nomorTextView = itemView.findViewById(R.id.text_nomor_history);
            pengirimTextView = itemView.findViewById(R.id.text_nama_pengirim_history);
            penerimaTextView = itemView.findViewById(R.id.text_nama_penerima_history);
            nominalTextView = itemView.findViewById(R.id.text_nominal_history);
            deskripsiTextView = itemView.findViewById(R.id.text_deskripsi_history);
            tanggalTextView = itemView.findViewById(R.id.text_tanggal_history);
            jamTextView = itemView.findViewById(R.id.text_jam_history);
        }
    }
}

