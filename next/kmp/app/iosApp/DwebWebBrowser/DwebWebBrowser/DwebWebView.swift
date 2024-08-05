//
//  DwebWebView.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/1/11.
//

import UIKit
import SwiftUI

var browserViewDelegate: WebBrowserViewDelegate {
    assert(DwebWebView.delegate != nil, "不应该在DwebWebView.delegate为空的时候，获取browserViewDelegate")
    return DwebWebView.delegate ?? WebBrowserDefaultProvider(trackModel: false)
}

var browserViewDataSource: WebBrowserViewDataSource {
    assert(DwebWebView.dataSource != nil, "不应该在DwebWebView.dataSource为空的时候，获取browserViewDataSource")
    return DwebWebView.dataSource ?? WebBrowserDefaultProvider(trackModel: false)
}

@objc public class DwebWebView: UIView {
    
    fileprivate static weak var delegate: WebBrowserViewDelegate? = nil
    fileprivate static weak var dataSource: WebBrowserViewDataSource? = nil
    
    @objc public init(frame: CGRect, delegate: WebBrowserViewDelegate?, dataSource: WebBrowserViewDataSource?) {
        DwebWebView.delegate = delegate
        DwebWebView.dataSource = dataSource
        super.init(frame: frame)
        setupView()
        accessibilityIdentifier = "Web browser"
    }
        
    public required init?(coder: NSCoder) {
        fatalError("")
    }
    
    deinit {
        Log("DwebWebView deinit")
        browerView?.destroy()
    }
    
    fileprivate var isTransitionEffect = false
    fileprivate var snap: UIView?
    
    var browerView: BrowserView? {
        if let hostingController = hostVC as? UIHostingController<BrowserView> {
            return hostingController.rootView
        }
        return nil
    }
    
    private lazy var hostVC: UIViewController = UIHostingController(rootView: BrowserView())
    
    private lazy var browserContainerView: UIView = {
        let v = UIView(frame: CGRect.zero)
        v.backgroundColor = .yellow
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()
    
    private lazy var blurView: UIView = {
        let blurView = UIView(frame: .zero)
        blurView.backgroundColor = .black.withAlphaComponent(0.5)
        return blurView
    }()
        
    fileprivate func setupView() {
        addFullSize(to: self, subView: browserContainerView)
        addFullSize(to: browserContainerView, subView: hostVC.view)
    }
    
    func addFullSize(to: UIView, subView: UIView?) {
        guard let subView = subView else { return }
        subView.translatesAutoresizingMaskIntoConstraints = false
        to.addSubview(subView)
        NSLayoutConstraint.activate([
            subView.leadingAnchor.constraint(equalTo: to.leadingAnchor),
            subView.trailingAnchor.constraint(equalTo: to.trailingAnchor),
            subView.topAnchor.constraint(equalTo: to.topAnchor),
            subView.bottomAnchor.constraint(equalTo: to.bottomAnchor),
        ])
    }

}

public extension DwebWebView {
    @objc func doSearch(key: String) {
        browerView?.searchFromOutside(outerSearchKey: key)
    }
    
    @objc func doNewTabUrl(url: String, target: String) {
        browerView?.doNewTabUrl(url: url, target: target)
    }

    @objc func loadPullMenuConfig(isActived: Bool) {
        browerView?.loadPullMenuConfig(isActived: isActived)
    }

    @objc func colorSchemeChanged(color: Int32) {
        browerView?.updateColorScheme(color: Int(color))
    }
    
    @objc func gobackIfCanDo() -> Bool {
        guard let browser = browerView else { return false }
        return browser.gobackIfCanDo()
    }
  
    @objc func browserClear() {
        isTransitionEffect = false
        snap = nil
        browerView?.resetStates()
    }
    
    @objc func browserActive(on: Bool) {
        if on == false {
            DispatchQueue.main.async { [weak self] in
                self?.isTransitionEffect = true
                self?.snap = self?.hostVC.view.snapshotView(afterScreenUpdates: false)
            }
        }
    }
    
    @objc func prepareToKmp() {
        if isTransitionEffect {
            if let snap = snap {
                addFullSize(to: browserContainerView, subView: snap)
                addFullSize(to: browserContainerView, subView: blurView)
                hostVC.view.removeFromSuperview()
                
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
                    guard let self = self else { return }
                    UIView.animate(withDuration: 0.3) {
                        self.addFullSize(to: self.browserContainerView, subView: self.hostVC.view)
                        snap.removeFromSuperview()
                        self.blurView.alpha = 0.0
                    } completion: { _ in
                        self.blurView.removeFromSuperview()
                        self.blurView.alpha = 1.0
                    }
                }
            }
            snap = nil
            isTransitionEffect = false
        }
    }
}
