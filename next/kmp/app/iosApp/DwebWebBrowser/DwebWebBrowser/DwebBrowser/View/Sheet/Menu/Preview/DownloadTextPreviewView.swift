//
//  DownloadTextPreviewView.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/18.
//

import SwiftUI
import QuickLook

struct DownloadTextPreviewView: DownloadPreview, UIViewControllerRepresentable {
    
    let url: URL
    
    init(_ url: URL) {
        self.url = url
    }
    
    static func isSupport(_ mime: DownloadDataMIME, _ localPath: String?) -> Bool {
        if let filePath = localPath {
            let url = URL(filePath: filePath)
            let isSupport = QLPreviewController.canPreview(url as QLPreviewItem)
            Log("mime:\(mime), support: \(isSupport)")
            return isSupport
        } else {
            return false
        }
    }
    
    typealias UIViewControllerType = QLPreviewController
    
    class Coordinator: NSObject, QLPreviewControllerDataSource, QLPreviewControllerDelegate {
        
        func numberOfPreviewItems(in controller: QLPreviewController) -> Int {
            return 1
        }
        
        func previewController(_ controller: QLPreviewController, previewItemAt index: Int) -> QLPreviewItem {
            return pereant.url as QLPreviewItem
        }
                
        let pereant: DownloadTextPreviewView
        init(pereant: DownloadTextPreviewView) {
            self.pereant = pereant
        }
    }
    
    func makeCoordinator() -> Coordinator {
        return Coordinator(pereant: self)
    }

    func makeUIViewController(context: Context) -> QLPreviewController {
        let vc = QLPreviewController(nibName: nil, bundle: nil)
        vc.delegate = context.coordinator
        vc.dataSource = context.coordinator
        return vc
    }
    
    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {
        
    }

}


