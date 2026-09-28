package app.termosh.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.termosh.core.database.entity.TotpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TotpDao {

    @Query("SELECT * FROM totp_secrets ORDER BY label ASC")
    fun observeAll(): Flow<List<TotpEntity>>

    @Query("SELECT * FROM totp_secrets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TotpEntity?

    @Query("SELECT * FROM totp_secrets WHERE label = :label LIMIT 1")
    suspend fun getByLabel(label: String): TotpEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: TotpEntity)

    @Query("DELETE FROM totp_secrets WHERE id = :id")
    suspend fun deleteById(id: String)
}
