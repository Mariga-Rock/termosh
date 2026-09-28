package app.termosh.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.termosh.core.database.entity.SnippetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {

    @Query("SELECT * FROM snippets ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<SnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snippet: SnippetEntity)

    @Delete
    suspend fun delete(snippet: SnippetEntity)
}
