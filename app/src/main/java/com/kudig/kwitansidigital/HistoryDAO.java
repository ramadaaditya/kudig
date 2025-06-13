package com.kudig.kwitansidigital;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

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
