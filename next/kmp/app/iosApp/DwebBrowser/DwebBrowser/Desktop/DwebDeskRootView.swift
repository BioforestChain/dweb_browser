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
    private var leftAnimationView: EdgeAnimationView? = nil
    private var rightAnimationView: EdgeAnimationView? = nil
    private var leftEdgePanGesture: UIScreenEdgePanGestureRecognizer? = nil
    private var rightEdgePanGesture: UIScreenEdgePanGestureRecognizer? = nil

    var vcds: [DwebVCData]
    
    init(vcds: [DwebVCData]) {
        self.vcds = vcds
        super.init(nibName: nil, bundle: nil)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupTouchDispatchView()
        updateContent(vcds)
    }
    
    func updateEdgeSwipeEnable(enable: Bool){
        if enable{
            configScreenEdgeGesture()
        }else{
            removeEdgeGesture()
        }
    }
    
    func setupTouchDispatchView() {
        addViewToContainer(subView: touchDispatchView, to: view, fullscreen: true)
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init no iomplememnte") }
    
    private let touchDispatchView = TouchThroughView(frame: .zero)
    
    func updateContent(_ vcds: [DwebVCData]) {
        Log("\(vcds)")
        
        children.forEach { childVc in
            if vcds.contains(where: {vcd in vcd.vc == childVc}) { return }
            childVc.removeFromParent()
            childVc.view.removeFromSuperview()
            childVc.didMove(toParent: nil)
        }
        
        vcds.forEach { vcd in
            if !children.contains(vcd.vc) {
                addChild(vcd.vc)
                addViewToContainer(subView: vcd.vc.view, to: touchDispatchView, fullscreen: vcd.prop.fullscreen)
                vcd.vc.didMove(toParent: self)
            }
            
            if vcd.prop.visible {
                touchDispatchView.bringSubviewToFront(vcd.vc.view)
            } else {
                touchDispatchView.sendSubviewToBack(vcd.vc.view)
            }
            
            vcd.vc.view.isHidden = !vcd.prop.visible
            vcd.vc.view.isUserInteractionEnabled = vcd.prop.visible
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            DwebLifeStatusCenter.shared.postDidRendedNotification()
        }
    }
    func addViewToContainer(subView: UIView, to container: UIView, fullscreen: Bool) {
        container.addSubview(subView)
        // subView.isOpaque = false
        subView.backgroundColor = .clear
        
        /// 填充父级视图
        if fullscreen {
            subView.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                subView.topAnchor.constraint(equalTo: container.topAnchor),
                subView.bottomAnchor.constraint(equalTo: container.bottomAnchor),
                subView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
                subView.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            ])
        }
    }
}

struct DwebDeskRootView: UIViewControllerRepresentable {
    var deskVCStore: DwebDeskVCStore
    init(deskVCStorex: DwebDeskVCStore) {
        self.deskVCStore = deskVCStorex
    }
    
    func makeUIViewController(context: Context) -> DwebComposeRootViewController {
        let rootVC = DwebComposeRootViewController(vcds: self.deskVCStore.vcs)
        rootVC.updateEdgeSwipeEnable(enable: deskVCStore.shouldEnableEdgeSwipe)
        return rootVC
    }
    
    func updateUIViewController(_ uiViewController: DwebComposeRootViewController, context: Context) {
        uiViewController.updateContent(self.deskVCStore.vcs)
        if context.coordinator.previousShouldEnableEdgeSwipe != deskVCStore.shouldEnableEdgeSwipe {
            uiViewController.updateEdgeSwipeEnable(enable: deskVCStore.shouldEnableEdgeSwipe)
            context.coordinator.previousShouldEnableEdgeSwipe = deskVCStore.shouldEnableEdgeSwipe
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator()
    }
    
    class Coordinator {
        var previousShouldEnableEdgeSwipe: Bool = false
    }
}

//屏幕边缘手势相关
extension DwebComposeRootViewController: UIGestureRecognizerDelegate {
    @objc func configScreenEdgeGesture(){
        removeEdgeGesture()
        
        leftAnimationView = EdgeAnimationView(frame: CGRect(x: 0, y: 300, width: 0, height: 200), rectEdge: .left) {
            Main_iosKt.dwebViewController.emitOnGoBack()
        }
        rightAnimationView = EdgeAnimationView(frame: CGRect(x: view.bounds.width, y: 300, width: 0, height: 200), rectEdge: .right) {
            Main_iosKt.dwebViewController.emitOnGoBack()
        }
        if (leftAnimationView != nil) {
            view.addSubview(leftAnimationView!)
            leftEdgePanGesture = setupEdgePanGesture(target: leftAnimationView!, edges: .left)

        }
        if (rightAnimationView != nil) {
            view.addSubview(rightAnimationView!)
            rightEdgePanGesture = setupEdgePanGesture(target: rightAnimationView!, edges: .right)
        }
    }
    
    func removeEdgeGesture(){
        leftAnimationView?.removeFromSuperview()
        leftAnimationView = nil
        rightAnimationView?.removeFromSuperview()
        rightAnimationView = nil
        
        if let gesture1 = leftEdgePanGesture {
            view.removeGestureRecognizer(gesture1)
            leftEdgePanGesture = nil
        }
        if let gesture2 = rightEdgePanGesture {
            view.removeGestureRecognizer(gesture2)
            rightEdgePanGesture = nil
        }
    }
    
    
    private func setupEdgePanGesture(target: EdgeAnimationView, edges: UIRectEdge) -> UIScreenEdgePanGestureRecognizer{
        let edgePan = UIScreenEdgePanGestureRecognizer(target: target, action: #selector( target.handlePanGesture))
        edgePan.edges = edges
        edgePan.delegate = self
        view.addGestureRecognizer(edgePan)
        return edgePan
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldBeRequiredToFailBy otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        if gestureRecognizer is UIScreenEdgePanGestureRecognizer {
            return true
        }
        return false
    }
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        return gestureRecognizer is UIScreenEdgePanGestureRecognizer || otherGestureRecognizer is UIScreenEdgePanGestureRecognizer
    }
    
}
