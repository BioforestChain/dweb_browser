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
    let trigger: (() -> Void)?
    let parentView: UIView
    
    var leftEdgePan: UIScreenEdgePanGestureRecognizer? = nil
    var rightEdgePan: UIScreenEdgePanGestureRecognizer? = nil
    
    var leftAnimationView: EdgeSwipeAnimationView? = nil
    var rightAnimationView: EdgeSwipeAnimationView? = nil
    
    var leftBeginPoint: CGPoint = .zero
    var rightBeginPoint: CGPoint = .zero
    
    init(_ view: UIView, trigger: (() -> Void)?) {
        self.trigger = trigger
        self.parentView = view
        super.init()
        setup()
    }
    
    func setup() {
        self.leftEdgePan = setupEdgePanGesture(edges: .left, action: #selector(edgeAction(ges:)))
        self.rightEdgePan = setupEdgePanGesture(edges: .right, action: #selector(edgeAction(ges:)))
        
        self.leftAnimationView = setupEdgeAnimation(x: 0)
        self.rightAnimationView = setupEdgeAnimation(x: 0, isRight: true)
    }
    
    private func setupEdgePanGesture(edges: UIRectEdge, action: Selector) -> UIScreenEdgePanGestureRecognizer {
        let edgePan = UIScreenEdgePanGestureRecognizer(target: self, action: action)
        edgePan.edges = edges
        edgePan.delegate = self
        parentView.addGestureRecognizer(edgePan)
        return edgePan
    }
    
    private func setupEdgeAnimation(x: CGFloat, isRight: Bool = false) -> EdgeSwipeAnimationView {
        let animationView = EdgeSwipeAnimationView(frame: CGRect(x: x, y: 100, width: animationInitWidth, height: animationHeight), isRight: isRight)
        animationView.alpha = 0.0
        animationView.anchorPoint = CGPoint(x: isRight ? 1.0 : 0.0, y: 0.5)
        parentView.addSubview(animationView)
        return animationView
    }
    
    @objc private func edgeAction(ges: UIGestureRecognizer) {
        guard let leftAnimationView = leftAnimationView, let rightAnimationView = rightAnimationView else { return }
        
        let p = ges.location(in: ges.view)
        let isLeftEdge = ges == leftEdgePan
        let animationView = isLeftEdge ? leftAnimationView : rightAnimationView
        let beginPoint = isLeftEdge ? leftBeginPoint : rightBeginPoint
        
        if ges.state == .began {
            // 在新的手势开始时，取消当前动画
            animationView.layer.removeAllAnimations()
            if isLeftEdge { leftBeginPoint = p } else { rightBeginPoint = p }
            animationView.frame = CGRect(x: isLeftEdge ? 0 : parentView.bounds.width - animationInitWidth,
                                     y: p.y - animationHeight / 2.0,
                                     width: animationInitWidth,
                                     height: animationHeight)
            animationView.alpha = 1.0
        } else if ges.state == .changed {
            let deltaX = isLeftEdge ? p.x - beginPoint.x : beginPoint.x - p.x
            let newWidth = min(animationInitWidth + deltaX, animationFullWidth)
            animationView.frame = CGRect(x: isLeftEdge ? 0 : parentView.bounds.width - newWidth,
                                     y: beginPoint.y - animationHeight / 2.0,
                                     width: newWidth,
                                     height: animationHeight)
        } else if ges.state == .ended || ges.state == .cancelled {
            UIView.animate(withDuration: 0.3) {
                animationView.transform = CGAffineTransform.identity.scaledBy(x: 0.1, y: 1)
                animationView.alpha = 0.0
            } completion: { _ in
                animationView.transform = .identity
                animationView.alpha = 0.0
                if isLeftEdge { self.leftBeginPoint = .zero } else { self.rightBeginPoint = .zero }
            }
        }
        
        if ges.state == .ended {
            let deltaX = isLeftEdge ? p.x - beginPoint.x : beginPoint.x - p.x
            if animationInitWidth + deltaX > animationFullWidth {
                trigger?()
            }
        }
    }
}

extension DwebScreenEdgePanGestureRecognizer: UIGestureRecognizerDelegate {
    var animationInitWidth: CGFloat { 25 }
    var animationHeight: CGFloat { 330 }
    var animationFullWidth: CGFloat { 70 }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldBeRequiredToFailBy otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        if gestureRecognizer == leftEdgePan || gestureRecognizer == rightEdgePan {
            return true
        }
        return false
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        return false
    }
}
