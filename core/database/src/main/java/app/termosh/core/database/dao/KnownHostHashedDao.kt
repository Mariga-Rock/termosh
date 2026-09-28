package app.termosh.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.termosh.core.database.entity.KnownHostHashedEntity

@Dao
interface KnownHostHashedDao {

    @Query("SELECT * FROM known_hosts_hashed")
    suspend fun getAll(): List<KnownHostHashedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: KnownHostHashedEntity)

    @Query("DELETE FROM known_hosts_hashed")
    suspend fun deleteAll()
}
