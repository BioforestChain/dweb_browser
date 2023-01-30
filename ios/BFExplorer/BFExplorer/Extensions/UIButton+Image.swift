//
//  UIButton+Image.swift
//  Browser
//
//   22.
//

import UIKit


//extension UIButton {
//    /*
//     UIButton默认布局是图片在左、文字在右，图片和文字之间边距为0，图片和文字整体居中显示
//     当同时存在image和title时，imageEdgeInsets中的top、left、bottom相对于UIButton，right相对于title，同理，titleEdgeInsets中的top、bottom、rright相对于UIButton，left相对于title
//     imageEdgeInsets = UIEdgeInsets.init(top: 5, left: 0, bottom: -5, right: 0) 表示图片整体向下移动5
//     imageEdgeInsets = UIEdgeInsets.init(top: -5, left: 4, bottom: 5, right: -4) 表示图片整体向上移动5，向右移动4
//     
//     默认的情况下当按钮比较小时会自动保留图片的尺寸和将文字部分缩小，一般出现在图标和文字上下布局，button的整体较小时,因为按钮总体宽度比较小，导致系统给分配的宽度不足以完整显示文字
//     */
//    enum ImagePosition: Int {
//        case top    = 1
//        case left   = 2
//        case bottom = 3
//        case right  = 4
//    }
//    
//    /// 设置label相对于图片的位置
//    /// - Parameters:
//    ///   - anImage: 按钮图片
//    ///   - title: 标题
//    ///   - imagePosition: label相对于图片的位置（上下左右）
//    ///   - additionalSpacing: 文字和图片的间隔
//    ///   - state: UIControl.State
//    ///   - isSureTitleCompress: 是否明确文字被系统挤压，true：使用文字被压缩的调整模式，false：根据系统为文字分配的size自动适配（主要是了为了应对有些文字挤压的按钮被重复设置的情况）
//    func setImage(image anImage: UIImage?, title: String, imagePosition: ImagePosition, additionalSpacing: CGFloat, state: UIControl.State = .normal, isSureTitleCompress: Bool = false){
//        self.setImage(anImage, for: state)
//        self.setTitle(title, for: state)
//        positionLabelRespectToImage(title: title, position: imagePosition, spacing: additionalSpacing, isSureTitleCompress: isSureTitleCompress)
//    }
//    
//    private func positionLabelRespectToImage(title: String, position: ImagePosition,spacing: CGFloat, isSureTitleCompress: Bool = false) {
//        self.layoutIfNeeded()//这一步很重要，否则如果UIButton通过约束布局会导致titleRect获取的rect不准
//        let imageSize = self.imageView?.image?.size ?? .zero
//        let titleSize = self.titleRect(forContentRect: self.frame).size//系统为titleLabel分配的size
//        
//        var titleNeedSize: CGSize = .zero//展示文字实际所需的size
//        if let font = self.titleLabel?.font {
//            titleNeedSize = title.size(withAttributes: [NSAttributedString.Key.font: font])
//        }
//        var isTitleCompress = false//文字是否被系统压缩
//        if isSureTitleCompress {
//            isTitleCompress = true
//        } else if titleNeedSize.width > titleSize.width {
//            isTitleCompress = true
//        }
//        
//        switch (position){
//        case .top:
//            let imageTop = -(titleSize.height/2 + spacing/2)
//            let titleTop = imageSize.height/2 + spacing/2
//            if isTitleCompress {
//                let imageLeft = (self.bounds.size.width - imageSize.width) / 2
//                self.imageEdgeInsets = UIEdgeInsets.init(top: imageTop, left: imageLeft, bottom: -imageTop, right: 0)
//                self.titleEdgeInsets = UIEdgeInsets(top: titleTop, left: -imageSize.width, bottom: -titleTop, right: 0)
//            } else {
//                self.imageEdgeInsets = UIEdgeInsets(top: imageTop, left: titleSize.width/2, bottom: -imageTop, right: -titleSize.width/2)
//                self.titleEdgeInsets = UIEdgeInsets(top: titleTop, left: -imageSize.width/2, bottom: -titleTop, right: imageSize.width/2)
//            }
//            
//        case .left:
//            self.imageEdgeInsets = UIEdgeInsets(top: 0, left: -spacing/2, bottom: 0, right: spacing/2)
//            self.titleEdgeInsets = UIEdgeInsets(top: 0, left: spacing/2, bottom: 0, right: -spacing/2)
//            
//        case .bottom:
//            let imageTop = titleSize.height/2 + spacing/2
//            let titleTop = -(imageSize.height/2 + spacing/2)
//            if isTitleCompress {
//                let imageLeft = (self.bounds.size.width - imageSize.width) / 2
//                self.imageEdgeInsets = UIEdgeInsets(top: imageTop, left: imageLeft, bottom: -imageTop, right: 0)
//                self.titleEdgeInsets = UIEdgeInsets(top: titleTop,
//                                                    left: -imageSize.width, bottom: -titleTop, right: 0)
//            } else {
//                self.imageEdgeInsets = UIEdgeInsets(top: imageTop, left: titleSize.width/2, bottom: -imageTop, right: -titleSize.width/2)
//                self.titleEdgeInsets = UIEdgeInsets(top: titleTop,
//                                                    left: -imageSize.width/2, bottom: -titleTop, right: imageSize.width/2)
//            }
//            
//        case .right:
//            self.imageEdgeInsets = UIEdgeInsets(top: 0, left: titleSize.width + spacing/2, bottom: 0,
//                                                right: -(titleSize.width + spacing/2))
//            self.titleEdgeInsets = UIEdgeInsets(top: 0, left: -(imageSize.width + spacing/2), bottom: 0, right: imageSize.width + spacing/2)
//        }
//    }
//    
//}

