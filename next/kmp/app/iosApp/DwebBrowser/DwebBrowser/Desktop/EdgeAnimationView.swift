//
//  EdgeAnimationView.swift
//  DwebBrowser
//
//  Created by linjie on 2024/9/4.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit

class EdgeAnimationView: UIView {
    var edge: UIRectEdge = .left
    var triggerAction: () -> Void = {}

    private let fullWidth: Int = 60
    private var initFrame: CGRect = .zero
    private var originalWidth: CGFloat = 0.0
    private var initialOriginX: CGFloat = 0.0 // 添加初始的 x 坐标变量

    private var animator: UIViewPropertyAnimator?
    private let shapeLayer = CAShapeLayer()
    private let iconView = UIImageView(frame: .zero)
    private var cachedPaths = [CGPath]()
    
    init(frame: CGRect, rectEdge: UIRectEdge, trigger: @escaping ()->Void) {
        super.init(frame: frame)
        edge = rectEdge
        generateCachedPaths()
        self.triggerAction = trigger
        initFrame = frame
        configUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func configUI(){
        layer.addSublayer(shapeLayer)
        shapeLayer.fillColor = UIColor.clear.withAlphaComponent(0.3).cgColor
        shapeLayer.strokeColor = UIColor.clear.cgColor
        shapeLayer.lineWidth = 3.0
        
        let panGesture = UIPanGestureRecognizer(target: self, action: #selector(handlePanGesture(_:)))
        self.addGestureRecognizer(panGesture)
        
        iconView.frame = bounds
        iconView.image = UIImage(systemName: edge == .right ? "chevron.left" : "chevron.right")
        iconView.tintColor = .white
        iconView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(iconView)
        if edge == .right {
            NSLayoutConstraint.activate([
                iconView.centerYAnchor.constraint(equalTo: centerYAnchor),
                iconView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 8),
            ])
        } else {
            NSLayoutConstraint.activate([
                iconView.centerYAnchor.constraint(equalTo: centerYAnchor),
                iconView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -8),
            ])
        }
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        // 更新layer的路径
        var indexWidth = Int(self.frame.width)
        if indexWidth == 0 {
            return
        }
        if indexWidth >= cachedPaths.count{
            indexWidth = cachedPaths.count-1
        }
        shapeLayer.path = cachedPaths[indexWidth]
        shapeLayer.frame = self.bounds
    }
    
    private func generateCachedPaths() {
        let height = self.frame.height
        let rectEdge = self.edge
        DispatchQueue.global(qos: .userInitiated).async {
            for i in 1...self.fullWidth {
                let path = EdgeAnimationView.generatePath(for: i, height: height, edge: rectEdge)
                self.cachedPaths.append(path)
            }
        }
    }
    
    @objc func handlePanGesture(_ sender: UIPanGestureRecognizer) {
        let translation = sender.translation(in: self.superview)

        switch sender.state {
        case .began:
            // 如果有正在进行的动画，停止并完成它
            if let animator = animator, animator.isRunning {
                animator.stopAnimation(true)
                animator.finishAnimation(at: .current)
            }else{
                //这是每次新触发的手势。 否则如果有动画在进行，二次拖拽动画则建立在前一个动画位置之上
                let gesPoint = sender.location(in: sender.view)
                initFrame = CGRect(x: initFrame.minX, y: gesPoint.y - initFrame.height/2, width: initFrame.width, height: initFrame.height)
            }
            stopAnimation()
            originalWidth = self.bounds.width
            initialOriginX = self.frame.origin.x // 记录初始的 x 坐标
            self.frame = initFrame
        case .changed:
            // 根据手势的水平移动调整宽度
            let newWidth = min(originalWidth + abs(translation.x), CGFloat(fullWidth))
            if edge == .left{
                self.frame = CGRect(x: self.frame.origin.x, y: self.frame.origin.y, width: newWidth, height: self.bounds.height)
            }else{
                let newX = initialOriginX - (newWidth - originalWidth)
                self.frame = CGRect(x: newX, y: self.frame.origin.y, width: newWidth, height: self.bounds.height)
            }
        case .ended, .cancelled:
            let offsetX = self.frame.width
            if (offsetX / CGFloat(fullWidth)) > 0.7 {
                triggerAction()
            }
            animateShapes()
            animateBackToOriginalWidth()
        default:
            break
        }
    }
    
    private func animateBackToOriginalWidth() {
        animator = UIViewPropertyAnimator(duration: 0.6, curve: .linear) {
            self.frame = self.initFrame
        }
        
        // 确保动画完成后才释放
        animator?.addCompletion { position in
            if position == .end && self.animator == animator {
                self.animator = nil
            }
        }
        
        animator?.startAnimation()
    }
    
    private func animateShapes() {
        let currentWidth = Int(self.frame.width)
        let index = min(currentWidth, cachedPaths.count - 1)
        let subArray = Array(cachedPaths[0...index].reversed())
        let animation = CAKeyframeAnimation(keyPath: "path")
        animation.values = subArray
        animation.duration = 0.6 // 动画持续时间
        animation.timingFunction = CAMediaTimingFunction(name: .linear)
        shapeLayer.add(animation, forKey: "shapeAnimation")
    }
    
    func stopAnimation() {
        if let presentationLayer = shapeLayer.presentation(),
           let currentPath = presentationLayer.path {
            // 在停止动画时，将当前路径设置为 shapeLayer 的路径，避免一闪现象
            shapeLayer.path = currentPath
        }
        // 二次拖拽时，需要停止前一次的回原动画，通过 key 来移除动画，这里使用添加动画时的 key
        shapeLayer.removeAnimation(forKey: "shapeAnimation")
    }
    
    private static func generatePath(for indexWidth: Int, height: CGFloat, edge: UIRectEdge) -> CGPath {
        let bezPath = UIBezierPath()
        let width = CGFloat(indexWidth)
        if edge == .left {
            bezPath.move(to: CGPoint.zero)
            bezPath.addCurve(to: CGPoint(x: width, y: height/2),
                             controlPoint1: CGPoint(x: 0, y: height/4),
                             controlPoint2: CGPoint(x: width, y: height/3.3))
            bezPath.addCurve(to: CGPoint(x: 0, y: height),
                             controlPoint1: CGPoint(x: width, y: height*2.3/3.3),
                             controlPoint2: CGPoint(x: 0, y: height*3.0/4))
        }else{
            bezPath.move(to: CGPoint(x: width, y: 0))
            bezPath.addCurve(to: CGPoint(x: 0, y: height / 2),
                             controlPoint1: CGPoint(x: width, y: height / 4),
                             controlPoint2: CGPoint(x: 0, y: height / 3.3))
            bezPath.addCurve(to: CGPoint(x: width, y: height),
                             controlPoint1: CGPoint(x: 0, y: height * 2.3 / 3.3),
                             controlPoint2: CGPoint(x: width, y: height * 3.0 / 4))
        }
        return bezPath.cgPath
    }
}
