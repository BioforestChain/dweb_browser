//
//  DwebBrowserIosIMP.swift
//  iosApp
//
//  Created by instinct on 2023/12/7.
//  Copyright © 2023 orgName. All rights reserved.
//

import DwebShared
import SwiftUI
import UIKit

class DwebBrowserIosIMP {
    static let shared = DwebBrowserIosIMP()
    private var hostVC: UIViewController?
    var isTransitionEffect = false
    var snap: UIView?
    
    var browerView: BrowserView? { hostVC?.view as? BrowserView }
    
    lazy var browserContainerView: UIView = {
        let v = UIView(frame: CGRect.zero)
        v.backgroundColor = .yellow
        return v
    }()
    
    lazy var blurView: UIView = {
        let blurView = UIView(frame: .zero)
        blurView.backgroundColor = .black.withAlphaComponent(0.5)
        return blurView
    }()
        
    func createBrowser() {
        hostVC = UIHostingController(rootView: BrowserView())
    }
    
    func containerAdd(subView: UIView?) {
        guard let subView = subView else { return }
        subView.translatesAutoresizingMaskIntoConstraints = false
        browserContainerView.addSubview(subView)
        NSLayoutConstraint.activate([
            subView.leadingAnchor.constraint(equalTo: browserContainerView.leadingAnchor),
            subView.trailingAnchor.constraint(equalTo: browserContainerView.trailingAnchor),
            subView.topAnchor.constraint(equalTo: browserContainerView.topAnchor),
            subView.bottomAnchor.constraint(equalTo: browserContainerView.bottomAnchor),
        ])
    }
}

extension DwebBrowserIosIMP: BrowserIosInterface {
    func doSearch(key: String) {
        browerView?.store.doSearch(key)
    }
    
    func colorSchemeChanged(color: Int32) {
        browerView?.store.updateColorScheme(newScheme: Int(color))
    }
    
    func gobackIfCanDo() -> Bool {
        guard let brower = browerView else { return false }
        return brower.store.doBackIfCan()
    }
    
    func browserClear() {
        isTransitionEffect = false
        browerView?.store.clear()
        browserContainerView.subviews.forEach { $0.removeFromSuperview() }
        hostVC = nil
    }
    
    func browserActive(on: Bool) {
        if on == false {
            isTransitionEffect = true
            snap = hostVC?.view.snapshotView(afterScreenUpdates: false)
        }
    }
    
    func getBrowserView() -> UIView {
        if browserContainerView.subviews.isEmpty {
            createBrowser()
            containerAdd(subView: hostVC?.view)
        }
        
        if isTransitionEffect {
            if let snap = snap {
                containerAdd(subView: snap)
                containerAdd(subView: blurView)
                hostVC?.view.removeFromSuperview()
                
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
                    guard let self = self else { return }
                    UIView.animate(withDuration: 0.3) {
                        self.containerAdd(subView: self.hostVC?.view)
                        snap.removeFromSuperview()
                        self.blurView.alpha = 0.0
                    } completion: { _ in
                        self.blurView.removeFromSuperview()
                        self.blurView.alpha = 1.0
                    }
                }
            }
            isTransitionEffect = false
        }
        
        return browserContainerView
    }
}
