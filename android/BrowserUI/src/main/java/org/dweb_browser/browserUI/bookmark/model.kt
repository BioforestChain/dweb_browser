package org.dweb_browser.browserUI.bookmark

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.dweb_browser.browserUI.database.Converters
import org.dweb_browser.browserUI.database.WebSiteType
import java.io.ByteArrayOutputStream
import java.util.Date

class BookmarkView {
  val bookList = mutableStateListOf<Bookmark>()
  var stopObserve = false;
}

val LocalBookmarkView =
  compositionLocalOf<BookmarkView> { throw Exception("No provider of Bookmark") }


@Entity(
  tableName = "bookmark"
)
data class Bookmark(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
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
  suspend fun add(bookmark: Bookmark)

  @Delete
  suspend fun remove(bookmark: Bookmark)
}

class Converters {

  @TypeConverter
  fun byteArrayToImageBitmap(byteArray: ByteArray?): ImageBitmap? {
    return byteArray?.let {
      if (it.isNotEmpty()) {
        BitmapFactory.decodeByteArray(it, 0, it.size).asImageBitmap()
      } else null
    }
  }

  @TypeConverter
  fun fromImageBitmap(imageBitmap: ImageBitmap?): ByteArray? {
    return imageBitmap?.asAndroidBitmap()?.let {
      val baos = ByteArrayOutputStream()
      it.compress(Bitmap.CompressFormat.PNG, 100, baos)
      baos.toByteArray()
    }
  }
}

@Database(entities = [Bookmark::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class BookmarkDatabase : RoomDatabase() {
  abstract fun bookmarkDao(): BookmarkDao
}