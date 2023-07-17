//
//  SwipeGesture.swift
//  SwiftUIDemo
//
//  Created by ui03 on 2023/7/14.
//

import SwiftUI

struct SwipeGesture: Gesture {
    
    enum Direction: String {
        case left, right, up, down
    }
    
    typealias Value = Direction
    
    private let minimumDistance: CGFloat
    private let coordinateSpace: CoordinateSpace
    
    init(minimumDistance: CGFloat = 5, coordinateSpace: CoordinateSpace = .local) {
        self.minimumDistance = minimumDistance
        self.coordinateSpace = coordinateSpace
    }
    
    var body: AnyGesture<Value> {
        AnyGesture(
            DragGesture(minimumDistance: minimumDistance, coordinateSpace: coordinateSpace)
                .map({ value in
                    let hori = value.translation.width
                    let verti = value.translation.height
                    
                    if abs(hori) > abs(verti) {
                        if hori < 0 {
                            return .left
                        } else {
                            return .right
                        }
                    } else {
                        if verti < 0 {
                            return .up
                        } else {
                            return .down
                        }
                    }
                })
        )
    }
}

extension View {
    
    func onSwipe(minimumDistance: CGFloat = 5,
                 coordinateSpace: CoordinateSpace = .local,
                 perform: @escaping (SwipeGesture.Direction) -> Void) -> some View {
            gesture(
                SwipeGesture(minimumDistance: minimumDistance, coordinateSpace: coordinateSpace)
                    .onEnded(perform)
            )
        }
}
