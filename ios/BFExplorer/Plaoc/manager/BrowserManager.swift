//
//  BrowserManager.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/12/16.
//

import UIKit
import SafariServices

class BrowserManager: NSObject {

    private var safariController: SFSafariViewController?
    private var baseController: UIViewController?
    
    init(viewController: UIViewController) {
        super.init()
        baseController = viewController
    }
    
    func prepare(for url: URL, withTint tint: UIColor? = nil, modalPresentation style: UIModalPresentationStyle = .fullScreen) -> Bool {
        
        if safariController == nil, let scheme = url.scheme?.lowercased(), ["http","https"].contains(scheme) {
            safariController = SFSafariViewController(url: url)
            safariController?.delegate = self
            if let color = tint {
                safariController?.preferredBarTintColor = color
            }
            safariController?.modalPresentationStyle = style
            if style == .popover {
                DispatchQueue.main.async {
                    self.safariController?.popoverPresentationController?.delegate = self
                }
            }
            return true
        }
        return false
    }
    
    func openBrowserController() {
        guard safariController != nil else { return }
        baseController?.present(safariController!, animated: true)
    }
    
    func toolbarColor() -> String? {
        return safariController?.preferredBarTintColor?.hexString()
    }
    
    func presentationStyle() -> UIModalPresentationStyle? {
        return safariController?.modalPresentationStyle
    }
    
    func width() -> CGFloat {
        return safariController?.view.bounds.width ?? 0
    }
    
    func height() -> CGFloat {
        return safariController?.view.bounds.height ?? 0
    }
    
    func cleanup() {
        safariController = nil
    }
}

extension BrowserManager: SFSafariViewControllerDelegate {
    
    func safariViewControllerDidFinish(_ controller: SFSafariViewController) {
        
    }
    
    func safariViewController(_ controller: SFSafariViewController, didCompleteInitialLoad didLoadSuccessfully: Bool) {
        
    }
}

extension BrowserManager: UIPopoverPresentationControllerDelegate {
    
    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        safariController = nil
    }
    
    func popoverPresentationControllerDidDismissPopover(_ popoverPresentationController: UIPopoverPresentationController) {
        safariController = nil
    }
}
