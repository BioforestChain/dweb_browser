//
//  AdapterHandle.swift
//  BFExplorer
//
//  Created by ui03 on 2022/12/23.
//

import Foundation
import UIKit

protocol AdapterViewProtocol {
    
    var topView: UIView { get }
    var bottomView: UIView { get }
    var statusView: UIView { get }
}

struct AdapteeTarget {
    
    var topView: UIView
    var bottomView: UIView
    var statusView: UIView
    
    init(topView: UIView, bottomView: UIView, statusView: UIView) {
        self.topView = topView
        self.bottomView = bottomView
        self.statusView = statusView
    }
}
