//
//  FloatingView.swift
//  DwebBrowserFloatingdDemo
//
//  Created by instinct on 2023/9/6.
//

import SwiftUI

final class FloatingParams: ObservableObject {
    @Published var floating: Bool = false
    @Published var width: CGFloat = 80
    @Published var height: CGFloat = 80

    var rect: CGRect = CGRect.zero
}

struct FloatingContentView: UIViewRepresentable {
    typealias UIViewType = UIView
    
    let contentView: UIView
    
    func makeUIView(context: Context) -> UIView {
        return contentView
    }
    
    func updateUIView(_ uiView: UIView, context: Context) {
        
    }
}

struct FloatingView: View {
        
    private let screenSize = UIScreen.main.bounds.size
    let contentView: UIView
    @ObservedObject var floatingParams: FloatingParams
    
    var blurBgView: some View {
        Color
            .clear
            .background(.thinMaterial)
            .transition(.opacity)
            .onTapGesture {
                floatingParams.floating.toggle()
            }
    }
    
    var body: some View {
        GeometryReader(content: { geometry in
            ZStack(alignment: .trailing) {
                Color.clear
                
                if floatingParams.floating {
                    blurBgView
                }
                
                FloatingContentView(contentView: contentView)
                .frame(width: floatingParams.width, height: floatingParams.height)
                .followDrag(mappers: {
                    let maps = FollowDragMap()
                    maps.dragingPoistionMapper = { p in
                        return dragingMapperFunc(p: p, size: geometry.size)
                    }
                    maps.dragedPoistionMapper = { p in
                        return dragedMapperFunc(p: p, size: geometry.size)
                    }
                    return maps
                }())
            }
        })
    }

    
    func dragedMapperFunc(p: CGPoint, size: CGSize) -> CGPoint {
        let center = (size.width - floatingParams.width / 2.0) / 2.0
        var result = CGPoint(x: 0.0, y: p.y)
        if center + p.x < 0  {
            result.x = floatingParams.width - size.width
        }
        return result
    }
    
    func dragingMapperFunc(p: CGPoint, size: CGSize) -> CGPoint {
        let offX = max(floatingParams.width - size.width, min(0, p.x))
        let offY = max(floatingParams.height / 2.0 - size.height/2.0, min(size.height/2.0 - floatingParams.height / 2.0, p.y))
        
        floatingParams.rect = CGRect(x: size.width + offX - floatingParams.width,
                                   y: size.height / 2.0 - floatingParams.height / 2.0 + offY,
                                   width: floatingParams.width,
                                   height: floatingParams.height)
        return CGPoint(x: offX, y: offY)
    }
}

class FollowDragMap: ObservableObject {
    var dragingPoistionMapper: ((CGPoint) -> CGPoint)?
    var dragedPoistionMapper: ((CGPoint) -> CGPoint)?
}

struct FollowDrag: ViewModifier {
    typealias PoistionMapper = ((CGPoint) -> CGPoint)?
    
    @ObservedObject var mappers: FollowDragMap
    
    @GestureState private var dragOffSet = CGPoint.zero
    @State private var floatingViewPosition = CGPoint.zero
    
    var dragGesture: some Gesture {
        DragGesture()
            .updating($dragOffSet) {gestureState, gestureOffSet, transcation in
                gestureOffSet = CGPoint(x: gestureState.translation.width, y: gestureState.translation.height)
                transcation.animation = Animation.linear
            }
            .onEnded { value in
                floatingViewPosition.x += value.translation.width
                floatingViewPosition.y += value.translation.height
                guard let mapper = mappers.dragedPoistionMapper else {
                    return
                }
                withAnimation {
                    floatingViewPosition = mapper(floatingViewPosition)
                }
            }
    }
    
    func body(content: Content) -> some View {
        content
            .offset({
                    guard let mapper = mappers.dragingPoistionMapper else {
                        return CGSize(width: floatingViewPosition.x + dragOffSet.x, height: floatingViewPosition.y + dragOffSet.y)
                    }
                    let p = mapper(CGPoint(x: floatingViewPosition.x + dragOffSet.x, y: floatingViewPosition.y + dragOffSet.y))
                    return CGSize(width: p.x, height: p.y)
                }()
            )
            .gesture(dragGesture)
    }
}

extension View {
    func followDrag(mappers: FollowDragMap = FollowDragMap()) -> some View {
        self.modifier(FollowDrag(mappers: mappers))
    }
}

