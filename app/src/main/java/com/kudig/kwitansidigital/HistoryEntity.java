package com.kudig.kwitansidigital;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class HistoryEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "history_nomor")
    String history_nomor;
    @ColumnInfo(name = "history_pengirim")
    String history_pengirim;

    @ColumnInfo(name = "history_penerima")
    String history_penerima;

    @ColumnInfo(name = "history_nominal")
    String history_nominal;

    @ColumnInfo(name = "history_deskripsi")
    String history_deskripsi;

    @ColumnInfo(name = "history_tanggal")
    String history_tanggal;

    @ColumnInfo(name = "history_jam")
    String history_jam;

    public HistoryEntity(String history_nomor, String history_pengirim, String history_penerima, String history_nominal, String history_deskripsi, String history_tanggal, String history_jam) {
        this.history_nomor = history_nomor;
        this.history_pengirim = history_pengirim;
        this.history_penerima = history_penerima;
        this.history_nominal = history_nominal;
        this.history_deskripsi = history_deskripsi;
        this.history_tanggal = history_tanggal;
        this.history_jam = history_jam;
        this.id = 0;
    }

    @Ignore
    public HistoryEntity(String history_nomor, String history_Pengirim, String history_Penerima, double history_nominal, String history_deskripsi) {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHistory_nomor() {
        return history_nomor;
    }

    public void setHistory_nomor(String history_nomor) {
        this.history_nomor = history_nomor;
    }

    public String getHistory_pengirim() {
        return history_pengirim;
    }

    public void setHistory_pengirim(String history_pengirim) {
        this.history_pengirim = history_pengirim;
    }

    public String getHistory_penerima() {
        return history_penerima;
    }

    public void setHistory_penerima(String history_penerima) {
        this.history_penerima = history_penerima;
    }

    public String getHistory_nominal() {
        return history_nominal;
    }

    public void setHistory_nominal(String history_nominal) {
        this.history_nominal = history_nominal;
    }

    public String getHistory_deskripsi() {
        return history_deskripsi;
    }

    public void setHistory_deskripsi(String history_deskripsi) {
        this.history_deskripsi = history_deskripsi;
    }

    public String getHistory_tanggal() {
        return history_tanggal;
    }

    public void setHistory_tanggal(String history_tanggal) {
        this.history_tanggal = history_tanggal;
    }

    public String getHistory_jam() {
        return history_jam;
    }

    public void setHistory_jam(String history_jam) {
        this.history_jam = history_jam;
    }
}
