package com.kudig.kwitansidigital.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "Kwitansi")
public class KwitansiEntity implements Serializable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "kwitansi_id")
    private int id;
    @ColumnInfo(name = "nomor")
    private String nomor;
    @ColumnInfo(name = "nama_pengirim")
    private String namaPengirim;

    @ColumnInfo(name = "nama_penerima")
    private String namaPenerima;

    @ColumnInfo(name = "nominal")
    private long nominal;

    @ColumnInfo(name = "deskripsi")
    private String deskripsi;

    public KwitansiEntity(int id, String nomor, String namaPengirim, String namaPenerima, long nominal, String deskripsi) {
        this.id = id;
        this.nomor = nomor;
        this.namaPengirim = namaPengirim;
        this.namaPenerima = namaPenerima;
        this.nominal = nominal;
        this.deskripsi = deskripsi;
    }

    @Ignore
    public KwitansiEntity(String nomor, String namaPengirim, String namaPenerima, long nominal, String deskripsi) {
        this.nomor = nomor;
        this.namaPengirim = namaPengirim;
        this.namaPenerima = namaPenerima;
        this.nominal = nominal;
        this.deskripsi = deskripsi;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNomor() {
        return nomor;
    }

    public void setNomor(String nomor) {
        this.nomor = nomor;
    }

    public String getNamaPengirim() {
        return namaPengirim;
    }

    public void setNamaPengirim(String namaPengirim) {
        this.namaPengirim = namaPengirim;
    }


    public String getNamaPenerima() {
        return namaPenerima;
    }

    public void setNamaPenerima(String namaPenerima) {
        this.namaPenerima = namaPenerima;
    }

    public long getNominal() {
        return nominal;
    }

    public void setNominal(long nominal) {
        this.nominal = nominal;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public void setDeskripsi(String deskripsi) {
        this.deskripsi = deskripsi;
    }
}
