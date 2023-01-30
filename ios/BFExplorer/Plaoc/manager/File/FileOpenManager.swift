//
//  FileOpenManager.swift
//  BFExplorer
//
//  Created by ui08 on 2023/1/18.
//

import Foundation
import SwiftyJSON
import MobileCoreServices

let fileOpenManager = FileOpenManager()

class FileOpenManager: NSObject, UIDocumentInteractionControllerDelegate {
    var documentInteractionController: UIDocumentInteractionController!
    var controller: WebViewViewController?
    var functionName: String?
    
    struct chooserPosition: Codable {
        var x: Int
        var y: Int
    }
    
    struct FileOpenOption: Codable {
        var filePath: String
        var contentType: String?
        var openWithDefault: Bool? = true
        var chooserPosition: chooserPosition?
    }
    
    struct MimeTypeConverter {

        public static func mimeToUti(_ mimeType: String) -> String? {
            guard let contentType = UTTypeCreatePreferredIdentifierForTag(kUTTagClassMIMEType, mimeType as CFString, nil) else { return nil }

            return contentType.takeRetainedValue() as String
        }

        public static func fileExtensionToUti(_ ext: String) -> String? {
            guard let contentType = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, ext as CFString, nil) else { return nil }
            return contentType.takeRetainedValue() as String
        }

    }
    
    private func asyncReturnValue(functionName: String, result: String) {
        self.controller?.jsManager.asyncReturnValue(functionName: functionName, result: result)
    }
    
    @objc func open(param: Any, controller: WebViewViewController?, functionName: String, homePath: String) {
        self.controller = controller
        self.functionName = functionName
        
        guard let param = param as? String else {
            self.asyncReturnValue(functionName: functionName, result: "false")
            return
        }
        let jsonData = Data(param.utf8)
        
        do {
            let option = try JSONDecoder().decode(FileOpenOption.self, from: jsonData)
            let filePath = option.filePath
            let contentType = option.contentType
            let openWithDefault = option.openWithDefault ?? true
            
            DispatchQueue.main.async {
                var fileURL: URL
                let decodedPath = filePath.removingPercentEncoding
                
                if filePath == decodedPath {
                    let filePathReplaced = filePath.replacingOccurrences(of: "file://", with: "")
                    
                    fileURL = URL(fileURLWithPath: homePath + filePathReplaced)
                } else {
                    fileURL = URL(string: homePath + filePath)!
                }
                
                let fileManager = FileManager.default
                if !fileManager.fileExists(atPath: fileURL.path) {
                    print("File does not exist")
                    self.asyncReturnValue(functionName: functionName, result: "false")
                    return
                }
                
                var uti: String?
                if let mime = contentType, !mime.isEmpty {
                    uti = MimeTypeConverter.mimeToUti(mime)
                } else {
                    if fileURL.pathExtension.isEmpty {
                        print("Failed to determine the file type because extension is missing")
                        self.asyncReturnValue(functionName: functionName, result: "false")
                        return
                    }
                    
                    uti = MimeTypeConverter.fileExtensionToUti(fileURL.pathExtension)
                }
                
                guard let uti = uti else {
                    print("Failed to determine type of the file to open")
                    self.asyncReturnValue(functionName: functionName, result: "false")
                    return
                }
                
                self.documentInteractionController = UIDocumentInteractionController.init(url: fileURL)
                self.documentInteractionController.uti = uti
                self.documentInteractionController.delegate = self
                
                var wasOpened = false
                
                if openWithDefault {
                    wasOpened = self.documentInteractionController.presentPreview(animated: true)
                } else {
                    guard let view = controller?.view else {
                        print("Internal error. View not found!")
                        self.asyncReturnValue(functionName: functionName, result: "false")
                        return
                    }
                    
                    if UIDevice.current.userInterfaceIdiom == .pad {
                        if
                            let chooserPosition = option.chooserPosition
                        {
                            let x = CGFloat(chooserPosition.x)
                            let y = CGFloat(chooserPosition.y)
                            let rect = CGRect(x: 0, y: 0, width: x, height: y);
                            wasOpened = self.documentInteractionController.presentOpenInMenu(from: rect, in: view, animated: true)
                        } else {
                            let activityViewController = UIActivityViewController(activityItems: [fileURL], applicationActivities: nil)
                            activityViewController.popoverPresentationController?.permittedArrowDirections = UIPopoverArrowDirection(rawValue: 0)
                            activityViewController.popoverPresentationController?.sourceView = view
                            activityViewController.popoverPresentationController?.sourceRect = CGRect(x: view.frame.midX, y: view.frame.midY, width: 0, height: 0)
                            self.controller?.present(activityViewController, animated: true, completion: nil)
                            wasOpened = true
                        }
                    } else {
                        let rect = CGRect(x: 0, y: 0, width: view.frame.width, height: view.frame.height)
                        wasOpened = self.documentInteractionController.presentOpenInMenu(from: rect, in: view, animated: true)
                    }
                }
                
                if (wasOpened == false) {
                    print("Failed to open the file preview")
                    self.asyncReturnValue(functionName: functionName, result: "false")
                    return
                } else {
                    if (openWithDefault == false) {
                        self.asyncReturnValue(functionName: functionName, result: "true")
                        return
                    }
                }
            }
            
        } catch {
            print("FileOpenManager open error: \(error)")
            self.asyncReturnValue(functionName: functionName, result: "false")
            return
        }
    }
    
    public func documentInteractionControllerViewControllerForPreview(_ controller: UIDocumentInteractionController) -> UIViewController {
        var presentingViewController = self.controller;
        while (presentingViewController?.presentedViewController != nil && ((presentingViewController?.presentedViewController!.isBeingDismissed) != nil)) {
            presentingViewController = presentingViewController?.presentedViewController as? WebViewViewController;
        }
        return presentingViewController!;
    }
    
    public func documentInteractionControllerDidEndPreview(_ controller: UIDocumentInteractionController) {
        self.asyncReturnValue(functionName: self.functionName ?? "FileOpener", result: "true")
    }
}
