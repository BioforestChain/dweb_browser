//
//  PhotoHandler.swift
//  DWebBrowser
//
//  Created by apple on 2022/5/11.
//

import UIKit
import PhotosUI

@objc enum PhotoHandleType:Int{
    case notInit
    case fetchPhoto_QRCode
    case savePhoto_Album
    case albumPhoto_Profile
    case takePhoto_Profile
    case albumPhotos_Post

    case scanCode  //barCode and qrCode both works
}


class PhotoHandlerX: NSObject {
    static let shared = PhotoHandlerX()
    private override init() { }
    var delegate : UIViewController?
    
    var imagePicker: UIImagePickerController!
    var currentHandleType: PhotoHandleType = .notInit  //1-选照片识别二维码。 2-拍照识别二维码。 3-选照片换头像。  4- 拍照换头像
    
    var jsHandler = [Int: (Any?, String?) -> Void]()

    
    var xHandlers = [PhotoHandleType: ThirdCallback]()
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func albumPhotoForProfile(jsCallBack:@escaping (Any?, String?) -> Void){
        currentHandleType = .albumPhoto_Profile
        jsHandler[PhotoHandleType.albumPhoto_Profile.rawValue] = jsCallBack
        self.openLocalPhotoAlbum()
    }

    func albumPhotosForPost(jsCallBack:@escaping (Any?, String?) -> Void){
        currentHandleType = .albumPhotos_Post
        jsHandler[PhotoHandleType.albumPhotos_Post.rawValue] = jsCallBack
        self.openAlbumForMultiPhoto()
    }

    func takePhotoForProfile(jsCallBack:@escaping (Any?, String?) -> Void){
        currentHandleType = .takePhoto_Profile
        jsHandler[PhotoHandleType.takePhoto_Profile.rawValue] = jsCallBack
        self.takePhoto()
    }

    func fetchPhotoForQRCode(jsCallBack:@escaping (Any?, String?) -> Void){
        jsHandler[PhotoHandleType.fetchPhoto_QRCode.rawValue] = jsCallBack
        showPickPhotoSheet()
    }

    func savePhotoToAlbum(url:String, jsCallBack:@escaping (Any?, String?) -> Void){
        jsHandler[PhotoHandleType.savePhoto_Album.rawValue] = jsCallBack
        guard let urlData = URL(string: url) else {return}
           // Fetch Image Data
        if let data = try? Data(contentsOf: urlData) {
           let image = UIImage(data: data)
           UIImageWriteToSavedPhotosAlbum(image!, self, #selector(image(_:didFinishSavingWithError:contextInfo:)), nil)
        }
    }

    func saveMyQRCodeToAlbum(imageBase64String:String, jsCallBack:@escaping (Any?, String?) -> Void){

        jsHandler[PhotoHandleType.savePhoto_Album.rawValue] = jsCallBack
        guard let imageData = Data(base64Encoded: imageBase64String) else { return  }
        let image = UIImage(data: imageData)
        UIImageWriteToSavedPhotosAlbum(image!, self, #selector(image(_:didFinishSavingWithError:contextInfo:)), nil)

    }
    
    func showFilePicker(){
        DocumentManager.sharedInstance.showDocumentMenuController(self.delegate!) { (isFileSelected, fileName, fileExtension, filePath) in
            print(filePath)

            let url = URL(fileURLWithPath: filePath)
            guard let imageData = try? Data(contentsOf: url) else { return }
            guard let loadedImage = UIImage(data: imageData) else { return }
            self.identifyQRcode(from: loadedImage)
        }
    }
    
    func showPickPhotoSheet(){
        let items = ["扫一扫", "拍照", "选照片", "选文件"]
        
        let vProperty = FWSheetViewProperty()
        vProperty.touchWildToHide = "1"
        vProperty.cancelItemTitleColor = UIColor.red
        vProperty.dark_itemDefaultBackgroundColor = kPV_RGBA(r: 223, g: 223, b: 223, a: 1)
        
        let sheetView = FWSheetView.sheet(title: "", itemTitles: items, itemBlock: {  (popupView, index, title) in
            print("Sheet：点击了第\(index)个按钮")
            
            switch(index){
            case 0:
                self.scan4QRCode()
                break
            case 1:
                self.currentHandleType = .fetchPhoto_QRCode
                self.takePhoto()
                break
            case 2:
                self.currentHandleType = .fetchPhoto_QRCode
                self.openLocalPhotoAlbum()
                break
            case 3:
                self.showFilePicker()
                break
            
            default: break
            }
            
        }, cancenlBlock: {
            print("点击了取消")
        }, property: vProperty)
        sheetView.show()
    }
}

//JS 需要 1.二维码字符串 2.下载保存图片 3.相册选图片换头像 4.拍照换头像  5.相册选图片发动态  6.保存二维码（同2）
extension PhotoHandlerX: LBXScanViewControllerDelegate, UIImagePickerControllerDelegate, UINavigationControllerDelegate{

    
    
    func responseData(status:Int, data:Any)->[String:Any]{
        if status == 0 {
            return ["code":0,"data":data]
        }
        return ["code":1,"data":"failed"]
    }
    
    // MARK: - RESULT ----下载保存图片
    @objc func image(_ image: UIImage, didFinishSavingWithError error: Error?, contextInfo: UnsafeRawPointer) {
        guard let handler = jsHandler[PhotoHandleType.savePhoto_Album.rawValue] else{return}

        if let error = error {
            print("ERROR: \(error)")
            handler(["code":1,"data":"failed"], nil)

        }else {
            handler(["code":0,"data":"success"], nil)
            print("Image saved", "The iamge is saved into your Photo Library.")
        }
    }
    
    // MARK: - RESULT ------识别出来二维码
    func identifyQRcode(from image:UIImage){
        let arrayResult = LBXScanWrapper.recognizeQRImage(image: image)
        guard let handler = jsHandler[PhotoHandleType.fetchPhoto_QRCode.rawValue] else { return }
        if arrayResult.count > 0 {
            let result = arrayResult[0]
            let qrCode:[String:Any] = ["qrCode":result.strScanned ?? ""]
            print("identified qrcode : ",qrCode)
            guard let qrCode = result.strScanned else {
                handler(["code":1,"data":"failed"], nil)
                return
            }
            handler(["code":0,"data":qrCode], nil)
        }else{
            handler(["code":1,"data":"failed"], nil)
        }
    }

    func scanFinished(scanResult: LBXScanResult, error: String?) {
        NSLog("scanResult:\(scanResult)")
        guard let handler = xHandlers[PhotoHandleType.scanCode] else { return }
        guard let qrCode = scanResult.strScanned as String? else{ return}
        handler(qrCode)

        DispatchQueue.main.asyncAfter(deadline: .now()) {
            self.delegate?.dismiss(animated: true)
            
        }
    }
    
    // MARK: - RESULT ----相册选择图片, 1.识别二维码  2.更换头像
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
        picker.dismiss(animated: true, completion: nil)
        
        var image:UIImage? = info[UIImagePickerController.InfoKey.editedImage] as? UIImage
        
        if (image == nil )
        {
            image = info[UIImagePickerController.InfoKey.originalImage] as? UIImage
        }

        if(image == nil) {
            return
        }
        if image != nil{
            if self.currentHandleType == .fetchPhoto_QRCode{
                self.identifyQRcode(from:image!)
                
            }else if self.currentHandleType == .albumPhoto_Profile{
                guard let handler = jsHandler[PhotoHandleType.albumPhoto_Profile.rawValue]
                else{
                    print("no js callback");
                    return
                }
               
                let data = image?.jpegData(compressionQuality: 1)
                guard let dataString = data?.base64EncodedString() else { return }
                
                handler(["code":0,"data":dataString], nil)
            }
        }
    }
    
    //拍照
    func takePhoto() {
        imagePicker =  UIImagePickerController()
        imagePicker.delegate = self
        imagePicker.sourceType = .camera
        self.delegate!.present(imagePicker, animated: true, completion: nil)
    }
    
    // MARK: - --模仿支付宝------
    func scan4QRCode() {
        //设置扫码区域参数
        var style = LBXScanViewStyle()

        style.centerUpOffset = 60
        style.xScanRetangleOffset = 30

        if UIScreen.main.bounds.size.height <= 480 {
            //3.5inch 显示的扫码缩小
            style.centerUpOffset = 40
            style.xScanRetangleOffset = 20
        }

        style.color_NotRecoginitonArea = UIColor(red: 0.4, green: 0.4, blue: 0.4, alpha: 0.4)

        style.photoframeAngleStyle = LBXScanViewPhotoframeAngleStyle.Inner
        style.photoframeLineW = 2.0
        style.photoframeAngleW = 16
        style.photoframeAngleH = 16

        style.isNeedShowRetangle = false

        style.anmiationStyle = LBXScanViewAnimationStyle.NetGrid
        style.animationImage = UIImage(named: "CodeScan.bundle/qrcode_scan_full_net")

        let vc = LBXScanViewController()

        vc.scanStyle = style
        vc.isSupportContinuous = true;
        
        vc.scanResultDelegate = self

        self.delegate!.present(vc, animated: true, completion: nil)
    }

    // MARK: -  ------- 相册--选一张识别二维码
    func openLocalPhotoAlbum() {

        LBXPermissions.authorizePhotoWith { [weak self] (granted) in

            if granted {
                if let strongSelf = self {
                    let picker = UIImagePickerController()
                  
                    picker.sourceType = UIImagePickerController.SourceType.photoLibrary
                    picker.delegate = self

                    picker.allowsEditing = true
                    strongSelf.delegate!.present(picker, animated: true, completion: nil)
                }
            } else {
                LBXPermissions.jumpToSystemPrivacySetting()
            }
        }
    }
    
    
    // MARK: -  ------- 相册--选多张 发动态
    func openAlbumForMultiPhoto(){
        var config = PHPickerConfiguration()
        config.selectionLimit = 9
        config.filter = PHPickerFilter.images

        let pickerViewController = PHPickerViewController(configuration: config)
        pickerViewController.delegate = self
        self.delegate?.present(pickerViewController, animated: true, completion: nil)
    }
    
}

// MARK: - RESULT ------- 相册--选多张 发动态 回调

extension PhotoHandlerX:  PHPickerViewControllerDelegate{
    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true, completion: nil)
        var imageBase64StringArray = [String]()
        for result in results {
           
            result.itemProvider.loadObject(ofClass: UIImage.self, completionHandler: { [self] (object, error) in
                    
               if let image = object as? UIImage {
                    let data = image.jpegData(compressionQuality: 1)
                    guard let dataString = data?.base64EncodedString() else { return }
                   
                    imageBase64StringArray.append(dataString)

                    if(imageBase64StringArray.count == results.count){
                        guard let handler = jsHandler[PhotoHandleType.albumPhotos_Post.rawValue] else{return}
                         
                        handler(["code":0,"data":imageBase64StringArray], nil)
                    }
                }
           })
        }
    }
}
