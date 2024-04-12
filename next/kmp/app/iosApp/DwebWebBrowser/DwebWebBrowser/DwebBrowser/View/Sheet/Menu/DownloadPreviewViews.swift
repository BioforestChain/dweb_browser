//
//  DownloadPreviewViews.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/11.
//

import SwiftUI
import QuickLook

struct DownloadPreviewViews: View {
    var body: some View {
        Text("Hello, World!")
    }
}

#Preview {
    DownloadPreviewViews()
}


struct DownloadTextPreviewView: UIViewControllerRepresentable {
    
    let data: DownloadItem
    
    typealias UIViewControllerType = QLPreviewController
    
    class Coordinator: NSObject, QLPreviewControllerDataSource, QLPreviewControllerDelegate {
        
        func numberOfPreviewItems(in controller: QLPreviewController) -> Int {
            return 1
        }
        
        func previewController(_ controller: QLPreviewController, previewItemAt index: Int) -> QLPreviewItem {
            return pereant.data.url as QLPreviewItem
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


