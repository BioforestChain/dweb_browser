//
//  AnimationVIew.swift
//  BFS
//
//  Created by ui03 on 2022/9/13.
//

import UIKit

class AnimationView: UIImageView {

    var borderLayer: CAShapeLayer!   //中间的圆透明，圆与边框之间的内容半透明的layer
    var clockCircleLayer: CAShapeLayer!  //中心动画的layer
    var isStart: Bool = false
    
    init(targetView: UIView) {
        super.init(frame: targetView.bounds)
        borderLayer = CAShapeLayer()
        borderLayer.fillColor = UIColor.black.withAlphaComponent(0.5).cgColor
        borderLayer.path = self.maskPathWithDiameter(diameter: self.bounds.height - 20).cgPath
        borderLayer.fillRule = .evenOdd
        
        clockCircleLayer = CAShapeLayer()
        clockCircleLayer.fillColor = UIColor.clear.cgColor
        clockCircleLayer.strokeColor = UIColor.black.withAlphaComponent(0.5).cgColor
        clockCircleLayer.lineWidth = (self.bounds.height - 30) * 0.5
        clockCircleLayer.path = self.pathWithDiameter(diameter: (self.bounds.height - 30) * 0.5).cgPath
        clockCircleLayer.strokeEnd = 1
        
        self.layer.addSublayer(borderLayer)
        self.layer.addSublayer(clockCircleLayer)
        self.layer.masksToBounds = true
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)

    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func reveal() {
        let hahaView = self.viewWithTag(100) as? AnimationView
        guard hahaView != nil else { return }
        hahaView!.startClockCircleAnimation()
    }
    
    func startClockCircleAnimation() {
        guard !isStart else { return }
        isStart = true
        
        self.clockCircleLayer.add(self.clockRevealAnimation(), forKey: "clockRevealAnimation")
    }
    
    func pathWithDiameter(diameter: CGFloat) -> UIBezierPath {
        let path = UIBezierPath()
        path.move(to: CGPoint(x: self.bounds.width * 0.5, y: (self.bounds.height - diameter) * 0.5))
        path.addArc(withCenter: self.center, radius: diameter * 0.5, startAngle: -Double.pi * 0.5, endAngle: Double.pi * 3 * 0.5, clockwise: true)
        return path.reversing()
    }
    
    func maskPathWithDiameter(diameter: CGFloat) -> UIBezierPath {
        let path = UIBezierPath(rect: self.bounds)
        path.move(to: CGPoint(x: self.bounds.width * 0.5, y: (self.bounds.height - diameter) * 0.5))
        path.addArc(withCenter: self.center, radius: diameter * 0.5, startAngle: -Double.pi * 0.5, endAngle: Double.pi * 3 * 0.5, clockwise: true)
        return path
    }
    
    func clockRevealAnimation() -> CABasicAnimation {
        let animation = CABasicAnimation(keyPath: "strokeEnd")
        animation.toValue = 0
        animation.duration = 3
        animation.delegate = self
        animation.isRemovedOnCompletion = false
        animation.fillMode = .forwards
        return animation
    }
    
    func expandAnimation() -> CABasicAnimation {
        let animation = CABasicAnimation(keyPath: "path")
        animation.toValue = self.maskPathWithDiameter(diameter:150).cgPath
        animation.duration = 2.0
        animation.delegate = self
        animation.isRemovedOnCompletion = false
        animation.fillMode = .forwards
        return animation
    }
}

extension AnimationView :CAAnimationDelegate {
    // 移除mask
    func animationDidStop(_ anim: CAAnimation, finished flag: Bool) {
        
        if clockCircleLayer.animation(forKey: "clockRevealAnimation") == anim {
            self.borderLayer.add(self.expandAnimation(), forKey: "expandAnimation")
        } else if self.borderLayer.animation(forKey: "expandAnimation") == anim {
            self.isStart = false
            self.removeFromSuperview()
        }
    }
}

