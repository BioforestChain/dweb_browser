//
//  FloatingPannelView.swift
//  FloatingDemo
//
//  Created by instinct on 2023/9/7.
//

import UIKit
import SwiftUI
import Combine

@objc(FloatingPannelView)
public class FloatingPannelView: UIView {
    
    let floatingParams = FloatingParams()
    
    lazy var floatingView: FloatingView = {
        return FloatingView(contentView: contentView, floatingParams: floatingParams)
    }()
            
    lazy var hostVC: UIHostingController<FloatingView> = {
        let hostVC = UIHostingController(rootView: floatingView)
        hostVC.view.frame = bounds
        hostVC.view.backgroundColor = .clear
        hostVC.view.isUserInteractionEnabled = true
        return hostVC
    }()
    
    var contentView: UIView
    
    private var cancellables = Set<AnyCancellable>()
    
    @objc
    public init(frame: CGRect, content: UIView) {
        contentView = content
        super.init(frame: frame)
        setupSubViews()
        
        floatingParams.$floating.sink { isFloating in
            print("sink:\(isFloating)")
        }.store(in: &cancellables)
        
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupSubViews() {
        ConsoleSwift.inject("setupSubViews")
        addSubview(hostVC.view)
    }
    
    @objc
    public func updateSize(_ size: CGSize) {
        floatingParams.width = size.width
        floatingParams.height = size.height
    }
    
    @objc
    public func updateFloating(_ isFloating: Bool) {
        floatingParams.floating = isFloating
    }
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        hostVC.view.frame = bounds
    }
        
    public override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        if floatingParams.floating {
            ConsoleSwift.inject("hitTest: floating: \(point)")
            return super.hitTest(point, with: event)
        }
                
        if floatingParams.rect.contains(point) {
            ConsoleSwift.inject("hitTest contain: \(point)")
            return super.hitTest(point, with: event)
        }
        ConsoleSwift.inject("hitTest: pass: \(point)")
        return nil
    }
}

extension FloatingPannelView {
    func testSize() {
       updateSize(CGSize(width: floatingParams.width + 20, height: floatingParams.height + 20))
    }
    
    func testFloating() {
        updateFloating(!floatingParams.floating)
    }
}


