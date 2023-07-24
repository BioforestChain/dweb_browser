package org.dweb_browser.browserUI.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.dweb_browser.browserUI.R
import org.dweb_browser.helper.Mmid

class NotificationUtil {
  private val mNormalChannelId = "渠道id" // 唯一性
  private val mNormalChannelName = "普通通知"

  private val mHighChannelId = "重要渠道id"
  private val mHighChannelName = "重要通知"

  private val mProgressChannelId = "进度条渠道id"
  private val mProgressChannelName = "进度条通知"

  private val mBigTextChannelId = "大文本渠道id"
  private val mBigTextChannelName = "大文本通知"

  private val mBigImageChannelId = "大图片渠道id"
  private val mBigImageChannelName = "大图片通知"

  private val mCustomChannelId = "自定义渠道id"
  private val mCustomChannelName = "自定义通知"

  // private var mReceiver: NotificationReceiver? = null

  private lateinit var mBuilder: NotificationCompat.Builder

  companion object {
    private lateinit var mManager: NotificationManager
    private const val mNormalNotificationId = 9001 // 通知id
    private const val mHighNotificationId = 9002 // 通知id
    private const val mBigTextNotificationId = 9003 // 通知id
    private const val mProgressNotificationId = 9004 // 通知id
    private const val mBigImageNotificationId = 9005 // 通知id
    private const val mCustomNotificationId = 9006 // 通知id
    private const val mStopAction = "info.bagen.notification.stop" // 暂停继续action
    private const val mDoneAction = "info.bagen.notification.done" // 完成action
    private var mFlag = 0
    private var mIsStop = false // 是否在播放 默认未开始
    var notificationId = 5000

    val INSTANCE: NotificationUtil by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
      NotificationUtil()
    }
  }

  private constructor() {
    mManager = BrowserUIApp.Instance.appContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    // createReceiver() // 自定义通知时更新会用到
  }

  /**
   * 普通通知
   */
  fun createNotificationForNormal(pendingIntent: (() -> PendingIntent)? = null) {
    // 适配8.0及以上 创建渠道
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        mNormalChannelId, mNormalChannelName, NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "描述"
        setShowBadge(false) // 是否在桌面显示角标
      }
      mManager.createNotificationChannel(channel)
    }
    // 点击意图 // setDeleteIntent 移除意图
    /*val intent = Intent(BrowserUIApp.Instance.appContext, BrowserActivity::class.java)
    val pendingIntent =
      PendingIntent.getActivity(BrowserUIApp.Instance.appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)*/
    // 构建配置
    mBuilder =
      NotificationCompat.Builder(BrowserUIApp.Instance.appContext, mNormalChannelId).setContentTitle("普通通知") // 标题
        .setContentText("普通通知内容") // 文本
        .setSmallIcon(R.mipmap.ic_launcher) // 小图标
        //.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_avatar)) // 大图标
        .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 7.0 设置优先级
        //.setContentIntent(pendingIntent) //跳转配置
        .setAutoCancel(true) // 是否自动消失（点击）or mManager.cancel(mNormalNotificationId)、cancelAll、setTimeoutAfter()
    pendingIntent?.let { mBuilder.setContentIntent(it()) /*跳转配置*/ }
    // 发起通知
    mManager.notify(mNormalNotificationId, mBuilder.build())
  }

  /**
   * 重要通知
   */
  fun createNotificationForHigh(pendingIntent: (() -> PendingIntent)? = null) {
    /*val intent = Intent(BrowserUIApp.Instance.appContext, BrowserActivity::class.java)
    val pendingIntent =
      PendingIntent.getActivity(BrowserUIApp.Instance.appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)*/
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        mHighChannelId, mHighChannelName, NotificationManager.IMPORTANCE_HIGH
      )
      channel.setShowBadge(true)
      mManager.createNotificationChannel(channel)
    }
    mBuilder = NotificationCompat.Builder(BrowserUIApp.Instance.appContext, mHighChannelId).setContentTitle("重要通知")
      .setContentText("重要通知内容").setSmallIcon(R.mipmap.ic_launcher)
      //.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_avatar))
      .setAutoCancel(true).setNumber(999) // 自定义桌面通知数量
      //.addAction(R.mipmap.ic_launcher, "去看看", pendingIntent)// 通知上的操作
      .setCategory(NotificationCompat.CATEGORY_MESSAGE) // 通知类别，"勿扰模式"时系统会决定要不要显示你的通知
      .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) // 屏幕可见性，锁屏时，显示icon和标题，内容隐藏
    pendingIntent?.let { mBuilder.addAction(R.mipmap.ic_launcher, "去看看", it()) /*通知上的操作*/ }
    mManager.notify(mHighNotificationId, mBuilder.build())
  }

  /**
   * 进度条通知
   */
  fun createNotificationForProgress(
    mmid: Mmid,
    notificationId: Int = mProgressNotificationId,
    title: String = "进度通知",
    text: String = "下载中"
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        mProgressChannelId, mProgressChannelName, NotificationManager.IMPORTANCE_LOW
      )
      mManager.createNotificationChannel(channel)
    }
    val progressMax = 100
    val progressCurrent = 0

    // 适配12.0及以上
    /*mFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val intent = Intent(App.appContext, BFSBroadcastReceiver::class.java).apply {
      action = BFSBroadcastAction.DownLoadStatusChanged.action
      putExtra("mmid", mmid)
    }
    val pendingIntent = PendingIntent.getBroadcast(App.appContext, 999, intent, mFlag)*/

    mBuilder = NotificationCompat.Builder(BrowserUIApp.Instance.appContext, mProgressChannelId).setContentTitle(title)
      .setContentText("$text：$progressCurrent%").setSmallIcon(R.mipmap.ic_launcher)
      // .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_avatar))
      // 第3个参数indeterminate，false表示确定的进度，比如100，true表示不确定的进度，会一直显示进度动画，直到更新状态下载完成，或删除通知
      .setProgress(progressMax, progressCurrent, false)
    //.setContentIntent(pendingIntent)

    mManager.notify(notificationId, mBuilder.build())
  }

  /**
   * 更新进度条通知
   * 1.更新进度：修改进度值即可
   * 2.下载完成：总进度与当前进度都设置为0即可，同时更新文案
   */
  fun updateNotificationForProgress(
    progress: Int, notificationId: Int = mProgressNotificationId, text: String = "下载中",
    pendingIntent: (() -> PendingIntent)? = null
  ) {
    if (::mBuilder.isInitialized) {
      val progressMax = 100
      // 1.更新进度
      mBuilder.setContentText("$text：$progress%").setProgress(progressMax, progress, false)
      // 2.下载完成
      //mBuilder.setContentText("下载完成！").setProgress(0, 0, false)
      // 3.如果是下载完成，跳转到下载
      if (progress == 100 && pendingIntent != null) {
        mBuilder.setContentIntent(pendingIntent())
      }
      mManager.notify(notificationId, mBuilder.build())
      // Toast.makeText(App.appContext, "已更新进度到$progress%", Toast.LENGTH_SHORT).show()
    } else {
      // Toast.makeText(App.appContext, "请先发一条进度条通知", Toast.LENGTH_SHORT).show()
    }
  }

  /**
   * 大文本通知
   */
  fun createNotificationForBigText() {
    val bigText =
      "A notification is a message that Android displays outside your app's UI to provide the user with reminders, communication from other people, or other timely information from your app. Users can tap the notification to open your app or take an action directly from the notification."
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        mBigTextChannelId, mBigTextChannelName, NotificationManager.IMPORTANCE_DEFAULT
      )
      mManager.createNotificationChannel(channel)
    }
    mBuilder =
      NotificationCompat.Builder(BrowserUIApp.Instance.appContext, mBigTextChannelId).setContentTitle("大文本通知")
        .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        .setSmallIcon(R.mipmap.ic_launcher)
        //.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_avatar))
        .setAutoCancel(true)
    mManager.notify(mBigTextNotificationId, mBuilder.build())
  }

  /**
   * 大图片通知
   */
  /*fun createNotificationForBigImage() {
    val bigPic = BitmapFactory.decodeResource(resources, R.drawable.ic_big_pic)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(mBigImageChannelId, mBigImageChannelName, NotificationManager.IMPORTANCE_DEFAULT)
      mManager.createNotificationChannel(channel)
    }
    mBuilder = NotificationCompat.Builder(this@NotificationUtil, mBigImageChannelId)
      .setContentTitle("大图片通知")
      .setContentText("有美女，展开看看")
      .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bigPic))
      .setSmallIcon(R.mipmap.ic_launcher)
      .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_avatar))
      .setAutoCancel(true)
    mManager.notify(mBigImageNotificationId, mBuilder.build())
  }*/

  fun cancelNotification(notificationId: Int) {
    mManager.cancel(notificationId)
  }

  fun cancelAll() {
    mManager.cancelAll()
  }

  /**
   * 自定义通知
   */
  /*fun createNotificationForCustom() {
    // 适配8.0及以上
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(mCustomChannelId, mCustomChannelName, NotificationManager.IMPORTANCE_DEFAULT)
      mManager.createNotificationChannel(channel)
    }

    // 适配12.0及以上
    mFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    // 添加自定义通知view
    val views = RemoteViews(packageName, R.layout.layout_notification)

    // 添加暂停继续事件
    val intentStop = Intent(mStopAction)
    val pendingIntentStop = PendingIntent.getBroadcast(this@NotificationUtil, 0, intentStop, mFlag)
    views.setOnClickPendingIntent(R.id.btn_stop, pendingIntentStop)

    // 添加完成事件
    val intentDone = Intent(mDoneAction)
    val pendingIntentDone = PendingIntent.getBroadcast(this@NotificationUtil, 0, intentDone, mFlag)
    views.setOnClickPendingIntent(R.id.btn_done, pendingIntentDone)

    // 创建Builder
    mBuilder = NotificationCompat.Builder(this@NotificationUtil, mCustomChannelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_avatar))
      .setAutoCancel(true)
      .setCustomContentView(views)
      .setCustomBigContentView(views)// 设置自定义通知view

    // 发起通知
    mManager.notify(mCustomNotificationId, mBuilder.build())
  }*/

  /**
   * 创建广播接收器
   */
  /*fun createReceiver() {
    val intentFilter = IntentFilter()
    // 添加接收事件监听
    intentFilter.addAction(mStopAction)
    intentFilter.addAction(mDoneAction)
    mReceiver = NotificationReceiver()
    // 注册广播
    App.appContext.registerReceiver(mReceiver, intentFilter)
  }
*/
  /*private class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      // 拦截接收事件
      if (intent.action == mStopAction) {
        // 改变状态
        mIsStop = !mIsStop
        sInstance.updateCustomView()
      } else if (intent.action == mDoneAction) {
        Toast.makeText(context, "完成", Toast.LENGTH_SHORT).show()
      }
    }
  }*/

  /**
   * 更新自定义通知
   */
  /*private fun updateNotificationForCustom() {
    // 发送通知 更新状态及UI
    App.appContext.sendBroadcast(Intent(mStopAction))
  }*/

  /**
   * 更新自定义通知View
   */
  /*private fun updateCustomView() {
    val views = RemoteViews(packageName, R.layout.layout_notification)
    val intentUpdate = Intent(mStopAction)
    val pendingIntentUpdate = PendingIntent.getBroadcast(this, 0, intentUpdate, mFlag)
    views.setOnClickPendingIntent(R.id.btn_stop, pendingIntentUpdate)
    // 根据状态更新UI
    if (mIsStop) {
      views.setTextViewText(R.id.tv_status, "那些你很冒险的梦-停止播放")
      views.setTextViewText(R.id.btn_stop, "继续")
      mBinding.mbUpdateCustom.text = "继续"
    } else {
      views.setTextViewText(R.id.tv_status, "那些你很冒险的梦-正在播放")
      views.setTextViewText(R.id.btn_stop, "暂停")
      mBinding.mbUpdateCustom.text = "暂停"
    }

    mBuilder.setCustomContentView(views).setCustomBigContentView(views)
    // 重新发起通知更新UI，注意：必须得是同一个通知id，即mCustomNotificationId
    mManager.notify(mCustomNotificationId, mBuilder.build())
  }

  override fun onDestroy() {
    super.onDestroy()
    // 取消注册
    mReceiver?.let { unregisterReceiver(it) }
  }*/
}