package info.bagen.dwebbrowser.database

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.LiveData
import androidx.room.*
import info.bagen.dwebbrowser.App
import io.ktor.util.date.*
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


internal const val WebSiteTableName = "website"
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
  var url: String,
  val type: WebSiteType,
  val timeMillis: Long = LocalDate.now().toEpochDay(),
  val icon: ImageBitmap? = null,
) {
  fun getStickyName() : String {
    val currentOfEpochDay = LocalDate.now().toEpochDay()
    return if (timeMillis >= currentOfEpochDay) {
      "今天"
    } else if (timeMillis == currentOfEpochDay - 1) {
      "昨天"
    } else {
      LocalDate.ofEpochDay(timeMillis).format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE"))
    }
  }
}

enum class WebSiteType(val id: Int) {
  History(0), Book(1), Multi(2)
  ;
}

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

  @Query("SELECT * FROM $WebSiteTableName WHERE type IN (:type) limit 500")
  fun loadAllByTypeObserve(type: WebSiteType): LiveData<List<WebSiteInfo>>

  @Query(
    "SELECT * FROM $WebSiteTableName WHERE title LIKE :name LIMIT 10"
  )
  fun findByNameTop10(name: String): LiveData<List<WebSiteInfo>>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  fun insertAll(vararg users: WebSiteInfo)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  fun insert(user: WebSiteInfo)

  @Update
  fun update(user: WebSiteInfo)

  @Delete
  fun delete(user: WebSiteInfo)
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
  fun byteArrayToImageBitmap(byteArray: ByteArray?) : ImageBitmap? {
    return byteArray?.let {
      if (it.isNotEmpty()) {
        BitmapFactory.decodeByteArray(it, 0, it.size).asImageBitmap()
      } else null
    }
  }

  @TypeConverter
  fun fromImageBitmap(imageBitmap: ImageBitmap?) : ByteArray? {
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
      Room.databaseBuilder(App.appContext, WebSiteDatabase::class.java, "dweb_database.db").build()
    }
  }
}
