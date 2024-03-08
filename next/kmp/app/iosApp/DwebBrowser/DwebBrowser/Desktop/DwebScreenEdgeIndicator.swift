//
//  DwebScreenEdgeIndicator.swift
//  DwebBrowser
//
//  Created by instinct on 2024/3/8.
//  Copyright © 2024 orgName. All rights reserved.
//

import UIKit

class DwebScreenEdgeIndicator: UIView {
    lazy var bezLayer: CAShapeLayer = {
        let circle = CAShapeLayer()
        circle.fillColor = UIColor.black.withAlphaComponent(0.5).cgColor
        circle.strokeColor = UIColor.clear.cgColor
        circle.lineWidth = 5.0
        circle.strokeEnd = 0.0
        return circle
    }()
    
    lazy var iconView: UIImageView = {
        let imgV = UIImageView(image: UIImage(systemName: "chevron.left"))
        imgV.tintColor = .white
        imgV.translatesAutoresizingMaskIntoConstraints = false
        return imgV
    }()
    
    init(frame: CGRect, isRight: Bool = false) {
        self.isRight = isRight
        super.init(frame: frame)
        layer.backgroundColor = UIColor.clear.cgColor
        layer.addSublayer(bezLayer)
        contentMode = .redraw
        
        addSubview(iconView)

        if isRight {
            NSLayoutConstraint.activate([
                iconView.centerYAnchor.constraint(equalTo: centerYAnchor),
                iconView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 8),
                iconView.trailingAnchor.constraint(lessThanOrEqualTo: trailingAnchor, constant: -5),
            ])
        } else {
            NSLayoutConstraint.activate([
                iconView.centerYAnchor.constraint(equalTo: centerYAnchor),
                iconView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -8),
                iconView.leadingAnchor.constraint(greaterThanOrEqualTo: leadingAnchor, constant: 5),
            ])
        }
    }
    
    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        bezLayer.bounds = bounds
        bezLayer.position = CGPoint(x: bounds.width/2.0, y: bounds.height/2.0)
        updatePath(layer.frame)
    }
    
    override func draw(_ rect: CGRect) {
        updatePath(rect)
    }
    
    let isRight: Bool
    
    func updatePath(_ rect: CGRect) {
        let bezPath = UIBezierPath()

        if isRight {
            bezPath.move(to: CGPoint(x: rect.width, y: 0))

            bezPath.addCurve(to: CGPoint(x: 0, y: rect.size.height/2.0),
                             controlPoint1: CGPoint(x: rect.size.width, y: rect.size.height/4.0),
                             controlPoint2: CGPoint(x: 0, y: rect.size.height * 0.3))
            
            bezPath.addCurve(to: CGPoint(x: rect.size.width, y: rect.size.height),
                             controlPoint1: CGPoint(x: 0, y: rect.size.height * 0.7),
                             controlPoint2: CGPoint(x: rect.size.width, y: rect.size.height/4.0 * 3))
        } else {
            bezPath.move(to: CGPoint.zero)

            bezPath.addCurve(to: CGPoint(x: rect.size.width, y: rect.size.height/2.0),
                             controlPoint1: CGPoint(x: rect.size.width * 0.0, y: rect.size.height/4.0),
                             controlPoint2: CGPoint(x: rect.size.width, y: rect.size.height * 0.3))
            
            bezPath.addCurve(to: CGPoint(x: 0, y: rect.size.height),
                             controlPoint1: CGPoint(x: rect.size.width, y: rect.size.height * 0.7),
                             controlPoint2: CGPoint(x: rect.size.width * 0.0, y: rect.size.height/4.0 * 3))
        }
        
        bezLayer.path = bezPath.cgPath
    }
}
