package info.bagen.libappmgr.service

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import info.bagen.libappmgr.data.PreferencesHelper
import info.bagen.libappmgr.database.MediaDBManager
import info.bagen.libappmgr.utils.FilesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class AppMgrService : Service() {
    private val TAG: String = AppMgrService::class.java.simpleName
    private var mMediaFileObserver: MediaFileObserver? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        initMediaInfo()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mMediaFileObserver?.stopWatching()
        }
    }

    private fun initMediaInfo() {
        GlobalScope.launch(Dispatchers.Default) {
            val arrayList = arrayListOf<File>()
            arrayList.add(Environment.getExternalStoragePublicDirectory("DCIM"))
            arrayList.add(Environment.getExternalStoragePublicDirectory("Pictures"))
            // 将图片数据保存到数据库
            Log.d(TAG, "initMediaInfo 获取文件列表信息")
            val start = Calendar.getInstance().timeInMillis
            val maps = FilesUtil.getMediaInfoList(arrayList)
            Log.d(TAG, "initMediaInfo 将文件转存到数据库")
            MediaDBManager.saveMediaInfoList(maps)
            val end = Calendar.getInstance().timeInMillis
            Log.d(TAG, "initMediaInfo 转存耗时=${end - start}ms")
            // 打开文件目录监听
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mMediaFileObserver = MediaFileObserver(arrayList)
                mMediaFileObserver?.startWatching()
            }
            PreferencesHelper.saveMediaLoading(true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    class MediaFileObserver : FileObserver {
        val TAG = MediaFileObserver::class.java.simpleName
        var mObservers: ArrayList<SingleFileObserver> = arrayListOf()
        var mFileList: ArrayList<File> = arrayListOf()
        var mMask = 0

        var mThreadName: String = MediaFileObserver::class.java.simpleName
        var mHandlerThread: HandlerThread? = null
        var mHandler: Handler? = null

        companion object {
            private val CREATE_DIRECTORY = 1073742080
            private val DELETE_DIRECTORY = 1073742336
        }

        constructor(file: File) : this(file, CREATE or DELETE or MODIFY)

        constructor(file: File, mask: Int) : super(file, mask) {
            mFileList.add(file)
            mMask = mask
        }

        constructor(fileList: MutableList<File>) : this(fileList, CREATE or DELETE or MODIFY)

        constructor(fileList: MutableList<File>, mask: Int) : super(fileList, mask) {
            mFileList.addAll(fileList)
            mMask = mask
        }

        override fun startWatching() {
            if (mHandlerThread == null || !mHandlerThread!!.isAlive) {
                mHandlerThread = HandlerThread(mThreadName)
                mHandlerThread!!.start()
                mHandler = Handler(mHandlerThread!!.looper)
                mHandler!!.post(Runnable { startRunnable() })
            }
        }

        override fun stopWatching() {
            if (null != mHandler && null != mHandlerThread && mHandlerThread!!.isAlive) {
                mHandler!!.post(Runnable { stopRunnable() })
            }
            mHandler = null
            mHandlerThread!!.quit()
            mHandlerThread = null
        }

        override fun onEvent(event: Int, path: String?) {
            if (path == null) return
            mHandler?.post(Runnable {
                when (event) {
                    CREATE, CREATE_DIRECTORY -> {
                        doCreate(path)
                        if (event == CREATE) insertDatabase(path)
                    }
                    DELETE, DELETE_DIRECTORY -> {
                        doDelete(path)
                        if (event == DELETE) deleteDatabase(path)
                    }
                    MODIFY -> {
                        updateDatabase(path)
                    }
                    else -> {
                    }
                }
            })
        }

        private fun startRunnable() {
            synchronized(this@MediaFileObserver) {
                mFileList.forEach { file ->
                    // 1. 判断当前的file是否是目录，如果是目录，加入到监听队列
                    if (file.isDirectory) {
                        mObservers.add(SingleFileObserver(file, mMask, this@MediaFileObserver))
                        // 2. 遍历目录的级联子目录是否有目录，如果有，也一并加入到监听队列
                        file.walk().iterator().forEach { subFile ->
                            if (subFile.isDirectory && !subFile.name.startsWith(".")) {
                                mObservers.add(
                                    SingleFileObserver(
                                        subFile,
                                        mMask,
                                        this@MediaFileObserver
                                    )
                                )
                            }
                        }
                    }
                }
                mObservers.forEach { fileObserver ->
                    fileObserver.startWatching()
                }
            }
        }

        private fun stopRunnable() {
            synchronized(this@MediaFileObserver) {
                if (mObservers.isEmpty()) return
                mObservers.forEach { fileObserver ->
                    fileObserver.stopWatching()
                }
                mObservers.clear()
            }
        }

        private fun doCreate(path: String) {
            synchronized(this@MediaFileObserver) {
                var file = File(path)
                if (file.isDirectory && !file.startsWith(".")) {
                    // 新建文件夹，对该文件夹及子目录添加监听
                    var sfo = SingleFileObserver(file, mMask, this@MediaFileObserver)
                    mObservers.add(sfo)
                    sfo.startWatching()
                }
            }
        }

        private fun doDelete(path: String) {
            synchronized(this@MediaFileObserver) {
                mObservers.forEach { sfo ->
                    if (sfo.mFile.absolutePath == path) {
                        sfo.stopWatching()
                        mObservers.remove(sfo)
                        return
                    }
                }
            }
        }

        private fun updateDatabase(path: String) {
            val file = File(path)
            if (file.exists() && file.isFile) {
                // 更新当前文件
                MediaDBManager.updateMediaInfoByPath(path)
            }
        }

        private fun insertDatabase(path: String) {
            val file = File(path)
            if (file.exists() && file.isFile) {
                // 保存当前文件
                MediaDBManager.insertMediaInfoByPath(path)
            }
        }

        private fun deleteDatabase(path: String) {
            MediaDBManager.deleteMediaInfoByPath(path)
        }

        class SingleFileObserver : FileObserver {
            var mFile: File
            private var mfo: MediaFileObserver

            constructor(file: File, mask: Int, mfo: MediaFileObserver) : super(file, mask) {
                mFile = file
                this.mfo = mfo
            }

            override fun onEvent(event: Int, path: String?) {
                if (path == null) {
                    return
                }
                mfo.onEvent(event, "${mFile.absolutePath}/$path")
            }
        }
    }
}
