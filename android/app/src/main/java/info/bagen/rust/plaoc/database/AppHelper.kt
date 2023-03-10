package info.bagen.rust.plaoc.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppHelper(context: Context, db_name: String, db_version: Int) :
  SQLiteOpenHelper(context, db_name, null, db_version) {
  override fun onCreate(db: SQLiteDatabase?) {
    db?.execSQL(createPermissionTable())
    createMediaTable().forEach { sql -> db?.execSQL(sql) }
    db?.execSQL(createAppInfoTable())
  }

  override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    when(oldVersion) {
      1 -> {
        if (newVersion == 2) createMediaTable().forEach { sql -> db?.execSQL(sql) }
        if (newVersion == 3) db?.execSQL(createAppInfoTable())
      }
      2 -> db?.execSQL(createAppInfoTable())
    }
  }

  private fun createPermissionTable(): String {
    return "CREATE TABLE IF NOT EXISTS " + AppProvider.PERMISSIONS_TABLE + "(" +
        AppContract.Permissions.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        AppContract.Permissions.COLUMN_APP_ID + " TEXT NOT NULL," +
        AppContract.Permissions.COLUMN_NAME + " TEXT NOT NULL," +
        AppContract.Permissions.COLUMN_GRANT + " INT NOT NULL DEFAULT -1 );"
  }

  private fun createMediaTable(): ArrayList<String> {
    var list = arrayListOf<String>()
    list.add(
      "CREATE TABLE IF NOT EXISTS " + AppProvider.MEDIAS_TABLE + "(" +
          AppContract.Medias.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
          AppContract.Medias.COLUMN_PATH + " TEXT NOT NULL," +
          AppContract.Medias.COLUMN_FILTER + " TEXT NOT NULL," +
          AppContract.Medias.COLUMN_TYPE + " TEXT NOT NULL," +
          AppContract.Medias.COLUMN_DURATION + " INT NOT NULL DEFAULT 0," +
          AppContract.Medias.COLUMN_TIME + " INT NOT NULL DEFAULT 0," +
          AppContract.Medias.COLUMN_THUMBNAIL + " BLOB," +
          AppContract.Medias.COLUMN_BITMAP + " BLOB );"
    )
    list.add("CREATE INDEX IND_PATH ON " + AppProvider.MEDIAS_TABLE + "(${AppContract.Medias.COLUMN_PATH});")
    list.add("CREATE INDEX IND_FILTER ON " + AppProvider.MEDIAS_TABLE + "(${AppContract.Medias.COLUMN_FILTER});")
    list.add("CREATE INDEX IND_TYPE ON " + AppProvider.MEDIAS_TABLE + "(${AppContract.Medias.COLUMN_TYPE});")
    return list
  }

  private fun createAppInfoTable(): String {
    return "CREATE TABLE IF NOT EXISTS " + AppProvider.APPINFO_TABLE + "(" +
        AppContract.AppInfo.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
        AppContract.AppInfo.COLUMN_MMID + " TEXT NOT NULL," +
        AppContract.AppInfo.COLUMN_MAIN_URL + " TEXT NOT NULL," +
        AppContract.AppInfo.COLUMN_META_DATA + " BLOB );"
  }
}