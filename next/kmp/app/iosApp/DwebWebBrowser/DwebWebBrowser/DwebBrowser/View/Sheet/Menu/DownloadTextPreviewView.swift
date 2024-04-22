//
//  DownloadTextPreviewView.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/18.
//

import SwiftUI
import QuickLook

struct DownloadTextPreviewView: UIViewControllerRepresentable {
    
    let fileURL: URL
    
    typealias UIViewControllerType = QLPreviewController
    
    class Coordinator: NSObject, QLPreviewControllerDataSource, QLPreviewControllerDelegate {
        
        func numberOfPreviewItems(in controller: QLPreviewController) -> Int {
            return 1
        }
        
        func previewController(_ controller: QLPreviewController, previewItemAt index: Int) -> QLPreviewItem {
            return pereant.fileURL as QLPreviewItem
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


