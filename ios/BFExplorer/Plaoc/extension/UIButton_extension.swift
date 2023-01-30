//
//  UIButton_extension.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/4.
//

import Foundation

enum RGButtonImagePosition {
    case top
    case bottom
    case left
    case right
}

extension UIButton {
    
    func imagePosition(style: RGButtonImagePosition, spacing: CGFloat) {
        
        let imageWidth = self.imageView?.frame.size.width
        
        let imageHeight = self.imageView?.frame.size.height
        
        var labelWidth: CGFloat! = 0.0
        
        var labelHeight: CGFloat! = 0.0
        
        labelWidth = self.titleLabel?.intrinsicContentSize.width
        
        labelHeight = self.titleLabel?.intrinsicContentSize.height
        
        var imageEdgeInsets =  UIEdgeInsets.zero
        
        var labelEdgeInsets = UIEdgeInsets.zero
        
        switch style {
            
            case.top:
            
            imageEdgeInsets = UIEdgeInsets(top: -labelHeight-spacing/2,left:0,bottom:0,right: -labelWidth)
            
            labelEdgeInsets = UIEdgeInsets(top:0,left: -imageWidth!,bottom: -imageHeight!-spacing/2,right:0)
            
        case.left:
            
            imageEdgeInsets = UIEdgeInsets(top:0,left: -spacing/2,bottom:0,right: spacing)
            
            labelEdgeInsets = UIEdgeInsets(top:0,left: spacing/2,bottom:0,right: -spacing/2)
            
        case.bottom:
            
            imageEdgeInsets = UIEdgeInsets(top:0,left:0,bottom: -labelHeight!-spacing/2,right: -labelWidth)
            
            labelEdgeInsets = UIEdgeInsets(top: -imageHeight!-spacing/2,left: -imageWidth!,bottom:0,right:0)
            
            
        case.right:
            
            imageEdgeInsets = UIEdgeInsets(top:0,left: labelWidth+spacing/2,bottom:0,right: -labelWidth-spacing/2)
            
            labelEdgeInsets = UIEdgeInsets(top:0,left: -imageWidth!-spacing/2,bottom:0,right: imageWidth!+spacing/2)

        }
        
        self.titleEdgeInsets = labelEdgeInsets
        
        self.imageEdgeInsets = imageEdgeInsets
    }
    
    
}
