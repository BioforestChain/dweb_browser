//
//  ScanNetAnimationImageView.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/12/15.
//

import UIKit

class ScanNetAnimationImageView: UIImageView {

    private var isAnimation: Bool = false
    private var animationRect: CGRect = .zero
    
    public static func instance() -> ScanNetAnimationImageView {
        return ScanNetAnimationImageView()
    }

    func startAnimatingWithRect(animationRect: CGRect, parentView: UIView, image: UIImage?) {
        self.image = image
        self.animationRect = animationRect
        parentView.addSubview(self)
        
        isHidden = false
        isAnimation = true
        
        if image != nil {
            stepAnimation()
        }
    }
    
    @objc private func stepAnimation() {
        guard isAnimation else { return }
        var frame = animationRect
        let hImg = image!.size.height * animationRect.size.width / image!.size.width
        
        frame.origin.y = -hImg + 200
        frame.size.height = hImg
        self.frame = frame
        
        alpha = 0.0
        
        UIView.animate(withDuration: 1.2) {
            self.alpha = 1.0
            var frame = self.animationRect
            let hImg = self.image!.size.height * self.animationRect.size.width / self.image!.size.width
            
            frame.origin.y += (frame.size.height - hImg)
            frame.size.height = hImg
            self.frame = frame
        } completion: { _ in
            self.perform(#selector(self.stepAnimation), with: nil, afterDelay: 0.3)
        }
    }
    
    func stopStepAnimating() {
        isHidden = true
        isAnimation = false
    }
}
