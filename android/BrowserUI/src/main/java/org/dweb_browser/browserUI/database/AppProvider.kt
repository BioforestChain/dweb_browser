package org.dweb_browser.browserUI.database

import android.content.*
import android.database.Cursor
import android.database.CursorWindow
import android.net.Uri
import info.bagen.dwebbrowser.App
import org.dweb_browser.browserUI.database.AppContract.Companion.PATH_APP_INFO
import org.dweb_browser.browserUI.database.AppContract.Companion.PATH_MEDIA
import org.dweb_browser.browserUI.database.AppContract.Companion.PATH_PERMISSION
import java.lang.reflect.Field

class AppProvider : ContentProvider() {
  companion object {
    private val DATABASE_NAME = "plaoc.db"
    val DATABASE_VERSION = 3
    val PERMISSIONS_TABLE = "permissions" // 用于存储应用是否含有权限
    val MEDIAS_TABLE = "medias" // 用于存储照片和视频信息
    val APPINFO_TABLE = "appinfo" // 用于存储下载的应用信息
  }

  private var mOpenHelper: AppHelper =
    AppHelper(App.appContext, DATABASE_NAME, DATABASE_VERSION)
  private val sUriMatcher: UriMatcher = UriMatcher(UriMatcher.NO_MATCH)
  private val MATCH_PERMISSION = 1
  private val MATCH_PERMISSION_ID = 2
  private val MATCH_MDEIA = 3
  private val MATCH_MDEIA_ID = 4
  private val MATCH_APP_INFO = 5
  private val MATCH_APP_INFO_ID = 6

  private val sProjectionMap: HashMap<String, String> = hashMapOf()

  init {
    sUriMatcher.addURI(AppContract.AUTHORITY, PATH_PERMISSION, MATCH_PERMISSION)
    sUriMatcher.addURI(AppContract.AUTHORITY, "$PATH_PERMISSION/#", MATCH_PERMISSION_ID)
    sUriMatcher.addURI(AppContract.AUTHORITY, PATH_MEDIA, MATCH_MDEIA)
    sUriMatcher.addURI(AppContract.AUTHORITY, "$PATH_MEDIA/#", MATCH_MDEIA_ID)
    sUriMatcher.addURI(AppContract.AUTHORITY, PATH_APP_INFO, MATCH_APP_INFO)
    sUriMatcher.addURI(AppContract.AUTHORITY, "$PATH_APP_INFO/#", MATCH_APP_INFO_ID)

    sProjectionMap[AppContract.Permissions.COLUMN_ID] =
      PERMISSIONS_TABLE + "." + AppContract.Permissions.COLUMN_ID
    sProjectionMap[AppContract.Permissions.COLUMN_APP_ID] =
      PERMISSIONS_TABLE + "." + AppContract.Permissions.COLUMN_APP_ID
    sProjectionMap[AppContract.Permissions.COLUMN_NAME] =
      PERMISSIONS_TABLE + "." + AppContract.Permissions.COLUMN_NAME
    sProjectionMap[AppContract.Permissions.COLUMN_GRANT] =
      PERMISSIONS_TABLE + "." + AppContract.Permissions.COLUMN_GRANT

    sProjectionMap[AppContract.Medias.COLUMN_ID] =
      PERMISSIONS_TABLE + "." + AppContract.Medias.COLUMN_ID
    sProjectionMap[AppContract.Medias.COLUMN_PATH] =
      PERMISSIONS_TABLE + "." + AppContract.Medias.COLUMN_PATH
    sProjectionMap[AppContract.Medias.COLUMN_TYPE] =
      PERMISSIONS_TABLE + "." + AppContract.Medias.COLUMN_TYPE
    sProjectionMap[AppContract.Medias.COLUMN_DURATION] =
      PERMISSIONS_TABLE + "." + AppContract.Medias.COLUMN_DURATION
    sProjectionMap[AppContract.Medias.COLUMN_TIME] =
      PERMISSIONS_TABLE + "." + AppContract.Medias.COLUMN_TIME
  }


  override fun onCreate(): Boolean {
    try {
      val field: Field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
      field.isAccessible = true
      val mb = 2
      field.set(null, mb * 1024 * 1024)
    } catch (e: Throwable) {
    }
    return true
  }

  override fun getType(uri: Uri): String? {
    when (sUriMatcher.match(uri)) {
      MATCH_PERMISSION -> AppContract.Permissions.CONTENT_TYPE
      MATCH_PERMISSION_ID -> AppContract.Permissions.CONTENT_ITEM_TYPE
      MATCH_MDEIA -> AppContract.Medias.CONTENT_TYPE
      MATCH_MDEIA_ID -> AppContract.Medias.CONTENT_ITEM_TYPE
      MATCH_APP_INFO -> AppContract.AppInfo.CONTENT_TYPE
      MATCH_APP_INFO_ID -> AppContract.AppInfo.CONTENT_ITEM_TYPE
      else ->
        throw IllegalArgumentException("Unknown URI " + uri);
    }
    return null
  }

  private inline fun <T> getTableName(uri: Uri, call: (String?, String?) -> T) : T {
    return when (sUriMatcher.match(uri)) {
      MATCH_PERMISSION -> call(PERMISSIONS_TABLE, PATH_PERMISSION)
      MATCH_MDEIA -> call(MEDIAS_TABLE, PATH_MEDIA)
      MATCH_APP_INFO -> call(APPINFO_TABLE, PATH_APP_INFO)
      else -> call(null, null)
    }
  }
  
  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    selection: String?,
    selectionArgs: Array<out String>?,
    sortOrder: String?
  ): Cursor? {
    return getTableName(uri) { tableName, _ ->
      tableName?.let {
        mOpenHelper.writableDatabase.query(
          tableName, projection, selection, selectionArgs, null, null, sortOrder
        )
      }
    }
  }

  override fun insert(uri: Uri, values: ContentValues?): Uri? {
    return getTableName(uri) { tableName, pathName ->
      tableName?.let {
        val id = mOpenHelper.writableDatabase.insert(tableName, null, values)
        if (id > 0) {
          Uri.parse("content://${AppContract.AUTHORITY}/${pathName!!}/$id")
        } else {
          null
        }
      }
    }
  }

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
    return getTableName(uri) { tableName, _ ->
      tableName?.let {
        mOpenHelper.writableDatabase.delete(tableName, selection, selectionArgs)
      } ?: 0
    }
  }

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<out String>?
  ): Int {
    return getTableName(uri) { tableName, _ ->
      tableName?.let {
        mOpenHelper.writableDatabase.update(it, values, selection, selectionArgs)
      } ?: 0
    }
  }

  override fun applyBatch(
    authority: String,
    operations: java.util.ArrayList<ContentProviderOperation>
  ): Array<ContentProviderResult> {
    val db = mOpenHelper.writableDatabase
    db.beginTransaction();
    try {
      val results = super.applyBatch(operations);
      db.setTransactionSuccessful();
      return results;
    } finally {
      db.endTransaction()
    }
    return super.applyBatch(authority, operations)
  }
}
