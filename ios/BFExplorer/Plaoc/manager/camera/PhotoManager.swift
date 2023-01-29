//
//  PhotoManager.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/22.
//

import UIKit
import ZLPhotoBrowser
import Photos

class PhotoManager: NSObject {
    
    private var assets: [PHAsset] = []
    private var images: [UIImage] = []
    private var assetDates: [Date] = []
    
    private func permissioned(action: @escaping (() -> Void)) {
        permissionManager.startPermissionAuthenticate(type: .photo, isSet: true) { result in
            if result {
                action()
            }
        }
    }
    //通过图片路径保存到相册
    func savePhoto(urlString: String) {
        permissioned {
            DispatchQueue.global().async {
                guard let imgUrl = URL(string: urlString) else { return }
                if let imageData = try? Data(contentsOf: imgUrl) {
                    let img = UIImage(data: imageData)
                    DispatchQueue.main.async {
                        guard img != nil else { return }
                        UIImageWriteToSavedPhotosAlbum(img!, self, #selector(self.saveImage(image:didFinishSavingWithError:contextInfo:)), nil)
                    }
                }
            }
        }
    }
    
    @objc private func saveImage(image: UIImage, didFinishSavingWithError error: NSError?, contextInfo: AnyObject) {
        if error != nil{
            print("保存失败")
        }else{
            print("保存成功")
        }
    }
    //获取相册图片
    func fetchTotalPhotos(photoCount: Int = 10) -> [UIImage] {
        
        generateImagesFromPhoto(photoCount: photoCount)
        return images
    }
    //获取相册图片转base64
    func fetchBase64Photos(photoCount: Int = 10) -> [String] {
        generateImagesFromPhoto(photoCount: photoCount)
        guard images.count > 0 else { return [] }
        let results = images.map { $0.imageToBase64() }
        return results
    }
    
    private func generateImagesFromPhoto(photoCount: Int) {
        permissioned {
            let photosOptions = PHFetchOptions()
            photosOptions.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
            photosOptions.predicate = NSPredicate(format: "mediaType = %d && NOT (creationDate IN %@)",
                                                  PHAssetMediaType.image.rawValue,self.assetDates)
            photosOptions.fetchLimit = photoCount
            let assets = PHAsset.fetchAssets(with: .image, options: photosOptions)
            let imageManager = PHCachingImageManager()
            imageManager.stopCachingImagesForAllAssets()
            for i in 0..<assets.count {
                let asset = assets[i]
                if asset.creationDate != nil {
                    self.assetDates.append(asset.creationDate!)
                }
                imageManager.requestImage(for: asset, targetSize: CGSize(width: 36, height: 36), contentMode: .aspectFill, options: nil) { image, info in
                    if image != nil {
                        self.images.append(image!)
                    }
                }
            }
        }
    }
    
    //拍照功能
    func startPrimordialCamera(controller: UIViewController) {
        permissioned {
            if UIImagePickerController.isSourceTypeAvailable(.camera) {
                let picker = UIImagePickerController()
                picker.delegate = self
                picker.sourceType = .camera
                picker.allowsEditing = true
                controller.present(picker, animated: true)
            }
        }
    }
    //从相册中选取图片/视频
    func fetchPhAssetsFromLibraya(isScan: Bool = false, controller: UIViewController, callback: @escaping (([UIImage]) -> Void)) {
        
        permissioned {
            let config = ZLPhotoConfiguration.default()
            config.allowSelectGif = false
            config.allowSelectOriginal = false
            config.cropVideoAfterSelectThumbnail = false
            config.allowEditVideo = false
            config.showSelectBtnWhenSingleSelect = true
            if isScan {
                config.maxSelectCount = 1
                config.allowSelectVideo = false
            }
            
            let photoPicker = ZLPhotoPreviewSheet(selectedAssets: self.assets)
            
            photoPicker.selectImageBlock = { [weak self] (photos, isSelectOriginal) in
                guard let strongSelf = self else { return }
                strongSelf.assets.removeAll()
                strongSelf.images.removeAll()
                for photo in photos {
                    strongSelf.assets.append(photo.asset)
                    strongSelf.images.append(photo.image)
                    if photo.asset.mediaType.rawValue == 2 {
                        //获取视频地址
                        ZLVideoManager.exportVideo(for: photo.asset) { url, error in
                            
                        }
                    }
                }
                callback(strongSelf.images)
            }
            photoPicker.showPhotoLibrary(sender: controller)
        }
    }
    //获取本地视频截图
    private func generatorLocalVedioImage(url: URL?) -> UIImage? {
        guard url != nil else { return nil }
        let avAsset = AVAsset(url: url!)
        
        let generator = AVAssetImageGenerator(asset: avAsset)
        generator.appliesPreferredTrackTransform = true
        let time = CMTimeMakeWithSeconds(0.0, preferredTimescale: 600)
        var actualTime = CMTimeMake(value: 0, timescale: 0)
        guard let imageRef = try? generator.copyCGImage(at: time, actualTime: &actualTime) else { return nil }
        let image = UIImage(cgImage: imageRef)
        return image
    }
    
    //获取网络视频截图
    private func generatorNetworkVedioImage(urlString: String) {
        guard let url = URL(string: urlString) else { return }
        DispatchQueue.global().async {
            let avAsset = AVURLAsset(url: url)
            let generator = AVAssetImageGenerator(asset: avAsset)
            generator.appliesPreferredTrackTransform = true
            let time = CMTimeMakeWithSeconds(0.0, preferredTimescale: 600)
            var actualTime = CMTimeMake(value: 0, timescale: 0)
            guard let imageRef = try? generator.copyCGImage(at: time, actualTime: &actualTime) else { return }
            let image = UIImage(cgImage: imageRef)
            DispatchQueue.main.async {
                //TODO 获取截图后逻辑
            }
        }
    }
    
    //保存图片到文件中
    func savePhotoToDocument(image: UIImage) {
        let fileManager = FileManager.default
        let filePath = "\(documentdir)/pickedimage.jpg"
        let imageData = image.jpegData(compressionQuality: 1.0)
        fileManager.createFile(atPath: filePath, contents: imageData, attributes: nil)
    }
    //获取图片路径
    func photoFilePath() -> String {
        "\(documentdir)/pickedimage.jpg"
    }
    
    //预览图片
    func previewImage(index: Int, controller: UIViewController, callback: @escaping (([UIImage], Bool) -> Void)) {
        let previewVC = ZLImagePreviewController(datas: assets, index: index)
        previewVC.doneBlock = { [weak self] res in  // res is [PHAsset]
            guard let strongSelf = self else { return }
            guard let photos = res as? [PHAsset] else { return }
            var isChange: Bool = false
            if res.isEmpty {
                //TODO 清空选择的图片
                strongSelf.assets.removeAll()
                strongSelf.images.removeAll()
                isChange = true
                callback(strongSelf.images, isChange)
                return
            }
            if strongSelf.assets.count != photos.count {  //预览时修改图片
                isChange = true
                let diffList = strongSelf.assets.filter{ !photos.contains($0) }
                let deleteImages = diffList.map { asset -> UIImage in
                    let index = strongSelf.assets.firstIndex(of: asset)
                    let image = strongSelf.images[index!]
                    return image
                }
                strongSelf.images = strongSelf.images.filter{ !deleteImages.contains($0) }
            }
            strongSelf.assets = photos
            callback(strongSelf.images, isChange)
        }
        previewVC.modalPresentationStyle = .fullScreen
        controller.showDetailViewController(previewVC, sender: nil)
    }
}

extension PhotoManager {
    
    //识别二维码
    func recognizeQRImage(image: UIImage) {
        
        guard let img = CIImage(image: image) else { return }
        let context = CIContext(options: nil)
        let detector = CIDetector(ofType: CIDetectorTypeQRCode, context: context,
                                          options: [CIDetectorAccuracy:CIDetectorAccuracyHigh])
        
        
        guard let features = detector?.features(in: img, options: [CIDetectorAccuracy: CIDetectorAccuracyHigh]) as? [CIQRCodeFeature] else { return }
        
        for feature in features {
            //TODO  扫描结果
            print(feature.messageString ?? "")
        }
    }
    
    //创建二维码图片
    func createQRForString(qrString: String?, qrImageName: String?) -> UIImage? {
        
        guard qrString != nil else { return nil }
        let data = qrString!.data(using: .utf8, allowLossyConversion: false)
        
        let filter = CIFilter(name: "CIQRCodeGenerator")
        filter?.setValue(data, forKey: "inputMessage")
        filter?.setValue("H", forKey: "inputCorrectionLevel")
        
        let qrCIImage = filter?.outputImage
    
        let colorFilter = CIFilter(name: "CIFalseColor")
        colorFilter?.setDefaults()
        colorFilter?.setValue(qrCIImage, forKey: "inputImage")
        colorFilter?.setValue(CIColor(red: 0, green: 0, blue: 0), forKey: "inputColor0")
        colorFilter?.setValue(CIColor(red: 1, green: 1, blue: 1), forKey: "inputColor1")
        
        guard let ciImage = (colorFilter?.outputImage?
            .transformed(by: CGAffineTransform(scaleX: 5, y: 5))) else { return nil }
        let codeImage = UIImage(ciImage: ciImage)
        
        if let qrName = qrImageName, let iconImage = UIImage(named: qrName) {
            let rect = CGRect(x: 0, y: 0, width: codeImage.size.width, height: codeImage.size.height)
            UIGraphicsBeginImageContext(rect.size)
            
            codeImage.draw(in: rect)
            let avatarSize = CGSize(width: rect.size.width * 0.25, height: rect.size.height * 0.25)
            let x = (rect.width - avatarSize.width) * 0.5
            let y = (rect.height - avatarSize.height) * 0.5
            
            iconImage.draw(in: CGRect(x: x, y: y, width: avatarSize.width, height: avatarSize.height))
            let resultImage = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()
            return resultImage
        }
        return codeImage
    }
    
    //创建条形码 ZBarSDK
    func generateBarcode(content: String, size: CGSize) -> UIImage? {
        
        guard let barcodeFilter = CIFilter(name: "CICode128BarcodeGenerator") else { return nil }
        // 条形码内容
        barcodeFilter.setValue(content.data(using: .utf8), forKey: "inputMessage")
        // 左右间距
        barcodeFilter.setValue(0, forKey: "inputQuietSpace")
        
        guard let outputImage = barcodeFilter.outputImage else { return nil }
        // 调整图片大小及位置（小数跳转为整数）位置值向下调整，大小只向上调整
        let extent = outputImage.extent.integral
        // 条形码放大 处理模糊
        let scaleX = size.width / extent.width
        let scaleY = size.height / extent.height
        let clearImage = UIImage(ciImage: outputImage.transformed(by: CGAffineTransform(scaleX: scaleX, y: scaleY)))
        return clearImage
    }
    
    //条形码中插入文本
    func insertTextBarcode(text: String, attributes: [NSAttributedString.Key: Any]?, height: CGFloat, barcodeImage: UIImage) -> UIImage? {
        
        let barcodeSize = barcodeImage.size
        // 开启上下文
        UIGraphicsBeginImageContext(CGSize(width: barcodeSize.width, height: barcodeSize.height + 20))
        // 绘制条形码图片
        barcodeImage.draw(in: CGRect(origin: .zero, size: barcodeSize))
        // 文本样式
        let style = NSMutableParagraphStyle()
        style.alignment = .center
        let defaultAttri: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 15),
            .foregroundColor: UIColor.black,
            .kern: 2,
            .paragraphStyle: style
        ]
        let attri = attributes ?? defaultAttri
        // 绘制文本
        (text as NSString).draw(in: CGRect(x: 0, y: barcodeSize.height, width: barcodeSize.width, height: height), withAttributes: attri)
        let outputImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return outputImage
    }
}

extension PhotoManager: UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
        picker.dismiss(animated: true)
        guard let image = info[UIImagePickerController.InfoKey.editedImage] as? UIImage else { return }
        savePhotoToDocument(image: image)
    }

    
    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        
        picker.dismiss(animated: true)
    }
}
