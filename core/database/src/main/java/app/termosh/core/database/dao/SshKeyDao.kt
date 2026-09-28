package app.termosh.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.termosh.core.database.entity.SshKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SshKeyDao {

    @Query("SELECT * FROM ssh_keys ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SshKeyEntity>>

    @Query("SELECT * FROM ssh_keys WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SshKeyEntity?

    @Query("SELECT * FROM ssh_keys WHERE fingerprintSha256 = :fp LIMIT 1")
    suspend fun getByFingerprint(fp: String): SshKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(key: SshKeyEntity)

    @Delete
    suspend fun delete(key: SshKeyEntity)

    @Query("DELETE FROM ssh_keys WHERE id = :id")
    suspend fun deleteById(id: String)
}
