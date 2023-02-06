package info.bagen.rust.plaoc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import info.bagen.rust.plaoc.system.callable_map
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


val NOTIFICATION_TITLE: CharSequence = "WorkRequest Starting"
const val CHANNEL_ID = "VERBOSE_NOTIFICATION"
private const val TAG = "DENO_WORKER"
var TASK: Thread? = null
val threadPoolExecutor = ThreadPoolExecutor(
    5, 10, 60,
    TimeUnit.MINUTES,
    ArrayBlockingQueue<Runnable>(100),
    RejectedExecutionHandler { _, _ -> println("reject submit thread to thread pool") }
)


class DenoWorker(val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val funName = inputData.getString(WorkerNative.WorkerName.toString())
        val data = inputData.getString("WorkerData")
        Log.i(TAG, "WorkerName=$funName,WorkerData=$data")
        if (funName !== null) {
            val calFn = ExportNative.valueOf(funName)
            threadPoolExecutor.execute {
                callable_map[calFn]?.let { it ->
                    if (data != null) {
                        it(data)
                    }
                }
            }
        }
        return Result.success()
    }

    // fix issue https://github.com/BioforestChain/plaoc/issues/44
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                NOTIFICATION_TITLE,
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationManager.createNotificationChannel(channel)
        }

        val PENDING_INTENT_FLAG_MUTABLE =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentIntent(
                PendingIntent.getActivity(
                    appContext,
                    0,
                    Intent(appContext, MainActivity::class.java),
                    PENDING_INTENT_FLAG_MUTABLE
                )
            )
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentTitle(appContext.getString(R.string.app_name))
            .setLocalOnly(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        return ForegroundInfo(1337, notification)
    }
}

/** 创建后台线程worker，来运行Service*/
fun createWorker(funName: WorkerNative, data: String = "") {
    val fnName = funName.toString()
    val done = WorkManager.getInstance(App.appContext.applicationContext)
        .getWorkInfosByTag(fnName).isDone
//    Log.i("xx","workManager=> $done")
    if (done) {
        return
    }
    // 创建worker
    val denoWorkRequest: WorkRequest =
        OneTimeWorkRequestBuilder<DenoWorker>()
            .setInputData(
                Data.Builder()
                    .putString(WorkerNative.WorkerName.toString(), fnName) // 添加方法名
                    .putString(WorkerNative.WorkerData.toString(), data)  // 导入数据
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)// 加急服务，如果配额允许马上运行
            .addTag(funName.toString()) // 标记操作对象
            .build()

    // 推入处理队列
    WorkManager
        .getInstance(App.appContext)
        .enqueue(denoWorkRequest)
}

//  WorkManager.getInstance(App.appContext.applicationContext).enqueueUniquePeriodicWork(
//    fnName,
//    ExistingPeriodicWorkPolicy.KEEP, request
//  )
