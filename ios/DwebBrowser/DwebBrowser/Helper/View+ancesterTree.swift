//
//  ViewTree.swift
//  DwebBrowser
//
//  Created by ui06 on 5/10/23.
//

import SwiftUI

struct ViewTree {
    let view: Any
    var subviews: [ViewTree] = []
}

extension View {
    func viewTree() -> ViewTree {
        let view = Mirror(reflecting: self)
        var children: [ViewTree] = []
        for child in view.children {
            if let childView = child.value as? (any View) {
                children.append(childView.viewTree())
            }
            if let childViews = child.value as? [any View] {
                children.append(contentsOf: childViews.map { $0.viewTree() })
            }
        }
        return ViewTree(view: self, subviews: children)
    }
}
