//
//  TouchThroughView.swift
//  iosApp
//
//  Created by instinct on 2023/11/27.
//  Copyright © 2023 orgName. All rights reserved.
//

import DwebShared
import UIKit

class TouchThroughView: UIView {
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
//        Log { "SUBVIEWS = \n\(logUIViews(views: Array(subviews), deep: 3))" }
        
//        Log("hitTest: \(point)")
        if (point.x < 10 || point.x > bounds.width - 10) {
            return nil
        }
        
        for v1 in subviews.reversed() {
            for v2 in v1.subviews.reversed() {
                if let target = v2.hitTest(v2.convert(point, from: self), with: event) {
//                    Log { "HITTEST = \(target) \n\t\tin \(v2)" }
                    return target
                }
            }
        }
        return nil
    }
}

func logUIViews(views: [UIView], prefix: String = "\t", deep: Int = 0) -> String {
    views.enumerated().map { index, element in
        let hashCode = ("\(element)".components(separatedBy: ";").first ?? "<") + ">"
        var log = "\(prefix)\(index). \(String(describing: type(of: element))):\(hashCode)"
        if deep > 0 {
            log += "\n" + logUIViews(views: element.subviews, prefix: prefix + "\t", deep: deep - 1)
        }
        return log
    }.joined(separator: "\n")
}
