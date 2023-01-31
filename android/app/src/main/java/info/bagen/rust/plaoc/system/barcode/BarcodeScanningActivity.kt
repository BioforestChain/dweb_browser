package info.bagen.rust.plaoc.system.barcode


import android.util.Log
import android.widget.ImageView
import com.google.mlkit.vision.barcode.Barcode
import com.king.app.dialog.AppDialog
import com.king.app.dialog.AppDialogConfig
import com.king.mlkit.vision.barcode.BarcodeCameraScanActivity
import com.king.mlkit.vision.camera.AnalyzeResult
import com.king.mlkit.vision.camera.config.ResolutionCameraConfig
import info.bagen.rust.plaoc.ExportNative
import info.bagen.rust.plaoc.R
import info.bagen.rust.plaoc.createBytesFactory
import info.bagen.rust.plaoc.util.lib.drawRect


class BarcodeScanningActivity : BarcodeCameraScanActivity() {

    override fun initCameraScan() {
        super.initCameraScan()
        cameraScan.setPlayBeep(true)
            .setVibrate(true)
            .setCameraConfig(ResolutionCameraConfig(this))//设置CameraConfig
    }

    override fun onScanResultCallback(result: AnalyzeResult<MutableList<Barcode>>) {

        cameraScan.setAnalyzeImage(false)
        val buffer = StringBuilder()
        val bitmap = result.bitmap.drawRect { canvas, paint ->
            for ((index, data) in result.result.withIndex()) {
                buffer.append("[$index] ").append(data.displayValue).append("\n")
                data.boundingBox?.let {
                    Log.d("条形码扫码数据.xxxxxxxx", it.toString())
                    createBytesFactory(ExportNative.OpenQrScanner, it.toString())
                    canvas.drawRect(it, paint)
                }
            }
        }

        val config = AppDialogConfig(this, R.layout.barcode_result_dialog)
        config.setContent(buffer).setOnClickOk {
            AppDialog.INSTANCE.dismissDialog()
            cameraScan.setAnalyzeImage(true)
        }.setOnClickCancel {
            AppDialog.INSTANCE.dismissDialog()
            finish()
        }
        val imageView = config.getView<ImageView>(R.id.ivDialogContent)
        imageView.setImageBitmap(bitmap)
        AppDialog.INSTANCE.showDialog(config, false)
    }


}



