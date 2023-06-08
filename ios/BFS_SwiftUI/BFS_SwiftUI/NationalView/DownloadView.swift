//
//  DownloadAnimationView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/22.
//

import SwiftUI

struct DownloadView: View {
    
    @State var progress: CGFloat = 1.0
    @State var isOverlay: Bool = false
    @State var isFinish: Bool = false
    
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    
    var body: some View {
        VStack {
            Button {
                isOverlay = true
            } label: {
                Image(systemName: "arrow.right.circle.fill")
                    .resizable()
                    .frame(width: 200, height: 200)
                    .cornerRadius(100)
            }
            .overlay {
                if isOverlay {
                    DownloadAnimationView(progress: progress, size: CGSize(width: 200, height: 200), isFinished: $isFinish)
                        .frame(width: 200, height: 200)
                        .onReceive(timer) { out in
                            if self.progress >= 0.0 {
                                self.progress -= 0.1
                            } else {
                                if isFinish {
                                    self.isOverlay = false
                                }
                            }
                        }
                }
            }
        }
    }
}

struct DownloadView_Previews: PreviewProvider {
    static var previews: some View {
        DownloadView()
    }
}


struct DownloadAnimationView: UIViewRepresentable {
    
    var progress: CGFloat
    var size: CGSize
    @Binding var isFinished: Bool
    
    func makeUIView(context: Context) -> UIView {
        
        let animationView = UIView()
        animationView.layer.addSublayer(circleLayer)
        animationView.layer.addSublayer(borderLayer)
        animationView.layer.cornerRadius = size.width * 0.5
        animationView.layer.masksToBounds = true
        return animationView
    }
    
    func updateUIView(_ uiView: UIView, context: Context) {
        guard let sublayers = uiView.layer.sublayers else { return }
        for layer in sublayers {
            if let layer = layer as? CAShapeLayer {
                if progress <= 0 {
                    layer.strokeEnd = 0
                    let anima = animation()
                    anima.delegate = context.coordinator
                    layer.add(anima, forKey: "expandAnimation")
                    if isFinished {
                        layer.fillColor = UIColor.clear.cgColor
                    }
                } else {
                    layer.strokeEnd = progress
                }
            }
        }
    }
    
    private var circleLayer: CAShapeLayer {
        let layer = CAShapeLayer()
        layer.fillColor = UIColor.clear.cgColor
        layer.strokeColor = UIColor.black.withAlphaComponent(0.5).cgColor
        layer.lineWidth = (size.width - 30) * 0.5
        layer.strokeEnd = 1
        layer.path = self.pathWithDiameter(size: size, diameter: (size.height - 30) * 0.5).cgPath
        layer.fillRule = .evenOdd
        return layer
    }
    
    private var borderLayer: CAShapeLayer {
        let layer = CAShapeLayer()
        layer.fillColor = UIColor.black.withAlphaComponent(0.5).cgColor
        layer.path = self.maskPathWithDiameter(size: size, diameter: size.height - 20).cgPath
        layer.fillRule = .evenOdd
        return layer
    }
    
    func animation() -> CABasicAnimation {
        let animation = CABasicAnimation(keyPath: "path")
        animation.toValue = self.maskPathWithDiameter(size: size, diameter:150).cgPath
        animation.duration = 0.35
        animation.isRemovedOnCompletion = true
        animation.fillMode = .forwards
        return animation
    }
    
    func makeCoordinator() -> Coordinator {
        return Coordinator(parent1: self)
    }

    class Coordinator: NSObject, CAAnimationDelegate {

        var parent: DownloadAnimationView

        init(parent1: DownloadAnimationView) {
            self.parent = parent1
        }

        func animationDidStop(_ anim: CAAnimation, finished flag: Bool) {
            parent.isFinished = true
        }
    }
    
    //根据直径生成圆的path，注意圆点是self的中心点，所以（x，y）不是（0，0）
    private func pathWithDiameter(size: CGSize, diameter: CGFloat) -> UIBezierPath {
        let path = UIBezierPath()
        path.move(to: CGPoint(x: size.width * 0.5, y: (size.height - diameter) * 0.5))
        path.addArc(withCenter: CGPoint(x: size.width * 0.5, y: size.height * 0.5), radius: diameter * 0.5, startAngle: -Double.pi * 0.5, endAngle: Double.pi * 3 * 0.5, clockwise: true)
        return path.reversing()  //注意动画是从strokeEnd从1到0，所以path应该取反
    }
    
    //用添加的方式画一个边是矩形，中间有一个圆形的path，配合fillRule和fillRule可以做出中间透明、外部半透明的效果
    private func maskPathWithDiameter(size: CGSize, diameter: CGFloat) -> UIBezierPath {
        let path = UIBezierPath(rect: CGRect(origin: .zero, size: size))
        path.move(to: CGPoint(x: size.width * 0.5, y: (size.height - diameter) * 0.5))
        path.addArc(withCenter: CGPoint(x: size.width * 0.5, y: size.height * 0.5), radius: diameter * 0.5, startAngle: -Double.pi * 0.5, endAngle: Double.pi * 3 * 0.5, clockwise: true)
        return path
    }
    
}
