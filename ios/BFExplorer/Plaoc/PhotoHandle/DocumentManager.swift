//
//  DocumentManager.swift
//  DocumentPicker
//
//  Created by hb on 04/06/20.
//  Copyright © 2020 hb. All rights reserved.
//

import UIKit

let PATH_SELECTED_FILES = "selected_files"
typealias DocumentFetchCompletion = ((_ isFileSelected: Bool, _ fileName: String, _ fileExtension: String, _ filePath: String) -> Void)?

class DocumentManager: NSObject {
    
    //shared instance
    static let sharedInstance = DocumentManager()
    
    //Documnet Fetch CompletionHandler
    private var documnetFetchCompletionHandler: DocumentFetchCompletion!
    
    // controller from where we present UIDocumentMenuViewController
    private var parentController: UIViewController?
    
    ///to show dodument menu picker
    ///on completion block you will get below paramerts
    /// - parameters:
    ///     - parentController: Parent viewController of DocumentMenuController
    ///     - isFileSelected:: is user select file it will be true and if user cancel then it will return false
    ///     - fileName:: actual file name cloud
    ///     - fileExtension:: file extension
    ///     - filePath:: after user slected image we are saving that file in to app cache directory and this object will have that path
    func showDocumentMenuController(_ parentController:  UIViewController, complitionhandler:@escaping (_ isFileSelected: Bool, _ fileName: String, _ fileExtension: String, _ filePath: String) -> Void) {
        self.documnetFetchCompletionHandler = complitionhandler
        self.parentController = parentController
        
        self.deleteFolderInCacheFolder()
        self.createFolderInCacheFolder()
        
        let documentPicker = UIDocumentPickerViewController.init(documentTypes: ["public.data"], in: .import)
        documentPicker.delegate = self
        documentPicker.modalPresentationStyle = .popover
        self.parentController?.present(documentPicker, animated: true, completion: nil)
    }
    
}
//MARK: - Document picker delegate methods
extension DocumentManager : UIDocumentPickerDelegate {
    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentAt url: URL) {
        if controller.documentPickerMode == .import {
            let coordinator = NSFileCoordinator()
            
            coordinator.coordinate(readingItemAt: url, options: .withoutChanges, error: nil, byAccessor: { (url) in
                let fileName = url.deletingPathExtension().lastPathComponent
                let fileExtension = url.pathExtension
                let desinationPath = self.getSelectedFilesFolder()?.appendingPathComponent(fileName).appendingPathExtension(fileExtension)
                let fileManager = FileManager.default
                do {
                    try fileManager.copyItem(at: url, to: desinationPath!)
                    if self.documnetFetchCompletionHandler != nil {
                        self.documnetFetchCompletionHandler!(true, fileName, fileExtension, desinationPath!.path)
                    }
                } catch {
                    if self.documnetFetchCompletionHandler != nil {
                        self.documnetFetchCompletionHandler!(false, "", "", "")
                    }
                }
            })
        }
    }
    ///Gets called when document picker was cancelled
    /// - parameters:
    ///     - controller: DocumentPickerViewController
    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        if self.documnetFetchCompletionHandler != nil {
            self.documnetFetchCompletionHandler!(false, "", "", "")
        }
    }
}

//MARK: - Document menu delegate methods
extension DocumentManager: UIDocumentMenuDelegate {
    func documentMenu(_ documentMenu: UIDocumentMenuViewController, didPickDocumentPicker documentPicker: UIDocumentPickerViewController) {
        documentPicker.delegate = self
        self.parentController?.present(documentPicker, animated: true, completion: nil)
    }
}

extension DocumentManager {
    //MARK: - Create folder in Cache
    ///crate one folder where  selected file will be saved
    private func createFolderInCacheFolder() {
        let fileManager = FileManager.default
        if let tDocumentDirectory = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first {
            let filePath =  tDocumentDirectory.appendingPathComponent(PATH_SELECTED_FILES)
            if !fileManager.fileExists(atPath: filePath.path) {
                do {
                    try fileManager.createDirectory(atPath: filePath.path, withIntermediateDirectories: true, attributes: nil)
                } catch {
                    NSLog("Couldn't create document directory")
                }
            }
            NSLog("Document directory is \(filePath)")
        }
    }
    //MARK: - Delete selected folder in Cache
    ///delete selected_files folder
    private func deleteFolderInCacheFolder() {
        let fileManager = FileManager.default
        if let tDocumentDirectory = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first {
            let filePath =  tDocumentDirectory.appendingPathComponent(PATH_SELECTED_FILES)
            if fileManager.fileExists(atPath: filePath.path) {
                do {
                    try fileManager.removeItem(at: filePath)
                } catch {
                    NSLog("Couldn't delete directory")
                }
            }
        }
    }
    //MARK: - Get selected file's folder path
    ///returns path of selected_files folder
    private func getSelectedFilesFolder() -> URL? {
        let fileManager = FileManager.default
        if let tDocumentDirectory = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first {
            let filePath =  tDocumentDirectory.appendingPathComponent(PATH_SELECTED_FILES)
            return filePath
        }
        return nil
    }
}
