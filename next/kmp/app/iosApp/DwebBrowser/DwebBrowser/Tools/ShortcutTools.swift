//
//  QuickActionTools.swift
//  DwebBrowser
//
//  Created by instinct on 2024/1/29.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import DwebShared
import UIKit
import DwebWebBrowser
import SwiftUI

struct ShortcutTools {
    static func hand(_ item: UIApplicationShortcutItem) -> Bool {
        if DwebShortcutHandler().isScanShortcut(shortcut: item) {
            DwebLifeStatusCenter.shared.register(.didRended) {
                DispatchQueue.main.async {
                    let keyWindow = UIApplication.shared.currentWindow
                    let scanView = CodeScannerView(codeTypes: [.qr], showViewfinder: true) { result in
                        guard case let .success(qrCode) = result else { return }
                        Log("\(qrCode.string)")
                        DwebDeepLink.shared.openDeepLink(url: qrCode.string)
                        if let vc = keyWindow?.rootViewController?.presentedViewController as? UIHostingController<CodeScannerView> {
                            vc.dismiss(animated: true)
                        }
                    }
                    let hostVC = UIHostingController(rootView: scanView)
                    keyWindow?.rootViewController?.present(hostVC, animated: true)
                }
            }
            return true
        } else {
            return DwebShortcutHandler().hand(shortcut: item)
        }
    }
}
