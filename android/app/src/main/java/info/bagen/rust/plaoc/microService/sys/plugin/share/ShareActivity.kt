package info.bagen.rust.plaoc.microService.sys.plugin.share

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import info.bagen.rust.plaoc.R
import info.bagen.rust.plaoc.microService.helper.Callback
import info.bagen.rust.plaoc.microService.helper.Signal
import info.bagen.rust.plaoc.microService.helper.ioAsyncExceptionHandler
import info.bagen.rust.plaoc.microService.sys.plugin.share.ShareController.Companion.controller
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ShareActivity : AppCompatActivity(){

    private var isFirstOpen = true
    companion object {
        val RESULT_SHARE_CODE = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller.activity = this
        debugShare("ShareActivity",this)
    }

    override fun onResume() {
        super.onResume()
        if (isFirstOpen) {
            isFirstOpen = false
        } else {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 分享返回数据
        if (requestCode == RESULT_SHARE_CODE) {
            GlobalScope.launch(ioAsyncExceptionHandler) {
                getShareSignal.emit(data?.dataString ?: "OK")
                debugShare("RESULT_SHARE_CODE", data?.dataString)
            }
        }
    }

    private val getShareSignal = Signal<String>()

    fun getShareData(cb: Callback<String>) = getShareSignal.listen(cb)
}