//
//  SoundRecordManager.swift
//  DwebPlatformIosKit
//
//  Created by ios on 2024/1/29.
//

import Foundation
import UIKit
import SwiftUI

@objc public class SoundRecordManager: NSObject {
    
    @objc public func create() -> UIViewController {
        let soundController = UIHostingController(rootView: RecordHomeView())
        return soundController
    }
    
    @objc public func completeRecord(callback: @escaping (String) -> Void) {
        RecordManager.shared.completeCallback = callback
    }
    
    @objc public func completeSingleRecord(callback: @escaping (String) -> Void) {
        RecordManager.shared.completeSingleRecordCallback = callback
    }
    
    @objc public func createRecordController() -> UIViewController {
        let soundController = UIHostingController(rootView: SingleRecrodView())
        return soundController
    }
}
