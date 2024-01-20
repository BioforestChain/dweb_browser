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
        setupBrowser()
    }
        
    public required init?(coder: NSCoder) {
        fatalError("")
    }
    
    deinit {
        Log("DwebWebView deinit")
    }
    
    fileprivate var isTransitionEffect = false
    fileprivate var snap: UIView?
    
    private lazy var browserStates: BrowserViewStates = {
        BrowserViewStates()
    }()
    
    private lazy var hostVC: UIViewController = UIHostingController(rootView: BrowserView(states: browserStates, toolBarState: browserStates.toolBarState))
    
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
        
    fileprivate func setupContainerView() {
        addFullSize(to: self, subView: browserContainerView)
    }
    
    func setupBrowser() {
        hostVC = UIHostingController(rootView: BrowserView(states: browserStates, toolBarState: browserStates.toolBarState))
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
        browserStates.doSearch(key)
    }
    
    @objc func colorSchemeChanged(color: Int32) {
        browserStates.updateColorScheme(newScheme: Int(color))
    }
    
    @objc func gobackIfCanDo() -> Bool {
        return browserStates.doBackIfCan()
    }
    
    @objc func browserClear() {
        isTransitionEffect = false
        snap = nil
    }
    
    @objc func browserActive(on: Bool) {
        if on == false {
            isTransitionEffect = true
            snap = hostVC.view.snapshotView(afterScreenUpdates: false)
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
