//
//  ShiftAnimation.swift
//  DwebBrowser
//
//  Created by ui06 on 5/28/23.
//

import Foundation

let fadingDuration = 0.1
let shiftingDuration: CGFloat = 0.5

enum AnimationProgress: Int{
    case none
    case initial
    case startExpanding
    case expanded

    case startShrinking
    case shrinked
    
    case fading
    case invisible
    
    case finished
    
    func isAnimating() -> Bool{
        return self.rawValue > AnimationProgress.initial.rawValue && self.rawValue < AnimationProgress.invisible.rawValue
    }
    
    func next()->AnimationProgress{
        switch self{
        case .startExpanding: return .expanded
        case .startShrinking: return .shrinked
        case .fading: return .invisible
        default: return .finished
        }
    }
    func imageIsLarge() -> Bool{
        return self == .startShrinking || self == .expanded
    }
    func imageIsSmall() -> Bool{
        return self == .startExpanding || self == .shrinked
    }
    
    func isOnStage() -> Bool{
        return self == .startExpanding
    }
}
