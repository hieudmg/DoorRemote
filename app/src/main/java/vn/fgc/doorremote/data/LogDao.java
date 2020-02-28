package vn.fgc.doorremote.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LogDao {
    @Query("SELECT * FROM applog")
    List<AppLog> getAll();

    @Query("SELECT * FROM applog WHERE type LIKE :type")
    List<AppLog> getByType(String type);

    @Query("DELETE FROM applog WHERE type LIKE :type")
    void deleteByType(String type);

    @Insert
    void addLog(AppLog appLogs);

    @Delete
    void deleteLog(AppLog appLog);
}
