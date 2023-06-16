package org.dweb_browser.browserUI.bookmark

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

class BookmarkView {
  val bookList = mutableStateListOf<Bookmark>()
  var stopObserve = false;
}

val LocalBookmarkView = compositionLocalOf<BookmarkView> { throw Exception("No provider of Bookmark") }


@Entity(
  tableName = "bookmark"
)
data class Bookmark(
  @PrimaryKey val id: Int = 0,
  val title: String,
  val url: String,
  val icon: ImageBitmap? = null,
)


@Dao
interface BookmarkDao {
  @Query("SELECT * FROM bookmark")
  fun observeAll(): Flow<List<Bookmark>>

  @Upsert
  suspend fun upsert(bookmark: Bookmark)

  @Upsert
  suspend fun upsertAll(bookmarks: List<Bookmark>)


  @Insert
  suspend fun add(bookmark: Bookmark): Boolean

  @Delete
  suspend fun remove(bookmark: Bookmark): Boolean
}

@Database(entities = [Bookmark::class], version = 1, exportSchema = false)
abstract class BookmarkDatabase : RoomDatabase() {
  abstract fun bookmarkDao(): BookmarkDao
}