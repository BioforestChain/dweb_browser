//
//  ShiftAnimation.swift
//  DwebBrowser
//
//  Created by ui06 on 5/28/23.
//

import Foundation

let fadingDuration = 0.01
var shiftingDuration: CGFloat = 0.5

enum AnimationProgress: Int{
    case initial
    case startExpanding
    case expanded

    case startShrinking
    case shrinked
    
    case invisible
    
    func isAnimating() -> Bool{
        return self.rawValue >= AnimationProgress.startExpanding.rawValue && self.rawValue < AnimationProgress.invisible.rawValue
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
