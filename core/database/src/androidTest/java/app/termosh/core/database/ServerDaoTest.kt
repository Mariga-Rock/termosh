package app.termosh.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.termosh.core.database.entity.AuthType
import app.termosh.core.database.entity.ServerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServerDaoTest {

    private lateinit var db: TermoshDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TermoshDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertAndObserve() = runTest {
        val server = ServerEntity(
            id = "s1",
            name = "prod",
            host = "example.com",
            port = 22,
            username = "root",
            authType = AuthType.KEY,
            createdAt = 1L,
        )
        db.serverDao().upsert(server)
        val list = db.serverDao().observeAll().first()
        assertEquals(1, list.size)
        assertEquals("prod", list.first().name)
    }
}
