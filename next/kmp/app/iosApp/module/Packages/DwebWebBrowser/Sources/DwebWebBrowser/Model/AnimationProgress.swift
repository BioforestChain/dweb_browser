//
//  ShiftAnimation.swift
//  DwebBrowser
//
//  Created by ui06 on 5/28/23.
//

import Foundation

enum AnimationProgress: Int, Equatable{
    

    //shrink前置条件 1.获取截图 - 2.获取cell位置（grid设置opacity为0.01，并scale到原始尺寸，需要滚动grid）-  startShrink（grid设置opacity为1，scale为0.8）
    // 1和2是同时进行
    case obtainedSnapshot
    case obtainedCellFrame
    case startExpanding
    case startShrinking

    case invisible
    
    func isAnimating() -> Bool{
        return self.rawValue >= AnimationProgress.startExpanding.rawValue && self.rawValue < AnimationProgress.invisible.rawValue
    }
}
