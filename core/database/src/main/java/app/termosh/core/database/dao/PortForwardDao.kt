package app.termosh.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.termosh.core.database.entity.PortForwardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortForwardDao {
    @Query("SELECT * FROM port_forwards WHERE serverId = :serverId ORDER BY createdAt ASC")
    fun observeForServer(serverId: String): Flow<List<PortForwardEntity>>

    @Query("SELECT * FROM port_forwards WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PortForwardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: PortForwardEntity)

    @Query("DELETE FROM port_forwards WHERE id = :id")
    suspend fun deleteById(id: String)
}
