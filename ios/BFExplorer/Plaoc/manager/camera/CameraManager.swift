//  CameraManager.swift
//  BFExplorer
//
//  Created by ui08 on 2023/1/13.
//

import UIKit
import Photos
import PhotosUI
import JavaScriptCore

let sharedCameraMgr = CameraManager()

class CameraManager: NSObject {
    private var settings = CameraSettings()
    private let defaultSource = CameraSource.prompt
    private let defaultDirection = CameraDirection.rear
    private var imageOption: ImageOption?
    private var multiple = false
    private var imageCounter = 0
    private var controller: WebViewViewController?
    private var functionName: String?

    public var isSimEnvironment: Bool {
        #if targetEnvironment(simulator)
        return true
        #else
        return false
        #endif
    }

    private func permissioned(action: @escaping (() -> Void)) {
        var cameraGranted = false, photoGranted = false;

        for permission in CameraPermissionType.allCases {
            switch permission {
            case .camera:
                permissionManager.startPermissionAuthenticate(type: .camera, isSet: true) { result in
                    cameraGranted = result
                }
            case .photos:
                permissionManager.startPermissionAuthenticate(type: .photo, isSet: true) { result in
                    photoGranted = result
                }
            }
        }

        if cameraGranted && photoGranted {
            action()
        }
    }

    // quality、resultType、saveToGallery、direction
    func cameraSettings(option: ImageOption) -> CameraSettings {
        var settings = CameraSettings()

        settings.jpegQuality = min(abs(CGFloat(option.quality?.pk.toFloat ?? 100.0)) / 100.0, 1.0)
        settings.allowEditing = option.allowEditing ?? false
        settings.source = CameraSource(rawValue: option.source ?? defaultSource.rawValue) ?? defaultSource
        settings.direction = CameraDirection(rawValue: option.direction ?? defaultDirection.rawValue) ?? defaultDirection
        settings.saveToGallery = option.saveToGallery ?? false
        settings.shouldCorrectOrientation = option.correctOrientation ?? true
        if let typeString = option.resultType, let type = CameraResultType(rawValue: typeString) {
            settings.resultType = type
        }

        settings.width = CGFloat(option.width ?? 0)
        settings.height = CGFloat(option.height ?? 0)
        if settings.width > 0 || settings.height > 0 {
            settings.shouldResize = true
        }

        settings.userPromptText = CameraPromptText(title: option.promptLabelHeader,
                                                   photoAction: option.promptLabelPhoto,
                                                   cameraAction: option.promptLabelPicture,
                                                   cancelAction: option.promptLabelCancel)

        if let styleString = option.presentationStyle, styleString == "popover" {
            settings.presentationStyle = .popover
        } else {
            settings.presentationStyle = .fullScreen
        }

        return settings
    }

    // 异步返回结果
    private func asyncReturnValue(_ result: [String: Any]) {
        let functionName = self.functionName!
        let data = ChangeTools.dicValueString(result) ?? "{'error': '\(functionName) return value error'}"
        
        controller?.jsManager.asyncReturnValue(functionName: functionName, result: data)
    }

    private func checkUsageDescriptions() -> String? {
        if let dict = Bundle.main.infoDictionary {
            for key in CameraPropertyListKeys.allCases where dict[key.rawValue] == nil {
                return key.missingMessage
            }
        }
        return nil
    }
}

extension CameraManager {
    func showPrompt() {
        let alert = UIAlertController(title: settings.userPromptText.title, message: nil, preferredStyle: UIAlertController.Style.actionSheet)
        alert.addAction(UIAlertAction(title: settings.userPromptText.photoAction, style: .default, handler: { [weak self] (_: UIAlertAction) in
            self?.showPhotos()
        }))

        alert.addAction(UIAlertAction(title: settings.userPromptText.cameraAction, style: .default, handler: { [weak self] (_: UIAlertAction) in
            self?.showCamera()
        }))

        alert.addAction(UIAlertAction(title: settings.userPromptText.cancelAction, style: .default, handler: { [weak self] (_: UIAlertAction) in
            self?.asyncReturnValue(["error": "User cancelled photos app"])
        }))

        self.setCenteredPopover(alert)

        controller?.present(alert, animated: true, completion: nil)
    }

    func showCamera() {
        if isSimEnvironment || !UIImagePickerController.isSourceTypeAvailable(UIImagePickerController.SourceType.camera) {
            self.asyncReturnValue(["error": "Camera not available while running in Simulator"])
            return
        }

        DispatchQueue.main.async {
            self.presentCameraPicker()
        }
    }

    func showPhotos() {
        presentSystemAppropriateImagePicker()
    }

    func presentCameraPicker() {
        let picker = UIImagePickerController()
        picker.delegate = self
        picker.allowsEditing = self.settings.allowEditing

        picker.sourceType = .camera
        if settings.direction == .rear, UIImagePickerController.isCameraDeviceAvailable(.rear) {
            picker.cameraDevice = .rear
        } else if settings.direction == .front, UIImagePickerController.isCameraDeviceAvailable(.front) {
            picker.cameraDevice = .front
        }
        // present
        picker.modalPresentationStyle = settings.presentationStyle
        if settings.presentationStyle == .popover {
            picker.popoverPresentationController?.delegate = self
            setCenteredPopover(picker)
        }
        controller?.present(picker, animated: true, completion: nil)
    }

    func presentSystemAppropriateImagePicker() {
        if #available(iOS 14, *) {
            presentPhotoPicker()
        } else {
            presentImagePicker()
        }
    }

    func presentImagePicker() {
            let picker = UIImagePickerController()
            picker.delegate = self
            picker.allowsEditing = self.settings.allowEditing
            // select the input
            picker.sourceType = .photoLibrary
            // present
            picker.modalPresentationStyle = settings.presentationStyle
            if settings.presentationStyle == .popover {
                picker.popoverPresentationController?.delegate = self
                setCenteredPopover(picker)
            }
            controller?.present(picker, animated: true, completion: nil)
        }

    @available(iOS 14, *)
    func presentPhotoPicker() {
        var configuration = PHPickerConfiguration(photoLibrary: PHPhotoLibrary.shared())
        configuration.selectionLimit = self.multiple ? (self.imageOption?.limit ?? 0) : 1
        configuration.filter = .images
        let picker = PHPickerViewController(configuration: configuration)
        picker.delegate = self

        picker.modalPresentationStyle = settings.presentationStyle

        if settings.presentationStyle == .popover {
            picker.popoverPresentationController?.delegate = self

            if controller != nil {
                setCenteredPopover(picker)
            }
        }

        controller?.present(picker, animated: true, completion: nil)
    }

    func processedImage(from image: UIImage, with metadata: [String: Any]?) -> ProcessedImage {
        var result = ProcessedImage(image: image, metadata: metadata ?? [:])
        if settings.shouldResize, settings.width > 0 || settings.height > 0 {
            result.image = result.image.reformat(to: CGSize(width: settings.width, height: settings.height))
            result.overwriteMetadataOrientation(to: 1)
        } else if settings.shouldCorrectOrientation {
            result.image = result.image.reformat()
            result.overwriteMetadataOrientation(to: 1)
        }

        return result
    }

    func returnImage(_ processedImage: ProcessedImage, isSaved: Bool) {
        guard let jpeg = processedImage.generateJPEG(with: settings.jpegQuality) else {
            self.asyncReturnValue(["error": "Unable to convert image to jpeg"])
            return
        }

        if settings.resultType == CameraResultType.uri || multiple {
            guard let fileURL = try? saveTemporaryImage(jpeg)
                   else {
                self.asyncReturnValue(["error": "Unable to get portable path to file"])
                return
            }
            let webURL = URL(fileURLWithPath: fileURL.path)
            if self.multiple {
                self.asyncReturnValue([
                    "photos": [[
                        "path": fileURL.absoluteString,
                        "exif": processedImage.exifData,
                        "webPath": webURL.absoluteString,
                        "format": "jpeg"
                    ]]
                ])
                return
            }

            self.asyncReturnValue([
                "path": fileURL.absoluteString,
                "exif": processedImage.exifData,
                "webPath": webURL.absoluteString,
                "format": "jpeg",
                "saved": isSaved
            ])
        } else if settings.resultType == CameraResultType.base64 {
            self.asyncReturnValue([
                "base64String": jpeg.base64EncodedString(),
                "exif": processedImage.exifData,
                "format": "jpeg",
                "saved": isSaved
            ])
        } else if settings.resultType == CameraResultType.dataURL {
            self.asyncReturnValue([
                "dataUrl": "data:image/jpeg;base64," + jpeg.base64EncodedString(),
                "exif": processedImage.exifData,
                "format": "jpeg",
                "saved": isSaved
            ])
        }
    }

    func returnImages(_ processedImages: [ProcessedImage]) {
        var photos: [PhotosResult] = []
        for processedImage in processedImages {
            guard let jpeg = processedImage.generateJPEG(with: settings.jpegQuality) else {
                self.asyncReturnValue(["error": "Unable to convert image to jpeg"])
                return
            }

            guard let fileURL = try? saveTemporaryImage(jpeg) else {
                self.asyncReturnValue(["error": "Unable to get portable path to file"])
                return
            }

            let webURL = URL(fileURLWithPath: fileURL.path)

            photos.append([
                "path": fileURL.absoluteString,
                "exif": processedImage.exifData,
                "webPath": webURL.absoluteString,
                "format": "jpeg"
            ])
        }

        self.asyncReturnValue(["photos": photos])
    }

    func returnProcessedImage(_ processedImage: ProcessedImage) {
        // conditionally save the image
        if settings.saveToGallery && (processedImage.flags.contains(.edited) == true || processedImage.flags.contains(.gallery) == false) {
            _ = ImageSaver(image: processedImage.image) { error in
                var isSaved = false
                if error == nil {
                    isSaved = true
                }
                self.returnImage(processedImage, isSaved: isSaved)
            }
        } else {
            self.returnImage(processedImage, isSaved: false)
        }
    }

    func saveTemporaryImage(_ data: Data) throws -> URL {
            var url: URL
            repeat {
                imageCounter += 1
                url = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("photo-\(imageCounter).jpg")
            } while FileManager.default.fileExists(atPath: url.path)

            try data.write(to: url, options: .atomic)
            return url
    }

    func processImage(from info: [UIImagePickerController.InfoKey: Any]) -> ProcessedImage? {
            var selectedImage: UIImage?
            var flags: PhotoFlags = []
            // get the image
            if let edited = info[UIImagePickerController.InfoKey.editedImage] as? UIImage {
                selectedImage = edited // use the edited version
                flags = flags.union([.edited])
            } else if let original = info[UIImagePickerController.InfoKey.originalImage] as? UIImage {
                selectedImage = original // use the original version
            }
            guard let image = selectedImage else {
                return nil
            }
            var metadata: [String: Any] = [:]
            // get the image's metadata from the picker or from the photo album
            if let photoMetadata = info[UIImagePickerController.InfoKey.mediaMetadata] as? [String: Any] {
                metadata = photoMetadata
            } else {
                flags = flags.union([.gallery])
            }
            if let asset = info[UIImagePickerController.InfoKey.phAsset] as? PHAsset {
                metadata = asset.imageData
            }
            // get the result
            var result = processedImage(from: image, with: metadata)
            result.flags = flags
            return result
        }



    func setCenteredPopover(_ vc: UIViewController) {
        if controller != nil {
            vc.popoverPresentationController?.sourceRect = CGRect(x:controller!.view.center.x, y:controller!.view.center.y, width:0, height:0)
            vc.popoverPresentationController?.sourceView = controller!.view
            vc.popoverPresentationController?.permittedArrowDirections = .init(rawValue: 0)
        }
    }
}

extension CameraManager: PHPickerViewControllerDelegate {
    internal func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true, completion: nil)
        guard let result = results.first else {
            self.asyncReturnValue(["error": "User cancelled photos app"])
            return
        }

        if multiple {
            var images: [ProcessedImage] = []
            var processCount = 0
            for img in results {
                guard img.itemProvider.canLoadObject(ofClass: UIImage.self) else {
                    self.asyncReturnValue(["error": "Error loading image"])
                    return
                }

                img.itemProvider.loadObject(ofClass: UIImage.self) { [weak self] (reading, _) in
                    if let image = reading as? UIImage {
                        var asset: PHAsset?
                        if let assetId = img.assetIdentifier {
                            asset = PHAsset.fetchAssets(withLocalIdentifiers: [assetId], options: nil).firstObject
                        }
                        if let processedImage = self?.processedImage(from: image, with: asset?.imageData) {
                            images.append(processedImage)
                        }
                        processCount += 1
                        if processCount == results.count {
                            self?.returnImages(images)
                        }
                    } else {
                        self?.asyncReturnValue(["error": "Error loading image"])
                    }
                }
            }
        } else {
            guard result.itemProvider.canLoadObject(ofClass: UIImage.self) else {
                self.asyncReturnValue(["error": "Error loading image"])
                return
            }

            result.itemProvider.loadObject(ofClass: UIImage.self) { [weak self] (reading, _) in
                if let image = reading as? UIImage {
                    var asset: PHAsset?
                    if let assetId = result.assetIdentifier {
                        asset = PHAsset.fetchAssets(withLocalIdentifiers: [assetId], options: nil).firstObject
                    }
                    if var processedImage = self?.processedImage(from: image, with: asset?.imageData) {
                        processedImage.flags = .gallery
                        self?.returnProcessedImage(processedImage)
                        return
                    }
                }

                self?.asyncReturnValue(["error": "Error loading image"])
            }
        }
    }
}

extension CameraManager: UIImagePickerControllerDelegate, UINavigationControllerDelegate, UIPopoverPresentationControllerDelegate {
    internal func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true)
        self.asyncReturnValue(["error": "User cancelled photos app"])
    }

    public func popoverPresentationControllerDidDismissPopover(_ popoverPresentationController: UIPopoverPresentationController) {
        self.asyncReturnValue(["error": "User cancelled photos app"])
    }

    public func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        self.asyncReturnValue(["error": "User cancelled photos app"])
    }

    public func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
        picker.dismiss(animated: true) {
            if let processedImage = self.processImage(from: info) {
                self.returnProcessedImage(processedImage)
            } else {
                self.asyncReturnValue(["error": "Error processing image"])
            }
        }
    }
}

extension CameraManager {
    func getPhoto(param: Any, controller: WebViewViewController?, functionName: String) {
        self.controller = controller
        self.functionName = functionName
        self.multiple = false
        guard let param = param as? String else {
            self.asyncReturnValue(["error": "\(functionName) param error"])
            return
        }
        let jsonData = Data(param.utf8)

        do {
            let option = try JSONDecoder().decode(ImageOption.self, from: jsonData)
            self.settings = cameraSettings(option: option)
            self.imageOption = option

            if let missingUsageDescription = checkUsageDescriptions() {
                print("missingUsageDescription: \(missingUsageDescription)")
                self.asyncReturnValue(["error": missingUsageDescription])
                return
            }

            permissioned {
                DispatchQueue.main.async {
                    switch self.settings.source {
                    case .prompt:
                        self.showPrompt()
                    case .camera:
                        self.showCamera()
                    case .photos:
                        self.showPhotos()
                    }
                }
            }
        } catch {
            print(error.localizedDescription)
        }
    }

    func pickImages(param: Any, controller: WebViewViewController?, functionName: String) {
        self.controller = controller
        self.functionName = functionName
        self.multiple = true
        guard let param = param as? String else {
            self.asyncReturnValue(["error": "\(functionName) param error"])
            return
        }
        let jsonData = Data(param.utf8)

        do {
            var option = try JSONDecoder().decode(ImageOption.self, from: jsonData)
            self.settings = cameraSettings(option: option)
            self.imageOption = option

            permissioned {
                DispatchQueue.main.async {
                    self.showPhotos()
                }
            }
        } catch {
            print(error.localizedDescription)
            self.asyncReturnValue(["error": error.localizedDescription])
        }
    }
}
