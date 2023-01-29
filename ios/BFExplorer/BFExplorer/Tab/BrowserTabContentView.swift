//
//  BrowserTabContentView.swift
//  Browser
//
//    13.    
//

import UIKit
import WebKit
import SnapKit

class BrowserTabContentView: UIView {
    let webView = WKWebView()
    let homePageView = BrowserTabHomeView()
    let statusBarBackgroundView = StatusBarBackgroundView()
    var statusBarBackgroundViewHeightConstraint: Constraint?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        var statusBarHeight = 0.0
        if #available(iOS 13.0, *) {
            statusBarHeight = Double(window?.windowScene?.statusBarManager?.statusBarFrame.height ?? 0.0) + 5
        } else {
            // Fallback on earlier versions
        }
        statusBarBackgroundViewHeightConstraint?.update(offset: statusBarHeight)
    }
}
 
// MARK: Helper methods
private extension BrowserTabContentView {
    func setupView() {
        backgroundColor = .white
        layer.masksToBounds = false
        setupShadow()
        setupWebView()
        setupHomeStateView()
        setupStatusBarBackgroundView()
    }
    
    func setupShadow() {
        layer.masksToBounds = false
        layer.shadowColor = UIColor.lightGray.cgColor
        layer.shadowOffset = CGSize(width: 0, height: 0)
        layer.shadowOpacity = 0.5
        layer.shadowRadius = 15
    }
    
    func setupWebView() {
        webView.allowsBackForwardNavigationGestures = true
        if #available(iOS 13.0, *) {
            webView.scrollView.automaticallyAdjustsScrollIndicatorInsets = false
        } else {
            // Fallback on earlier versions
        }
        webView.scrollView.contentInset = .zero
        webView.scrollView.layer.masksToBounds = false
        addSubview(webView)
        webView.snp.makeConstraints {
            $0.top.equalTo(safeAreaLayoutGuide)
            $0.leading.bottom.trailing.equalToSuperview()
        }
        
    }
    
    func setupStatusBarBackgroundView() {
        statusBarBackgroundView.backgroundColor =  .white
        addSubview(statusBarBackgroundView)
        statusBarBackgroundView.snp.makeConstraints {
            $0.top.leading.trailing.equalToSuperview()
            statusBarBackgroundViewHeightConstraint = $0.height.equalTo(0).constraint
        }
    }
    
    func setupHomeStateView() {
        
        homePageView.alpha = 0
        addSubview(homePageView)
        homePageView.snp.makeConstraints {
            $0.top.equalTo(safeAreaLayoutGuide)
            $0.leading.bottom.trailing.equalToSuperview()
        }
    }
}
