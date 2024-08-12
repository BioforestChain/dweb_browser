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
    private var recongizer: DwebScreenEdgePanGestureRecognizer? = nil

    init(vcds: [DwebVCData]) {
        super.init(nibName: nil, bundle: nil)
        setupTouchDispatchView()
        updateContent(vcds)

        addEdgePanGesture()
    }

    private func addEdgePanGesture() {
        recongizer = DwebScreenEdgePanGestureRecognizer(view, tigger: {
            Main_iosKt.dwebViewController.emitOnGoBack()
        })
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
    var vcds: [DwebVCData]

    func makeUIViewController(context: Context) -> DwebComposeRootViewController {
        return DwebComposeRootViewController(vcds: vcds)
    }

    func updateUIViewController(_ uiViewController: DwebComposeRootViewController, context: Context) {
        uiViewController.updateContent(vcds)
    }
}
