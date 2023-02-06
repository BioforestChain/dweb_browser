/*
 * Copyright (C) Jenly, MLKit Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.bagen.rust.plaoc.system.barcode

import android.Manifest
import android.content.Intent
import android.graphics.Point
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.google.mlkit.vision.barcode.Barcode
import com.king.app.dialog.AppDialog
import com.king.app.dialog.AppDialogConfig
import com.king.mlkit.vision.barcode.BarcodeDecoder
import com.king.mlkit.vision.barcode.QRCodeCameraScanActivity
import com.king.mlkit.vision.barcode.utils.PointUtils
import com.king.mlkit.vision.camera.AnalyzeResult
import com.king.mlkit.vision.camera.CameraScan
import com.king.mlkit.vision.camera.analyze.Analyzer
import com.king.mlkit.vision.camera.util.LogUtils
import com.king.mlkit.vision.camera.util.PermissionUtils
import info.bagen.rust.plaoc.ExportNative
import info.bagen.rust.plaoc.MainActivity
import info.bagen.rust.plaoc.R
import info.bagen.rust.plaoc.createBytesFactory
import info.bagen.rust.plaoc.util.lib.drawRect
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QRCodeScanningActivity : QRCodeCameraScanActivity() {
    var isQRCode = false

    private lateinit var ivResult: ImageView
    private lateinit var ivPhoto: View
    override fun initUI() {
        super.initUI()

        val ivPhotoId = pathId
        if (ivPhotoId != View.NO_ID && ivPhotoId != 0) {
            ivPhoto = findViewById(ivPhotoId)
            if (ivPhoto != null) {
                ivPhoto.setOnClickListener { v: View? -> onPickPhotoClicked(true) }
            }
        }
        ivResult = findViewById<ImageView>(R.id.ivResult)
    }

    override fun initCameraScan() {
        super.initCameraScan()
        cameraScan.setPlayBeep(true)
            .setVibrate(true)
            .bindFlashlightView(ivFlashlight)
    }

    override fun getLayoutId(): Int {
        return R.layout.qrcode_scan_activity
    }

    override fun onBackPressed() {
        if (viewfinderView.isShowPoints) {//如果是结果点显示时，用户点击了返回键，则认为是取消选择当前结果，重新开始扫码
            ivResult.setImageResource(0)
            viewfinderView.showScanner()
            cameraScan.setAnalyzeImage(true)
            return
        }
        super.onBackPressed()
    }

    override fun onScanResultCallback(result: AnalyzeResult<MutableList<Barcode>>) {
        cameraScan.setAnalyzeImage(false)
        val results = result.result

        //取预览当前帧图片并显示，为结果点提供参照
        ivResult.setImageBitmap(previewView.bitmap)
        val points = ArrayList<Point>()
        for ((index, data) in results.withIndex()) {
            val rect = results[index].boundingBox // data.boundingBox
            //将实际的结果中心点坐标转换成界面预览的坐标
            val point = PointUtils.transform(
                rect.centerX(),
                rect.centerY(),
                result.bitmap.width,
                result.bitmap.height,
                viewfinderView.width,
                viewfinderView.height
            )
            points.add(point)
        }
        //设置Item点击监听
        viewfinderView.setOnItemClickListener {
            //显示点击Item将所在位置扫码识别的结果返回
            val intent = Intent()
            val data = results[it].displayValue
            intent.putExtra(CameraScan.SCAN_RESULT, data)
            data?.let { displayValue ->
                Log.d("1.2.xxxxxxxx", displayValue)
                // 拿到扫完的数据，传递给rust方法
                createBytesFactory(ExportNative.OpenQrScanner, displayValue)
            }
            setResult(RESULT_OK, intent)
            finish()

            /*
                显示结果后，如果需要继续扫码，则可以继续分析图像
             */
//            ivResult.setImageResource(0)
//            viewfinderView.showScanner()
//            cameraScan.setAnalyzeImage(true)
        }
        //显示结果点信息
        viewfinderView.showResultPoints(points)
        if (results.size == 1) {//只有一个结果直接返回
            GlobalScope.launch {
                delay(1000)
                val intent = Intent()
                intent.putExtra(CameraScan.SCAN_RESULT, results[0].displayValue)
                results[0].displayValue?.let {
                    Log.d("2.xxxxxxxx", it)
                    createBytesFactory(ExportNative.OpenQrScanner, it)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                MainActivity.REQUEST_CODE_PHOTO -> processPhoto(data)
                MainActivity.REQUEST_CODE_SCAN_CODE -> processScanResult(data)
            }
        }
    }

    fun getContext() = this

    private fun processScanResult(data: Intent?) {
        val text = CameraScan.parseScanResult(data)
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun processPhoto(data: Intent?) {
        data?.let {
            try {
                val src = MediaStore.Images.Media.getBitmap(contentResolver, it.data)
                BarcodeDecoder.process(src, object : Analyzer.OnAnalyzeListener<List<Barcode>?> {
                    override fun onSuccess(result: List<Barcode>) {
                        if (result.isNotEmpty()) {
                            val buffer = StringBuilder()
                            val bitmap = src.drawRect { canvas, paint ->
                                for ((index, data) in result.withIndex()) {
                                    buffer.append("[$index] ").append(data.displayValue)
                                        .append("\n")
                                    canvas.drawRect(data.boundingBox, paint)
                                }
                            }

                            val config =
                                AppDialogConfig(getContext(), R.layout.barcode_result_dialog)
                            config.setContent(buffer)
                                .setHideCancel(true)
                                .setOnClickOk {
                                    AppDialog.INSTANCE.dismissDialog()
                                }
                            val imageView = config.getView<ImageView>(R.id.ivDialogContent)
                            imageView.setImageBitmap(bitmap)
                            AppDialog.INSTANCE.showDialog(config)
                        } else {
                            LogUtils.d("result is null")
                            Toast.makeText(getContext(), "result is null", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                    override fun onFailure() {
                        LogUtils.d("onFailure")
                        Toast.makeText(getContext(), "onFailure", Toast.LENGTH_SHORT).show()
                    }
                    //如果指定具体的识别条码类型，速度会更快
                }, if (isQRCode) Barcode.FORMAT_QR_CODE else Barcode.FORMAT_ALL_FORMATS)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(getContext(), e.message, Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun onPickPhotoClicked(isQRCode: Boolean) {
        this.isQRCode = isQRCode
        if (PermissionUtils.checkPermission(
                getContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            startPickPhoto()
        } else {
            PermissionUtils.requestPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                MainActivity.REQUEST_CODE_REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    fun startPickPhoto() {
        val pickIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(pickIntent, MainActivity.REQUEST_CODE_PHOTO)
    }


    override fun getPathId(): Int {
        return R.id.btn_photo
    }
}
