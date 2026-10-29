package io.github.aedev.flow.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.aedev.flow.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class WatchHistoryImportTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .build()
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    @Test fun `an imported stub cannot overwrite actual playback progress`() =
        runBlocking {
            val played = entry(50).copy(position = 100, duration = 1000)
            db.watchHistoryDao().upsert(played)
            db.watchHistoryDao().importAll(listOf(entry(100).copy(isMusic = true)))
            assertThat(db.watchHistoryDao().getEntry("video").first()).isEqualTo(played)
        }

    @Test fun `overlapping archives refresh only unknown progress and retain music classification`() =
        runBlocking {
            val dao = db.watchHistoryDao()
            dao.importAll(listOf(entry(50), entry(100).copy(isMusic = true)))
            dao.importAll(listOf(entry(10)))
            val restored = dao.getEntry("video").first()!!
            assertThat(restored.timestamp).isEqualTo(100)
            assertThat(restored.isMusic).isTrue()
            assertThat(restored.position).isEqualTo(0)
            assertThat(restored.duration).isEqualTo(0)
            assertThat(dao.getCount().first()).isEqualTo(1)
        }

    private fun entry(at: Long) = WatchHistoryEntity("video", 0, 0, at, "Video", "", "Artist", "", false)
}
