package com.kudig.kwitansidigital;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {KwitansiEntity.class, HistoryEntity.class}, version = 6,  exportSchema = false)
public abstract class KwitansiDB extends RoomDatabase {
    // Metode lain yang sudah ada
    public abstract KwitansiDAO getKwitansiDAO();
    public abstract HistoryDAO getHistoryDAO();
    private static KwitansiDB instance;
    public static synchronized KwitansiDB getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            KwitansiDB.class, "KwitansiDB")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
