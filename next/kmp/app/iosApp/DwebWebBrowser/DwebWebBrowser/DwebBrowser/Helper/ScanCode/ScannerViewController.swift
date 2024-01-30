//
//  ScannerViewController.swift
//  DwebBrowser
//
//  Created by ui03 on 2023/7/11.
//

import UIKit
import AVFoundation

public class ScannerViewController: UIViewController {
    
    private let photoOutput = AVCapturePhotoOutput()
    private var isCapturing = false
    private var handler: ((UIImage) -> Void)?
    var parentView: CodeScannerView
    var codesFound = Set<String>()
    var didFinishScanning = false
    var lastTime = Date(timeIntervalSince1970: 0)
    private let showViewfinder: Bool
    
    let fallbackVideoCaptureDevice = AVCaptureDevice.default(for: .video)
    private var isGalleryShowing: Bool = false {
        didSet {
            if parentView.isGalleryPresented.wrappedValue != isGalleryShowing {
                parentView.isGalleryPresented.wrappedValue = isGalleryShowing
            }
        }
    }
    
    override public var prefersStatusBarHidden: Bool {
        true
    }
    
    override public var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        .all
    }
    
    var captureSession: AVCaptureSession?
    var previewLayer: AVCaptureVideoPreviewLayer!
    
    init(showViewfinder: Bool, parentView: CodeScannerView) {
        self.parentView = parentView
        self.showViewfinder = showViewfinder
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        self.addOrientationDidChangeObserver()
        self.setBackgroundColor()
        self.handleCameraPermission()
        self.view.addSubview(closeBtn)
    }
    
    override public func viewWillLayoutSubviews() {
        previewLayer?.frame = view.layer.bounds
    }
    
    override public func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        updateOrientation()
    }
    
    override public func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        setupSession()
    }
    
    override public func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        closeScanView()
    }
    
    @objc func updateOrientation() {
        guard let orientation = view.window?.windowScene?.interfaceOrientation else { return }
        guard let connection = captureSession?.connections.last, connection.isVideoOrientationSupported else { return }
        switch orientation {
        case .portrait:
            connection.videoOrientation = .portrait
        case .landscapeLeft:
            connection.videoOrientation = .landscapeLeft
        case .landscapeRight:
            connection.videoOrientation = .landscapeRight
        case .portraitUpsideDown:
            connection.videoOrientation = .portraitUpsideDown
        default:
            connection.videoOrientation = .portrait
        }
    }
    
    private func setupSession() {
        guard let captureSession = captureSession else {
            return
        }
        
        if previewLayer == nil {
            previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        }
        
        previewLayer.frame = view.layer.bounds
        previewLayer.videoGravity = .resizeAspectFill
        view.layer.addSublayer(previewLayer)
        addviewfinder()
        
        reset()
        
        if (captureSession.isRunning == false) {
            DispatchQueue.global(qos: .userInteractive).async {
                self.captureSession?.startRunning()
            }
        }
    }
    
    private func handleCameraPermission() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .restricted:
            break
        case .denied:
            self.didFail(reason: .permissionDenied)
        case .notDetermined:
            self.requestCameraAccess {
                self.setupCaptureDevice()
                DispatchQueue.main.async {
                    self.setupSession()
                }
            }
        case .authorized:
            self.setupCaptureDevice()
            self.setupSession()
            
        default:
            break
        }
    }
    
    private func requestCameraAccess(completion: (() -> Void)?) {
        AVCaptureDevice.requestAccess(for: .video) { [weak self] status in
            guard status else {
                self?.didFail(reason: .permissionDenied)
                return
            }
            completion?()
        }
    }
    
    private func addOrientationDidChangeObserver() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(updateOrientation),
            name: Notification.Name("UIDeviceOrientationDidChangeNotification"),
            object: nil
        )
    }
    
    private func setBackgroundColor(_ color: UIColor = .black) {
        view.backgroundColor = color
    }
    
    private func setupCaptureDevice() {
        captureSession = AVCaptureSession()
        
        guard let videoCaptureDevice = parentView.videoCaptureDevice ?? fallbackVideoCaptureDevice else {
            return
        }
        
        let videoInput: AVCaptureDeviceInput
        
        do {
            videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
        } catch {
            didFail(reason: .initError(error))
            return
        }
        
        if (captureSession!.canAddInput(videoInput)) {
            captureSession!.addInput(videoInput)
        } else {
            didFail(reason: .badInput)
            return
        }
        let metadataOutput = AVCaptureMetadataOutput()
        
        if (captureSession!.canAddOutput(metadataOutput)) {
            captureSession!.addOutput(metadataOutput)
            captureSession?.addOutput(photoOutput)
            metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = parentView.codeTypes
        } else {
            didFail(reason: .badOutput)
            return
        }
    }
    
    func openGallery() {
        isGalleryShowing = true
        let imagePicker = UIImagePickerController()
        imagePicker.delegate = self
        present(imagePicker, animated: true, completion: nil)
    }
    
    @objc func openGalleryFromButton(_ sender: UIButton) {
        openGallery()
    }
    
    func updateViewController(isTorchOn: Bool, isGalleryPresented: Bool, isManualCapture: Bool, isManualSelect: Bool) {
        guard let videoCaptureDevice = parentView.videoCaptureDevice ?? fallbackVideoCaptureDevice else {
            return
        }
        
        if videoCaptureDevice.hasTorch {
            try? videoCaptureDevice.lockForConfiguration()
            videoCaptureDevice.torchMode = isTorchOn ? .on : .off
            videoCaptureDevice.unlockForConfiguration()
        }
        
        if isGalleryPresented && !isGalleryShowing {
            openGallery()
        }
    }
    
    func reset() {
        codesFound.removeAll()
        didFinishScanning = false
        lastTime = Date(timeIntervalSince1970: 0)
    }
    
    func readyManualCapture() {
        guard parentView.scanMode == .manual else { return }
        self.reset()
        lastTime = Date()
    }
    
    func isPastScanInterval() -> Bool {
        Date().timeIntervalSince(lastTime) >= parentView.scanInterval
    }
    
    func isWithinManualCaptureInterval() -> Bool {
        Date().timeIntervalSince(lastTime) <= 0.5
    }
    
    func found(_ result: ScanResult) {
        lastTime = Date()
        
        if parentView.shouldVibrateOnSuccess {
            AudioServicesPlaySystemSound(SystemSoundID(kSystemSoundID_Vibrate))
        }
        
        parentView.completion(.success(result))
    }
    
    func didFail(reason: ScanError) {
        parentView.completion(.failure(reason))
    }
    
    private func addviewfinder() {
        guard showViewfinder, let imageView = viewFinder else { return }
        imageView.frame = CGRect(x: screen_width * 0.5 - 200, y: 160, width: 400, height: 20)
        view.addSubview(imageView)
        
        viewFinderAnimation()
    }
    
    private func viewFinderAnimation() {
        let animation = CAKeyframeAnimation(keyPath: "position.y")
        animation.values = [160,500]
        animation.keyTimes = [0,1]
        animation.duration = 1.2
        animation.repeatCount = MAXFLOAT
        animation.calculationMode = .linear
        viewFinder?.layer.add(animation, forKey: nil)
    }
    
    @objc private func closeAction() {
        closeScanView()
        dismiss(animated: true)
    }
    
    private func closeScanView() {
        if (captureSession?.isRunning == true) {
            DispatchQueue.global(qos: .userInteractive).async {
                self.captureSession?.stopRunning()
            }
        }
        
        NotificationCenter.default.removeObserver(self)
        
        viewFinder?.layer.removeAllAnimations()
    }
    
    private lazy var closeBtn: UIButton = {
        let button = UIButton()
        button.frame = CGRect(x: 16, y: 16, width: 36, height: 36)
        button.setImage(UIImage(resource: .scanClose), for: .normal)
        button.addTarget(self, action: #selector(closeAction), for: .touchUpInside)
        return button
    }()
    
    private lazy var viewFinder: UIImageView? = {
        let image = UIImage(resource: .viewfinder)
        let imageView = UIImageView(image: image)
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    
}

extension ScannerViewController: UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    
    public func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
        isGalleryShowing = false
        dismiss(animated: true)
        
        guard let qrcodeImg = info[.originalImage] as? UIImage else { return }
        guard let detector = CIDetector(ofType: CIDetectorTypeQRCode, context: nil,options: [CIDetectorAccuracy: CIDetectorAccuracyHigh]) else { return }
        guard let ciImage = CIImage(image: qrcodeImg) else { return }
        var qrCodeLink = ""
        guard let features = detector.features(in: ciImage) as? [CIQRCodeFeature] else { return }
        
        for feature in features {
            qrCodeLink = feature.messageString ?? ""
            if qrCodeLink == "" {
                didFail(reason: .badOutput)
            } else {
                let corners = [
                    feature.bottomLeft,
                    feature.bottomRight,
                    feature.topRight,
                    feature.topLeft
                ]
                let result = ScanResult(string: qrCodeLink, type: .qr, image: qrcodeImg, corners: corners)
                found(result)
            }
        }
        
    }
    
    public func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        isGalleryShowing = false
        dismiss(animated: true)
    }
}

extension ScannerViewController: AVCaptureMetadataOutputObjectsDelegate {
    
    public func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        guard let metadataObject = metadataObjects.first else { return }
        guard let readableObject = metadataObject as? AVMetadataMachineReadableCodeObject else { return }
        guard let stringValue = readableObject.stringValue else { return }
        
        guard didFinishScanning == false else { return }
        
        let photoSettings = AVCapturePhotoSettings()
        guard !isCapturing else { return }
        isCapturing = true
        
        handler = { [self] image in
            let result = ScanResult(string: stringValue, type: readableObject.type, image: image, corners: readableObject.corners)
            
            switch parentView.scanMode {
            case .once:
                found(result)
                // make sure we only trigger scan once per use
                didFinishScanning = true
                
            case .manual:
                if !didFinishScanning, isWithinManualCaptureInterval() {
                    found(result)
                    didFinishScanning = true
                }
                
            case .oncePerCode:
                if !codesFound.contains(stringValue) {
                    codesFound.insert(stringValue)
                    found(result)
                }
                
            case .continuous:
                if isPastScanInterval() {
                    found(result)
                }
            }
        }
        photoOutput.capturePhoto(with: photoSettings, delegate: self)
    }
}

extension ScannerViewController: AVCapturePhotoCaptureDelegate {
    
    public func photoOutput(
        _ output: AVCapturePhotoOutput,
        didFinishProcessingPhoto photo: AVCapturePhoto,
        error: Error?
    ) {
        isCapturing = false
        guard let imageData = photo.fileDataRepresentation() else {
            Log("Error while generating image from photo capture data.");
            return
        }
        guard let qrImage = UIImage(data: imageData) else {
            Log("Unable to generate UIImage from image data.");
            return
        }
        handler?(qrImage)
    }
    
    public func photoOutput(
        _ output: AVCapturePhotoOutput,
        willCapturePhotoFor resolvedSettings: AVCaptureResolvedPhotoSettings
    ) {
        AudioServicesDisposeSystemSoundID(1108)
    }
    
    public func photoOutput(
        _ output: AVCapturePhotoOutput,
        didCapturePhotoFor resolvedSettings: AVCaptureResolvedPhotoSettings
    ) {
        AudioServicesDisposeSystemSoundID(1108)
    }
    
}

public extension AVCaptureDevice {
    
    /// This returns the Ultra Wide Camera on capable devices and the default Camera for Video otherwise.
    static var bestForVideo: AVCaptureDevice? {
        let deviceHasUltraWideCamera = !AVCaptureDevice.DiscoverySession(deviceTypes: [.builtInUltraWideCamera], mediaType: .video, position: .back).devices.isEmpty
        return deviceHasUltraWideCamera ? AVCaptureDevice.default(.builtInUltraWideCamera, for: .video, position: .back) : AVCaptureDevice.default(for: .video)
    }
    
}
