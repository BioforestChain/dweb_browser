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
package com.king.mlkit.vision.barcode;

import android.view.View;
import androidx.annotation.Nullable;
import com.google.mlkit.vision.barcode.Barcode;
import com.king.mlkit.vision.barcode.analyze.BarcodeScanningAnalyzer;

import com.king.mlkit.vision.camera.analyze.Analyzer;

import java.util.List;


public abstract class QRCodeCameraScanActivity extends BarcodeCameraScanActivity {

    protected ViewfinderView viewfinderView;
    protected View ivFlashlight;


    @Override
    public void initUI() {
        int viewfinderViewId = getViewfinderViewId();
        if(viewfinderViewId != View.NO_ID && viewfinderViewId != 0){
            viewfinderView = findViewById(viewfinderViewId);
        }
        int ivFlashlightId = getFlashlightId();
        if(ivFlashlightId != View.NO_ID && ivFlashlightId != 0){
            ivFlashlight = findViewById(ivFlashlightId);
            if(ivFlashlight != null){
                ivFlashlight.setOnClickListener(v -> onClickFlashlight());
            }
        }

        super.initUI();
    }
//    /**
//     * 点击相册
//     */
//    protected void pickPhotoClicked(){
//        if(PermissionUtils.checkPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
//            startPickPhoto();
//        }else{
//            PermissionUtils.requestPermission(this,
//                    Manifest.permission.READ_EXTERNAL_STORAGE,
//                    2
//            );
//        }
//    }
//
//
//    protected void startPickPhoto(){
//        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
//        startActivityForResult(pickIntent, 1);
//    }

    /**
     * 点击手电筒
     */
    protected void onClickFlashlight(){
        toggleTorchState();
    }

    /**
     * 切换闪光灯状态（开启/关闭）
     */
    protected void toggleTorchState(){
        if(getCameraScan() != null){
            boolean isTorch = getCameraScan().isTorchEnabled();
            getCameraScan().enableTorch(!isTorch);
            if(ivFlashlight != null){
                ivFlashlight.setSelected(!isTorch);
            }
        }
    }

//    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(resultCode == RESULT_OK){
//           if (requestCode == 1) {
//               processPhoto(data);
//           }
//           if (requestCode == 2) {
//               processScanResult(data);
//           }
//        }
//    }
//    protected void processScanResult(Intent data){
//        String text = CameraScan.parseScanResult(data);
//        Toast.makeText(this,text,Toast.LENGTH_SHORT).show();
//    }
//
//
//    protected void processPhoto(Intent data) {
//        Bitmap decodeAbleBitmap = null;
//        try {
//            decodeAbleBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
//             new BarcodeDecoder().process(decodeAbleBitmap);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if (decodeAbleBitmap == null) {
//                Log.e("======", "decodeAbleBitmap == null");
//                return;
//            }
//
//    }

    /**
     * 创建分析器，默认分析所有条码格式
     * @return
     */
    @Nullable
    @Override
    public Analyzer<List<Barcode>> createAnalyzer(){
        return new BarcodeScanningAnalyzer(Barcode.FORMAT_QR_CODE);
    }


    /**
     * 布局id
     * @return
     */
    public int getLayoutId(){
        return R.layout.ml_qrcode_camera_scan;
    }

    /**
     * {@link #viewfinderView} 的 ID
     * @return 默认返回{@code R.id.viewfinderView}, 如果不需要扫码框可以返回{@link View#NO_ID}
     */
    public int getViewfinderViewId(){
        return R.id.viewfinderView;
    }


    /**
     * 获取 {@link #ivFlashlight} 的ID
     * @return  默认返回{@code R.id.ivFlashlight}, 如果不需要手电筒按钮可以返回{@link View#NO_ID}
     */
    public int getFlashlightId(){
        return R.id.ivFlashlight;
    }

    public int getPathId(){
        return R.id.btn_photo;
    }
}
