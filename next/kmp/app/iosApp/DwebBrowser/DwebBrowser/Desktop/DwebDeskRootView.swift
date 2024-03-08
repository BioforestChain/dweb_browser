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

    init(vcs: [UIViewController]) {
        super.init(nibName: nil, bundle: nil)
        setupTouchDispatchView()
        updateContent(vcs)

        addEdgePanGesture()
    }

    private func addEdgePanGesture() {
        recongizer = DwebScreenEdgePanGestureRecognizer(view, tigger: {
            Main_iosKt.dwebViewController.emitOnGoBack()
        })
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

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            DwebLifeStatusCenter.shared.postDidRendedNotification()
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
