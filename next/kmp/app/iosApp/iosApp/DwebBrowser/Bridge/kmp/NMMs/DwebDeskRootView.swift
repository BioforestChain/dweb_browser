//
//  ComposeRootView.swift
//  iosApp
//
//  Created by instinct on 2023/11/27.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import UIKit
import SwiftUI

class DwebComposeRootViewController: UIViewController {
    
    init(vcs: [UIViewController]) {
        super.init(nibName: nil, bundle: nil)
        setupTouchDispatchView()
        updateContent(vcs)
    }
    
    func setupTouchDispatchView () {
        addFullSreen(subView: touchDispatchView, to: view)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init no iomplememnte") }

    private let touchDispatchView = TouchThroughView(frame: .zero)
    
    func updateContent(_ vcs: [UIViewController]) {
        Log("\(vcs)")
        
        children.forEach {
            $0.removeFromParent()
            $0.view.removeFromSuperview()
            $0.didMove(toParent: nil)
        }
        
        vcs.forEach { vc in
            addChild(vc)
            addFullSreen(subView: vc.view, to: touchDispatchView)
            vc.didMove(toParent: self)
        }
    }
    
    func addFullSreen(subView: UIView, to container: UIView) {
        container.addSubview(subView)
        subView.backgroundColor = .clear
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
