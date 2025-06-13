package com.kudig.kwitansidigital.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HistoryDAO {
    @Insert
    void insertHistory(HistoryEntity history);

    @Query("SELECT * FROM history")
    List<HistoryEntity> getAllHistory();

    @Query("DELETE FROM history")
    void deleteAllHistory();
}
