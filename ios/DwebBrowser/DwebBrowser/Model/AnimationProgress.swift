//
//  ShiftAnimation.swift
//  DwebBrowser
//
//  Created by ui06 on 5/28/23.
//

import Foundation

enum AnimationProgress: Int, Equatable{
    case initial
    case startExpanding
    case expanded

    //获取截图 - 获取cell位置（grid设置opacity为0.01，并scale到原始尺寸，需要滚动grid）-  startShrink（grid设置opacity为1，scale为0.8）
    case preparingShrink
    case startShrinking
    case shrinked
    
    case invisible
    
    func isAnimating() -> Bool{
        return imageIsLarge() || imageIsSmall()// self.rawValue >= AnimationProgress.startExpanding.rawValue && self.rawValue < AnimationProgress.invisible.rawValue
    }
    
    func next()->AnimationProgress{
        switch self{
        case .startExpanding: return .expanded
        case .startShrinking: return .shrinked
        default: return .invisible
        }
    }
    func imageIsLarge() -> Bool{
        return self == .startShrinking || self == .expanded
    }
    func imageIsSmall() -> Bool{
        return self == .startExpanding || self == .shrinked
    }
}
