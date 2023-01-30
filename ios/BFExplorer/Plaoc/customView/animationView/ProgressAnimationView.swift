//
//  ProgressAnimationView.swift
//  BFS
//
//  Created by ui03 on 2022/9/2.
//

import UIKit

class ProgressAnimationView: UIImageView {
    
    init(targetView: UIView) {
        
        super.init(frame: targetView.bounds)
        self.layer.addSublayer(borderLayer)
        self.layer.addSublayer(clockCircleLayer)
        //这一句必须要设置，否则layer会超出view范围
        self.layer.masksToBounds = true
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    //进度条动画
    func reveal(progress: CGFloat) {
        
        clockCircleLayer.strokeEnd = progress
    }
    //圆圈扩展动画
    func borderLayerAnimation() {
        clockCircleLayer.strokeEnd = 0
        self.borderLayer.add(self.expandAnimation, forKey: "expandAnimation")
    }

    //根据直径生成圆的path，注意圆点是self的中心点，所以（x，y）不是（0，0）
    private func pathWithDiameter(diameter: CGFloat) -> UIBezierPath {
        let path = UIBezierPath()
        path.move(to: CGPoint(x: self.bounds.width * 0.5, y: (self.bounds.height - diameter) * 0.5))
        path.addArc(withCenter: self.center, radius: diameter * 0.5, startAngle: -Double.pi * 0.5, endAngle: Double.pi * 3 * 0.5, clockwise: true)
        return path.reversing()  //注意动画是从strokeEnd从1到0，所以path应该取反
    }
    //用添加的方式画一个边是矩形，中间有一个圆形的path，配合fillRule和fillRule可以做出中间透明、外部半透明的效果
    private func maskPathWithDiameter(diameter: CGFloat) -> UIBezierPath {
        let path = UIBezierPath(rect: self.bounds)
        path.move(to: CGPoint(x: self.bounds.width * 0.5, y: (self.bounds.height - diameter) * 0.5))
        path.addArc(withCenter: self.center, radius: diameter * 0.5, startAngle: -Double.pi * 0.5, endAngle: Double.pi * 3 * 0.5, clockwise: true)
        return path
    }
    
    lazy  var expandAnimation: CABasicAnimation = {
        let animation = CABasicAnimation(keyPath: "path")
        animation.toValue = self.maskPathWithDiameter(diameter:150).cgPath
        animation.duration = 0.8
        animation.delegate = self
        animation.isRemovedOnCompletion = false
        animation.fillMode = .forwards
        return animation
    }()
    
    lazy private var borderLayer: CAShapeLayer = {
        let layer = CAShapeLayer()
        layer.fillColor = UIColor.black.withAlphaComponent(0.5).cgColor
        layer.path = self.maskPathWithDiameter(diameter: self.bounds.height - 20).cgPath
        layer.fillRule = .evenOdd
        return layer
    }()
    
    lazy private var clockCircleLayer: CAShapeLayer = {
        let layer = CAShapeLayer()
        layer.fillColor = UIColor.clear.cgColor
        layer.strokeColor = UIColor.black.withAlphaComponent(0.5).cgColor
        layer.lineWidth = (self.bounds.height - 30) * 0.5
        layer.strokeEnd = 1
        layer.path = self.pathWithDiameter(diameter: (self.bounds.height - 30) * 0.5).cgPath
        layer.fillRule = .evenOdd
        return layer
    }()
}

extension ProgressAnimationView :CAAnimationDelegate {
    // 移除mask
    func animationDidStop(_ anim: CAAnimation, finished flag: Bool) {
        if clockCircleLayer.animation(forKey: "clockRevealAnimation") == anim {
            self.borderLayer.add(self.expandAnimation, forKey: "expandAnimation")
        } else if self.borderLayer.animation(forKey: "expandAnimation") == anim {
//            self.isStart = false
            self.removeFromSuperview()
        }
        
//        if clockCircleLayer.animation(forKey: "clockRevealAnimation") == anim {
//            self.borderLayer.add(self.expandAnimation(), forKey: "expandAnimation")
//        }
//        if self.borderLayer.animation(forKey: "expandAnimation") == anim {
//            self.removeFromSuperview()
//        }
    }
}

extension UIView {
    
    var viewTag: Int {
        return 100
    }
    
    func setupForAppleReveal() {
        var revealView = self.viewWithTag(viewTag) as? ProgressAnimationView
        guard revealView == nil else { return }
        revealView = ProgressAnimationView(targetView: self)
        revealView?.layer.cornerRadius = self.layer.cornerRadius
        revealView?.layer.masksToBounds = self.layer.masksToBounds
        revealView?.tag = viewTag
        self.addSubview(revealView!)
        self.bringSubviewToFront(revealView!)
    }
    
    func startProgressAnimation(progress: CGFloat) {
        guard let revealView = self.viewWithTag(viewTag) as? ProgressAnimationView else { return }
        revealView.reveal(progress: progress)
    }
    
    func startExpandAnimation() {
        guard let revealView = self.viewWithTag(viewTag) as? ProgressAnimationView else { return }
        revealView.borderLayerAnimation()
    }
    
    func stopAnimation() {
        guard let revealView = self.viewWithTag(viewTag) as? ProgressAnimationView else { return }
        revealView.animationDidStop(revealView.expandAnimation, finished: true)
    }
}
