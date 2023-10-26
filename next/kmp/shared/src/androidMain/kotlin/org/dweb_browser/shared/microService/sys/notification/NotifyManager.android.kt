package org.dweb_browser.shared.microService.sys.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.dweb_browser.browser.web.ui.view.findActivity
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.shared.microService.sys.deepLink.DWebReceiver

actual class NotifyManager actual constructor() {

    private var intentUrl: String? = null
    enum class ChannelType(
        val typeName: String, val channelID: String, val property: Int, val importance: Int
    ) {
        // 默认消息，只需要任务栏有显示即可
        DEFAULT(
            typeName = "默认通知",
            channelID = "plaoc_cid_default",
            property = NotificationCompat.PRIORITY_DEFAULT,
            importance = NotificationManager.IMPORTANCE_DEFAULT,
        ),

        // 紧急消息，需要及时弹出提示
        IMPORTANT(
            typeName = "紧急通知",
            channelID = "plaoc_cid_high",
            property = NotificationCompat.PRIORITY_MAX,
            importance = NotificationManager.IMPORTANCE_HIGH,
        ),
    }

    val appName: String
        get() {
            val packageManager = context.packageManager
            var applicationInfo: ApplicationInfo? = null
            try {
                applicationInfo =
                    packageManager.getApplicationInfo(context.applicationInfo.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
            }

            return (if (applicationInfo != null) packageManager.getApplicationLabel(applicationInfo) else "unknown") as String
        }

    companion object {
        private const val TAG: String = "NotifyManager"
        val sInstance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            NotifyManager()
        }
        var notifyId = 100 // 消息id，如果notify的id相同时，相当于修改而不是新增
        var requestCode = 1 // 用于通知栏点击时，避免都是点击到最后一个
        val context get() = NativeMicroModule.getAppContext()

        fun getDefaultPendingIntent(): PendingIntent {
            //TODO  android 是写DesktopActivity 这里要获取相关Activity
            val intent = Intent(context, NativeMicroModule.getAppContext().findActivity()::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            return PendingIntent.getActivity(
                context,
                ++requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        /**
         * 打开DWeb需要通过调用broadcast后，由receiver打开
         */
        fun createBroadcastPendingIntent(appName: String, url: String): PendingIntent {
            var intent = Intent(DWebReceiver.ACTION_OPEN_DWEB).apply {
                putExtra("AppName", appName)
                putExtra("URL", url)
                `package` = NativeMicroModule.getAppContext().packageName
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            return PendingIntent.getBroadcast(
                context,
                ++requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    actual fun createNotification(
        title: String,
        subTitle: String?,
        text: String,
        androidIcon: Int?,
        iosIcon: String?,
        bigText: String?,
        intentUrl: String?,
        commonChannelType: CommonChannelType,
        intentType: PendingIntentType
    ) {
        val channelType = changeCommonChannelToChannelType(commonChannelType)
        val intent = pendingIntent(intentType)
        this.intentUrl = intentUrl
        val builder =
            NotificationCompat.Builder(context, channelType.channelID).setContentTitle(title) // 通知标题
                .setContentText(text) // 通知内容
                .setSmallIcon(androidIcon ?: 0) // 设置通知的小图标
                //.setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)) // 设置通知的大图标
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(bigText)
                ).setContentIntent(intent).setPriority(channelType.property) // 设置通知的优先级
                .setAutoCancel(true) // 设置点击通知之后通知是否消失
                .setWhen(System.currentTimeMillis()) // 设定通知显示的时间
                .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // 创建渠道，针对不同类别的消息进行独立管理
            val channel = NotificationChannel(
                channelType.channelID, channelType.typeName, channelType.importance
            )
            // channel.setBypassDnd(true) // 是否绕过勿打扰模式
            // channel.enableLights(true) // 是否允许呼吸灯闪烁
            // channel.lightColor = Color.RED // 闪关灯的灯光颜色
//      channel.canShowBadge() // 桌面launcher的消息角标
            // channel.enableVibration(true) // 是否允许震动
            // channel.vibrationPattern = LongArray(3) { 1000L; 500L; 2000L } // //先震动1秒，然后停止0.5秒，再震动2秒则可设置数组为：new long[]{1000, 500, 2000}

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: 权限申请
                return
            }
            notify(notifyId++, builder.build())
        }
    }

    private fun changeCommonChannelToChannelType(type: CommonChannelType): ChannelType {

        var channelType = when(type) {
            CommonChannelType.DEFAULT -> ChannelType.DEFAULT
            CommonChannelType.IMPORTANT -> ChannelType.IMPORTANT
        }
        return channelType
    }

    private fun pendingIntent(type: PendingIntentType): PendingIntent {
        //TODO 获取appName url
        var intent = when(type) {
            PendingIntentType.DEFAULT -> getDefaultPendingIntent()
            PendingIntentType.BROADCAST -> createBroadcastPendingIntent(appName,this.intentUrl ?: "")
        }
        return intent
    }

}