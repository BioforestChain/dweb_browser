package org.dweb_browser.sys.permission

import android.Manifest
import android.os.Build
import org.dweb_browser.core.std.permission.AuthorizationStatus

actual object BuildinPermission {
  private val camera: RequestSystemPermission = {
    if (task.name == SystemPermissionName.CAMERA) {
      PermissionActivity.launchAndroidSystemPermissionRequester(
        microModule,
        AndroidPermissionTask(listOf(Manifest.permission.CAMERA), task.title, task.description)
      ).values.firstOrNull()
    } else {
      null
    }
  }

  private val microPhone: RequestSystemPermission = {
    if (task.name == SystemPermissionName.MICROPHONE) {
      PermissionActivity.launchAndroidSystemPermissionRequester(
        microModule,
        AndroidPermissionTask(
          listOf(Manifest.permission.RECORD_AUDIO),
          task.title,
          task.description
        )
      ).values.firstOrNull()
    } else {
      null
    }
  }

  private val clipboard: RequestSystemPermission = {
    if (task.name == SystemPermissionName.CLIPBOARD) {
      AuthorizationStatus.GRANTED
    } else {
      null
    }
  }

  private val contacts: RequestSystemPermission = {
    if (task.name == SystemPermissionName.CONTACTS) {
      PermissionActivity.launchAndroidSystemPermissionRequester(
        microModule, AndroidPermissionTask(
          listOf(Manifest.permission.READ_CONTACTS), task.title, task.description
        )
      ).values.firstOrNull()

    } else {
      null
    }
  }

  private val phone: RequestSystemPermission = {
    if(task.name == SystemPermissionName.PHONE) {
      PermissionActivity.launchAndroidSystemPermissionRequester(
        microModule, AndroidPermissionTask(
          listOf(Manifest.permission.READ_PHONE_STATE), task.title, task.description
        )
      ).values.firstOrNull()
    } else {
      null
    }
  }

  private val storage: RequestSystemPermission = {
    if(task.name == SystemPermissionName.STORAGE) {
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) { // Android 6.0 (API等级23) 到 Android 10 (API等级29):WRITE_EXTERNAL_STORAGE和READ_EXTERNAL_STORAGE是危险权限
        PermissionActivity.launchAndroidSystemPermissionRequester(
          microModule, AndroidPermissionTask(
            listOf(
              Manifest.permission.WRITE_EXTERNAL_STORAGE,
              Manifest.permission.READ_EXTERNAL_STORAGE
            ), task.title, task.description
          )
        ).values.firstOrNull()
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2
      ) { // Android 11 (API等级30) 及以上: WRITE_EXTERNAL_STORAGE标记为已废弃。READ_EXTERNAL_STORAGE仍是一个危险权限
        PermissionActivity.launchAndroidSystemPermissionRequester(
          microModule, AndroidPermissionTask(
            listOf(
              Manifest.permission.READ_EXTERNAL_STORAGE
            ), task.title, task.description
          )
        ).values.firstOrNull()
      } else {
        AuthorizationStatus.GRANTED
      }
    } else {
      null
    }
  }

  private val location : RequestSystemPermission = {
    if(task.name == SystemPermissionName.LOCATION) {
      PermissionActivity.launchAndroidSystemPermissionRequester(
        microModule,
        AndroidPermissionTask(
          permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
          ), title = task.title, description = task.description
        ),
      ).values.firstOrNull()
    } else {
      null
    }
  }

  private val notification: RequestSystemPermission = {
    if(task.name == SystemPermissionName.Notification) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PermissionActivity.launchAndroidSystemPermissionRequester(
          microModule, AndroidPermissionTask(
            listOf(Manifest.permission.POST_NOTIFICATIONS), task.title, task.description
          )
        ).values.firstOrNull()
      } else {
        AuthorizationStatus.GRANTED
      }
    } else {
      null
    }
  }

  private val fileChooser: RequestSystemPermission = {
    if(task.name == SystemPermissionName.FILE_CHOOSER) {
      AuthorizationStatus.GRANTED
    } else {
      null
    }
  }

  actual fun start() {
    SystemPermissionAdapterManager.append(adapter = camera)
    SystemPermissionAdapterManager.append(adapter = microPhone)
    SystemPermissionAdapterManager.append(adapter = clipboard)
    SystemPermissionAdapterManager.append(adapter = contacts)
    SystemPermissionAdapterManager.append(adapter = phone)
    SystemPermissionAdapterManager.append(adapter = storage)
    SystemPermissionAdapterManager.append(adapter = location)
    SystemPermissionAdapterManager.append(adapter = notification)
    SystemPermissionAdapterManager.append(adapter = fileChooser)
  }
}

