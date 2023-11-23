//
//  CommonVCWrapView.swift
//  iosApp
//
//  Created by instinct on 2023/11/17.
//  Copyright © 2023 orgName. All rights reserved.
//

import DwebShared
import ObjectiveC
import SwiftUI
import UIKit

let COMPOSE_WINDOW_VIEW = 3389
extension UIView {
    // 替换的新方法
    @objc func myPointInside(inside point: CGPoint, with event: UIEvent?) -> Bool {
        if tag == COMPOSE_WINDOW_VIEW {
            var isInside = false
            for (index, subview) in subviews.enumerated() {
                if subview.isHidden { continue }
                let subviewpoint = convert(point, to: subview)
                isInside = subview.point(inside: subviewpoint, with: event)
                print("  \(index). myPointInside.subview subview=\(subview) isInside=\(isInside)")
                if isInside {
                    break
                }
            }
            return isInside
        } else {
            return myPointInside(inside: point, with: event)
        }
    }
}

// 在加载时替换方法实现
extension UIView {}

struct CommonVCWrapView: UIViewControllerRepresentable {
    let vc: UIViewController
    let prop: HelperPlatformDwebUIViewControllerProperty
    static func processComposeWindow(_ vc: UIViewController) {
        for (index, subview) in vc.view.subviews.enumerated() {
            for (index2, subview2) in subview.subviews.enumerated() {
                /// 如果compose中的视图不再捕捉手势，那么这个容器也不再捕捉手势，让手势自然穿透
                if subview2.tag == 257 {
                    subview.isHidden = true
                }
            }
        }
    }

    func makeUIViewController(context: Context) -> UIViewController {
        vc.view.backgroundColor = .clear
        if prop.vcId == 1 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                CommonVCWrapView.processComposeWindow(vc)
            }
        }

        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

class TouchUIViewController: UIViewController {
    private static func pointHook() {
        // 原先的实现
        let originalMethod = class_getInstanceMethod(UIView.self, #selector(UIView.point(inside:with:)))
        // 新的实现
        let swizzledMethod = class_getInstanceMethod(UIView.self, #selector(UIView.myPointInside(inside:with:)))

        // 交换实现
        method_exchangeImplementations(originalMethod!, swizzledMethod!)
    }

    static let shared = TouchUIViewController(vcs: [])
    init(vcs: [UIViewController]) {
        TouchUIViewController.pointHook()

        super.init(nibName: nil, bundle: nil)
        performUpdate(vcs)
        view.isOpaque = false
        let label = UILabel()
        label.text = "nih"
        label.frame = CGRect(x: 0, y: 100, width: 100, height: 20)
        label.backgroundColor = .red
        view.addSubview(label)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init no iomplememnte") }

    func performUpdate(_ vcs: [UIViewController]) {
        for vc in vcs.reversed() {
            if children.contains(vc) {
                view.bringSubviewToFront(vc.view)
                continue
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                CommonVCWrapView.processComposeWindow(vc)
            }
            addChild(vc)
            view.addSubview(vc.view)
            vc.view.tag = COMPOSE_WINDOW_VIEW
            vc.view.frame = view.bounds
            vc.view.didMoveToSuperview()
            vc.didMove(toParent: self)
            vc.view.isOpaque = false
            vc.view.backgroundColor = .clear
            view.bringSubviewToFront(vc.view)
        }
    }
}

struct TouchVC: UIViewControllerRepresentable {
    var vcs: [UIViewController]
    func makeUIViewController(context: Context) -> UIViewController {
        TouchUIViewController.shared.performUpdate(DwebDeskVCStroe.shared.getSortedVcs())
        return TouchUIViewController.shared
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        TouchUIViewController.shared.performUpdate(DwebDeskVCStroe.shared.getSortedVcs())
        print("updateUIViewController")
    }
}
