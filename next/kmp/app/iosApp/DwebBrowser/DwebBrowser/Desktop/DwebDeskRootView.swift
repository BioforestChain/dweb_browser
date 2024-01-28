//
//  ComposeRootView.swift
//  DwebBrowser
//
//  Created by instinct on 2023/11/27.
//  Copyright © 2023 orgName. All rights reserved.
//

import DwebShared
import Foundation
import SwiftUI
import UIKit

class DwebComposeRootViewController: UIViewController {
    private var rightEdgePan: UIScreenEdgePanGestureRecognizer!
    private var leftEdgePan: UIScreenEdgePanGestureRecognizer!
    
    init(vcs: [UIViewController]) {
        super.init(nibName: nil, bundle: nil)
        setupTouchDispatchView()
        updateContent(vcs)
        
        addEdgePanGesture()
    }
    
    private func addEdgePanGesture() {
        leftEdgePan = UIScreenEdgePanGestureRecognizer(target: self, action: #selector(edgeAction(ges:)))
        leftEdgePan.edges = .left
        leftEdgePan.delegate = self
        view.addGestureRecognizer(leftEdgePan)
        
        rightEdgePan = UIScreenEdgePanGestureRecognizer(target: self, action: #selector(edgeAction(ges:)))
        rightEdgePan.edges = .right
        rightEdgePan.delegate = self
        view.addGestureRecognizer(rightEdgePan)
    }
    
    @objc private func edgeAction(ges: UIGestureRecognizer) {
        if ges is UIScreenEdgePanGestureRecognizer {
            let recognizer = ges as! UIScreenEdgePanGestureRecognizer
            switch recognizer.state {
            case .ended:
                Main_iosKt.dwebViewController.emitOnGoBack()
            default:
                break
            }
        }
    }
    
    func setupTouchDispatchView() {
        addFullSreen(subView: touchDispatchView, to: view)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init no iomplememnte") }

    private let touchDispatchView = TouchThroughView(frame: .zero)
    
    func updateContent(_ vcs: [UIViewController]) {
        Log("\(vcs)")
        
        children.forEach { childVc in
            if vcs.contains(childVc) { return }
            childVc.removeFromParent()
            childVc.view.removeFromSuperview()
            childVc.didMove(toParent: nil)
        }
        
        vcs.forEach { vc in
            if !children.contains(vc) {
                addChild(vc)
                addFullSreen(subView: vc.view, to: touchDispatchView)
                vc.didMove(toParent: self)
            }
            
            touchDispatchView.bringSubviewToFront(vc.view)
        }
    }
    
    func addFullSreen(subView: UIView, to container: UIView) {
        container.addSubview(subView)
        subView.backgroundColor = .clear
        
        /// 填充父级视图
        subView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            subView.topAnchor.constraint(equalTo: container.topAnchor),
            subView.bottomAnchor.constraint(equalTo: container.bottomAnchor),
            subView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            subView.trailingAnchor.constraint(equalTo: container.trailingAnchor),
        ])
    }
}

struct DwebDeskRootView: UIViewControllerRepresentable {
    var vcs: [UIViewController]
    
    func makeUIViewController(context: Context) -> DwebComposeRootViewController {
        return DwebComposeRootViewController(vcs: vcs)
    }

    func updateUIViewController(_ uiViewController: DwebComposeRootViewController, context: Context) {
        uiViewController.updateContent(vcs)
    }
}

extension DwebComposeRootViewController: UIGestureRecognizerDelegate {
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldBeRequiredToFailBy otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        if otherGestureRecognizer == leftEdgePan || otherGestureRecognizer == rightEdgePan {
            return true
        }
        return false
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        return gestureRecognizer.isKind(of: UIScreenEdgePanGestureRecognizer.self)
    }
}
