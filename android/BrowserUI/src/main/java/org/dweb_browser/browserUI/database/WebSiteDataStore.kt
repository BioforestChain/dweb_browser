package org.dweb_browser.browserUI.database

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import org.dweb_browser.browserUI.R
import org.dweb_browser.browserUI.util.BrowserUIApp
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

internal const val WebSiteTableName = "website"
internal const val WebSiteFileName = "dweb_database.db"

/**
 * 定义数据实体
 */
@Entity(
  tableName = WebSiteTableName,
  indices = [Index(value = ["type"]), Index(value = ["timeMillis"]), Index(value = ["title"])]
)
data class WebSiteInfo(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  var title: String,
//  var parent: Int = 0,
  var url: String = "",
  val type: WebSiteType,
  val timeMillis: Long = LocalDate.now().toEpochDay(),
  val icon: ImageBitmap? = null,
//  val time: Long = System.currentTimeMillis(),
) {
  fun getStickyName(): String {
    val currentOfEpochDay = LocalDate.now().toEpochDay()
    return if (timeMillis >= currentOfEpochDay) {
      BrowserUIApp.Instance.appContext.getString(R.string.browser_history_today)
    } else if (timeMillis == currentOfEpochDay - 1) {
      BrowserUIApp.Instance.appContext.getString(R.string.browser_history_yesterday)
    } else {
      LocalDate.ofEpochDay(timeMillis).format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE"))
    }
  }
}

enum class WebSiteType(val id: Int) {
  History(0), Book(1), Multi(2)
  ;
}

enum class OrderBy { Desc, Asc; }

/**
 * 定义数据访问对象
 */
@Dao
interface WebSiteDao {
  @Query("SELECT * FROM $WebSiteTableName")
  fun getAll(): List<WebSiteInfo>

  @Query("SELECT * FROM $WebSiteTableName")
  fun getAllObserve(): LiveData<List<WebSiteInfo>>

  @Query("SELECT * FROM $WebSiteTableName WHERE type IN (:type)")
  fun loadAllByType(type: WebSiteType): List<WebSiteInfo>

  @Query("SELECT * FROM $WebSiteTableName WHERE type IN (:type) order by timeMillis ASC, id ASC limit 500")
  fun loadAllByTypeAscObserve(type: WebSiteType): LiveData<List<WebSiteInfo>>

  @Query("SELECT * FROM $WebSiteTableName WHERE type IN (:type) order by timeMillis DESC, id DESC limit 500")
  fun loadAllByTypeDescObserve(type: WebSiteType): LiveData<List<WebSiteInfo>>

  @Query("SELECT * FROM $WebSiteTableName WHERE title LIKE :name LIMIT 10")
  fun findByNameTop10(name: String): LiveData<List<WebSiteInfo>>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  fun insertAll(vararg users: WebSiteInfo)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  fun insert(user: WebSiteInfo)

  @Update
  fun update(user: WebSiteInfo)

  @Delete
  fun delete(user: WebSiteInfo)

  @Query(
    "DELETE FROM $WebSiteTableName WHERE type = :type"
  )
  fun clearByType(type: WebSiteType): Int
}

class Converters {
  @TypeConverter
  fun fromTimestamp(value: Long?): Date? {
    return value?.let { Date(it) }
  }

  @TypeConverter
  fun dateToTimestamp(date: Date?): Long? {
    return date?.time
  }

  @TypeConverter
  fun fromType(type: WebSiteType): String {
    return type.name
  }

  @TypeConverter
  fun toType(value: String): WebSiteType {
    return WebSiteType.valueOf(value)
  }

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

/**
 * 定义用户保存数据库的类
 */
@Database(entities = [WebSiteInfo::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class WebSiteDatabase : RoomDatabase() {
  abstract fun websiteDao(): WebSiteDao

  companion object {
    val INSTANCE by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
      Room.databaseBuilder(
        BrowserUIApp.Instance.appContext,
        WebSiteDatabase::class.java,
        WebSiteFileName
      ).build()
    }
  }
}
