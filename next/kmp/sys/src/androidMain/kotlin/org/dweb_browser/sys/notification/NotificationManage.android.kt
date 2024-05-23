package org.dweb_browser.sys.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.sys.R

actual class NotificationManager {
  actual suspend fun createNotification(
    microModule: MicroModule.Runtime,
    message: NotificationWebItem
  ) {
    val channelType = if (message.renotify) ChannelType.IMPORTANT else ChannelType.DEFAULT
    createNewNotification(
      title = message.actions.firstOrNull()?.title ?: microModule.mmid,
      text = message.body,
      bigText = message.body,
      channelType = channelType,
      actionUrl = message.actions.firstOrNull()?.action ?: ""
    )
  }

  enum class ChannelType(
    val typeName: String, val channelID: String, val property: Int, val importance: Int
  ) {
    // 默认消息，只需要任务栏有显示即可
    DEFAULT(
      typeName = "默认通知",
      channelID = "dweb_cid_default",
      property = NotificationCompat.PRIORITY_DEFAULT,
      importance = NotificationManager.IMPORTANCE_DEFAULT,
    ),

    // 紧急消息，需要及时弹出提示
    IMPORTANT(
      typeName = "紧急通知",
      channelID = "dweb_cid_high",
      property = NotificationCompat.PRIORITY_MAX,
      importance = NotificationManager.IMPORTANCE_HIGH,
    ),
  }

  private val mContext = getAppContextUnsafe()
  private var notifyId = 100 // 消息id，如果notify的id相同时，相当于修改而不是新增
  private var requestCode = 1 // 用于通知栏点击时，避免都是点击到最后一个

  private fun getDefaultPendingIntent(actionUrl: String): PendingIntent {
    val intent = Intent().apply {
      action = Intent.ACTION_VIEW
      `package` = mContext.packageName
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      if (actionUrl.isNotEmpty()) this.data = Uri.parse(actionUrl)
    }
    return PendingIntent.getActivity(
      mContext,
      ++requestCode,
      intent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
  }

  // 用于创建一个消息通知
  @SuppressLint("MissingPermission")
  private fun createNewNotification(
    title: String,
    text: String,
    smallIcon: Int = R.mipmap.ic_launcher,
    bigText: String = "",
    channelType: ChannelType = ChannelType.DEFAULT,
    enabledVibrate: Boolean = false,
    vibratePattern: List<Long> = emptyList(),
    actionUrl: String = "",
    showBadge: Boolean = false,
    intent: PendingIntent = getDefaultPendingIntent(actionUrl),
  ) {
    // 创建渠道，针对不同类别的消息进行独立管理
    val channel = NotificationChannel(
      channelType.channelID, channelType.typeName, channelType.importance
    ).apply {
      setShowBadge(showBadge)
      // setBypassDnd(true) // 是否绕过勿打扰模式
      // enableLights(true) // 是否允许呼吸灯闪烁
      // lightColor = Color.RED // 闪关灯的灯光颜色
      // canShowBadge() // 桌面launcher的消息角标
      // enableVibration(true) // 是否允许震动
      // vibrationPattern = LongArray(3) { 1000L; 500L; 2000L } // //先震动1秒，然后停止0.5秒，再震动2秒则可设置数组为：new long[]{1000, 500, 2000}
      enableVibration(enabledVibrate)
      if (enabledVibrate) vibrationPattern = vibratePattern.toLongArray()
    }

    val notificationManager =
      mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)

    // 创建通知
    val builder = when (channelType) {
      ChannelType.DEFAULT -> {
        NotificationCompat
          .Builder(mContext, channelType.channelID)
          .setContentTitle(title) // 通知标题
          .setContentText(text) // 通知内容
          .setSmallIcon(smallIcon) // 设置通知的小图标
          //.setLargeIcon(BitmapFactory.decodeResource(mContext.resources, R.mipmap.ic_launcher)) // 设置通知的大图标
          .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
          .setContentIntent(intent)
          .setPriority(channelType.property) // 设置通知的优先级
          .setAutoCancel(true) // 设置点击通知之后通知是否消失
          .setWhen(System.currentTimeMillis()) // 设定通知显示的时间
          .setDefaults(NotificationCompat.DEFAULT_ALL)
      }

      ChannelType.IMPORTANT -> {
        NotificationCompat
          .Builder(mContext, channelType.channelID)
          .setContentTitle(title) // 通知标题
          .setContentText(text) // 通知内容
          .setSmallIcon(smallIcon) // 设置通知的小图标
          //.setLargeIcon(BitmapFactory.decodeResource(mContext.resources, R.mipmap.ic_launcher)) // 设置通知的大图标
          .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
          .setPriority(channelType.property) // 设置通知的优先级
          .setAutoCancel(true) // 设置点击通知之后通知是否消失
          .setWhen(System.currentTimeMillis()) // 设定通知显示的时间
          .setDefaults(NotificationCompat.DEFAULT_ALL)
          .addAction(R.mipmap.ic_launcher, "DwebView", intent) // 通知上的操作
          .setCategory(NotificationCompat.CATEGORY_MESSAGE) // 通知类别，"勿扰模式"时系统会决定要不要显示你的通知
          .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) // 屏幕可见性，锁屏时，显示icon和标题，内容隐藏
      }
    }

    NotificationManagerCompat.from(mContext).notify(notifyId++, builder.build())
  }
}