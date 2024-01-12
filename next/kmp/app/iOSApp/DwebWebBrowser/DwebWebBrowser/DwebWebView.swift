//
//  DwebWebView.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/1/11.
//

import UIKit
import SwiftUI

var browserViewDelegate: WebBrowserViewDelegate {
    return DwebWebView.delegate ?? WebBrowserDefaultProvider(trackModel: false)
}

var browserViewDataSource: WebBrowserViewDataSource {
    return DwebWebView.dataSource ?? WebBrowserDefaultProvider(trackModel: false)
}

@objc public class DwebWebView: UIView {
    
    fileprivate static weak var delegate: WebBrowserViewDelegate? = nil
    fileprivate static weak var dataSource: WebBrowserViewDataSource? = nil
    
    @objc public init(frame: CGRect, delegate: WebBrowserViewDelegate?, dataSource: WebBrowserViewDataSource?) {
        super.init(frame: frame)
        DwebWebView.delegate = delegate
        DwebWebView.dataSource = dataSource
        setupContainerView()
        Log("")
    }
    
    public required init?(coder: NSCoder) {
        fatalError("")
    }
    
    private var hostVC: UIViewController?
    var isTransitionEffect = false
    var snap: UIView?
    
    var browerView: BrowserView? {
        if let hostingController = hostVC as? UIHostingController<BrowserView> {
            return hostingController.rootView
        }
        return nil
    }
    
    lazy var browserContainerView: UIView = {
        let v = UIView(frame: CGRect.zero)
        v.backgroundColor = .yellow
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()
    
    lazy var blurView: UIView = {
        let blurView = UIView(frame: .zero)
        blurView.backgroundColor = .black.withAlphaComponent(0.5)
        return blurView
    }()
        
    func setupContainerView() {
        addSubview(browserContainerView)
        NSLayoutConstraint.activate([
            browserContainerView.leadingAnchor.constraint(equalTo: leadingAnchor),
            browserContainerView.trailingAnchor.constraint(equalTo: trailingAnchor),
            browserContainerView.topAnchor.constraint(equalTo: topAnchor),
            browserContainerView.bottomAnchor.constraint(equalTo: bottomAnchor),
        ])
    }
    
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

public extension DwebWebView {
    @objc func doSearch(key: String) {
        browerView?.states.doSearch(key)
    }
    
    @objc func colorSchemeChanged(color: Int32) {
        browerView?.states.updateColorScheme(newScheme: Int(color))
    }
    
    @objc func gobackIfCanDo() -> Bool {
        guard let brower = browerView else { return false }
        return brower.states.doBackIfCan()
    }
    
    @objc func browserClear() {
        isTransitionEffect = false
        browerView?.states.clear()
        browserContainerView.subviews.forEach { $0.removeFromSuperview() }
        hostVC = nil
    }
    
    @objc func browserActive(on: Bool) {
        if on == false {
            isTransitionEffect = true
            snap = hostVC?.view.snapshotView(afterScreenUpdates: false)
        }
    }
    
    @objc func prepareToKmp() {
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
    }
}
