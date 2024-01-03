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
            
    lazy var browserContainerView: UIView = {
        let v = UIView(frame: CGRect.zero)
        v.backgroundColor = .yellow
        return v
    }()
    
    private var hostVC: UIViewController?
    
    lazy var blurView: UIView = {
        let blurView = UIView(frame: .zero)
        blurView.backgroundColor = .black.withAlphaComponent(0.5)
        return blurView
    }()
    
    var isTransitionEffect = false
    var snap: UIView? = nil
    
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
        BrowserViewStateStore.shared.doSearch(key)
    }
    
    
    func gobackIfCanDo() -> Bool {
        return BrowserViewStateStore.shared.doBackIfCan()
    }
    
    func isDarkColorScheme(isDark: Bool) {
        if isDark {
            BrowserViewStateStore.shared.colorScheme = .dark
        } else {
            BrowserViewStateStore.shared.colorScheme = .light
        }
    }
    
    func browserClear() {
        isTransitionEffect = false
        BrowserViewStateStore.shared.clear()
        browserContainerView.subviews.forEach { $0.removeFromSuperview() }
        hostVC = nil
    }
    
    func browserActive(on: Bool){
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
