//
//  CodeScanner.swift
//  DwebBrowser
//
//  Created by ui03 on 2023/7/11.
//

import AVFoundation
import SwiftUI

public enum ScanError: Error {
    /// The camera could not be accessed.
    case badInput

    /// The camera was not capable of scanning the requested codes.
    case badOutput

    /// Initialization failed.
    case initError(_ error: Error)
  
    /// The camera permission is denied
    case permissionDenied
}

public struct ScanResult {
    /// The contents of the code.
    public let string: String

    /// The type of code that was matched.
    let type: AVMetadataObject.ObjectType
    
    /// The image of the code that was matched
    let image: UIImage?
  
    /// The corner coordinates of the scanned code.
    let corners: [CGPoint]
}

/// The operating mode for CodeScannerView.
public enum ScanMode {
    /// Scan exactly one code, then stop.
    case once

    /// Scan each code no more than once.
    case oncePerCode

    /// Keep scanning all codes until dismissed.
    case continuous

    /// Scan only when capture button is tapped.
    case manual
}
