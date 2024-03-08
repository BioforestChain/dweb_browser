//
//  DwebScreenEdgePanGestureRecognizer.swift
//  DwebBrowser
//
//  Created by instinct on 2024/3/8.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import UIKit

class DwebScreenEdgePanGestureRecognizer: NSObject {
    let tigger: (() -> Void)?
    let view: UIView

    var leftEdgePan: UIScreenEdgePanGestureRecognizer? = nil
    var rightEdgePan: UIScreenEdgePanGestureRecognizer? = nil

    var leftIndicator: DwebScreenEdgeIndicator? = nil
    var rightIndicator: DwebScreenEdgeIndicator? = nil
        
    let indicatorWidth: CGFloat = 25
    let indicatorHeight: CGFloat = 330
    let indicatorWidthMax: CGFloat = 70
    
    init(_ view: UIView, tigger: (() -> Void)?) {
        self.tigger = tigger
        self.view = view
        super.init()
        setup()
    }
    
    func setup() {
        let leftEdgePan = UIScreenEdgePanGestureRecognizer(target: self, action: #selector(edgeAction(ges:)))
        leftEdgePan.edges = .left
        leftEdgePan.delegate = self
        view.addGestureRecognizer(leftEdgePan)
        self.leftEdgePan = leftEdgePan
        
        let rightEdgePan = UIScreenEdgePanGestureRecognizer(target: self, action: #selector(edgeAction(ges:)))
        rightEdgePan.edges = .right
        rightEdgePan.delegate = self
        view.addGestureRecognizer(rightEdgePan)
        self.rightEdgePan = rightEdgePan
        
        let leftIndicator = DwebScreenEdgeIndicator(frame: CGRect(x: 0, y: 100, width: indicatorWidth, height: indicatorHeight))
        leftIndicator.anchorPoint = CGPoint(x: 0, y: 0.5)
        leftIndicator.alpha = 0.0
        view.addSubview(leftIndicator)
        self.leftIndicator = leftIndicator
        
        let rightIndicator = DwebScreenEdgeIndicator(frame: CGRect(x: 0, y: 100, width: indicatorWidth, height: indicatorHeight), isRight: true)
        rightIndicator.alpha = 0.0
        rightIndicator.anchorPoint = CGPoint(x: 1.0, y: 0.5)
        view.addSubview(rightIndicator)
        self.rightIndicator = rightIndicator
    }
    
    var leftBeginPoint: CGPoint = .zero
    var rightBeginPoint: CGPoint = .zero
    
    @objc private func edgeAction(ges: UIGestureRecognizer) {
        guard let leftIndicator = leftIndicator, let rightIndicator = rightIndicator else {
            return
        }
        
        let p = ges.location(in: ges.view)
        if ges == leftEdgePan {
            switch ges.state {
            case .began:
                leftBeginPoint = p
                leftIndicator.frame = CGRect(x: 0,
                                             y: leftBeginPoint.y - indicatorHeight / 2.0,
                                             width: indicatorWidth,
                                             height: indicatorHeight)
                leftIndicator.alpha = 1.0
            case .changed:
                leftIndicator.frame = CGRect(x: 0,
                                             y: leftBeginPoint.y - indicatorHeight / 2.0,
                                             width: min(indicatorWidth + (p.x - leftBeginPoint.x), indicatorWidthMax),
                                             height: indicatorHeight)
            case .ended, .cancelled:
                UIView.animate(withDuration: 0.5) {
                    leftIndicator.transform = CGAffineTransform(scaleX: 0.01, y: 1.0)
                    leftIndicator.alpha = 0.0
                } completion: { _ in
                    leftIndicator.transform = CGAffineTransform.identity
                    leftIndicator.alpha = 0.0
                    self.leftBeginPoint = CGPoint.zero
                }
            default:
                leftBeginPoint = CGPoint.zero
                leftIndicator.frame = CGRect(x: 0,
                                             y: leftBeginPoint.y - indicatorHeight / 2.0,
                                             width: indicatorWidth,
                                             height: indicatorHeight)
            }
        } else if ges == rightEdgePan {
            switch ges.state {
            case .began:
                rightBeginPoint = p
                rightIndicator.frame = CGRect(x: view.bounds.width - indicatorWidth,
                                              y: rightBeginPoint.y - indicatorHeight / 2.0,
                                              width: indicatorWidth,
                                              height: indicatorHeight)
                rightIndicator.alpha = 1.0
            case .changed:
                let width = min(indicatorWidth + (rightBeginPoint.x - p.x), indicatorWidthMax)
                rightIndicator.frame = CGRect(x: view.bounds.width - width,
                                              y: rightBeginPoint.y - indicatorHeight / 2.0,
                                              width: width,
                                              height: indicatorHeight)
            case .ended, .cancelled:
                UIView.animate(withDuration: 0.5) {
                    rightIndicator.transform = CGAffineTransform(scaleX: 0.01, y: 1.0)
                    rightIndicator.alpha = 0.0
                } completion: { _ in
                    rightIndicator.transform = CGAffineTransform.identity
                    rightIndicator.alpha = 0.0
                    self.rightBeginPoint = CGPoint.zero
                }
            default:
                rightBeginPoint = CGPoint.zero
                rightIndicator.frame = CGRect(x: view.bounds.width - indicatorWidth,
                                              y: rightBeginPoint.y - indicatorHeight / 2.0,
                                              width: indicatorWidth,
                                              height: indicatorHeight)
            }
        }
        
        if ges.state == .ended {
            if ges == leftEdgePan && (indicatorWidth + (p.x - leftBeginPoint.x) > indicatorWidthMax) {
                tigger?()
            } else if ges == rightEdgePan && (indicatorWidth + (rightBeginPoint.x - p.x) > indicatorWidthMax) {
                tigger?()
            }
        }
    }
}

extension DwebScreenEdgePanGestureRecognizer: UIGestureRecognizerDelegate {
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
