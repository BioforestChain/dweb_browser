//
//  CodeScanner.swift
//  DwebBrowser
//
//  Created by ui03 on 2023/7/11.
//

import SwiftUI
import AVFoundation

public struct CodeScannerView: UIViewControllerRepresentable {
    
    let codeTypes: [AVMetadataObject.ObjectType]
    let scanMode: ScanMode
    let manualSelect: Bool
    let scanInterval: Double
    let showViewfinder: Bool
    var simulatedData = ""
    var shouldVibrateOnSuccess: Bool
    var isTorchOn: Bool
    var isGalleryPresented: Binding<Bool>
    var videoCaptureDevice: AVCaptureDevice?
    var completion: (Result<ScanResult, ScanError>) -> Void
    
    public init(
        codeTypes: [AVMetadataObject.ObjectType],
        scanMode: ScanMode = .once,
        manualSelect: Bool = false,
        scanInterval: Double = 2.0,
        showViewfinder: Bool = false,
        simulatedData: String = "",
        shouldVibrateOnSuccess: Bool = true,
        isTorchOn: Bool = false,
        isGalleryPresented: Binding<Bool> = .constant(false),
        videoCaptureDevice: AVCaptureDevice? = AVCaptureDevice.default(for: .video),
        completion: @escaping (Result<ScanResult, ScanError>) -> Void
    ) {
        self.codeTypes = codeTypes
        self.scanMode = scanMode
        self.manualSelect = manualSelect
        self.showViewfinder = showViewfinder
        self.scanInterval = scanInterval
        self.simulatedData = simulatedData
        self.shouldVibrateOnSuccess = shouldVibrateOnSuccess
        self.isTorchOn = isTorchOn
        self.isGalleryPresented = isGalleryPresented
        self.videoCaptureDevice = videoCaptureDevice
        self.completion = completion
    }
    public func makeUIViewController(context: Context) -> ScannerViewController {
        return ScannerViewController(showViewfinder: showViewfinder, parentView: self)
    }
    
    public func updateUIViewController(_ uiViewController: ScannerViewController, context: Context) {
        
        uiViewController.parentView = self
        uiViewController.updateViewController(
            isTorchOn: isTorchOn,
            isGalleryPresented: isGalleryPresented.wrappedValue,
            isManualCapture: scanMode == .manual,
            isManualSelect: manualSelect
        )
        
    }
}


