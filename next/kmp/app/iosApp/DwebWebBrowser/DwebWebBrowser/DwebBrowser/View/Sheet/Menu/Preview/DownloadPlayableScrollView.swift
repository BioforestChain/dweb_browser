//
//  DownloadPlayableScrollView.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/19.
//

import Foundation
import SwiftUI

public struct DwebUIScollView<Content>: UIViewRepresentable where Content: View {
    public typealias UIViewType = UIScrollView
    
    public class Coordinator: NSObject, UIScrollViewDelegate {
        let parent: DwebUIScollView
        init(parent: DwebUIScollView) {
            self.parent = parent
        }
        
        public func scrollViewDidScroll(_ scrollView: UIScrollView) {
            Log()
            parent.scrolledAction?(scrollView.contentOffset)
        }
        
        public func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
            Log()
            parent.willDragAction?()
        }
        
        public func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
            Log()
            if !decelerate {
                parent.endDeceleratedAction?()
            }
        }
        
        public func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
            Log()
            parent.endDeceleratedAction?()
        }
    }
    
    public func makeCoordinator() -> Coordinator {
        return Coordinator(parent: self)
    }
    
    public func makeUIView(context: Context) -> UIScrollView {
        let scrollView = UIScrollView(frame: CGRect(x: 0, y: 0, width: 100, height: 40))
        scrollView.delegate = context.coordinator
        let hostVC = UIHostingController(rootView: contentBuilder())
        hostVC.view.sizeToFit()
        
        scrollView.addSubview(hostVC.view)
        scrollView.contentSize = hostVC.view.frame.size
        
        hostVC.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            hostVC.view.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            hostVC.view.trailingAnchor.constraint(greaterThanOrEqualTo: scrollView.trailingAnchor),
            hostVC.view.topAnchor.constraint(equalTo: scrollView.topAnchor),
            hostVC.view.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
        ])
        return scrollView
    }
    
    public func updateUIView(_ uiView: UIScrollView, context: Context) {}
    
    let contentBuilder: ContentAction
    var willDragAction: WillDragAction?
    var endDeceleratedAction: DeceleratedAction?
    var scrolledAction: ScrolledAction?
    
    public typealias ContentAction = () -> Content
    public typealias ScrolledAction = (CGPoint) -> Void
    public typealias WillDragAction = () -> Void
    public typealias DeceleratedAction = () -> Void
    
    public init(@ViewBuilder content: @escaping () -> Content,
                scrolled: ((CGPoint) -> Void)? = nil,
                willDrage: (() -> Void)? = nil,
                endDecelerated: (() -> Void)? = nil)
    {
        contentBuilder = content
        scrolledAction = scrolled
        willDragAction = willDrage
        endDeceleratedAction = endDecelerated
    }
}

struct ScrollableViewOffsetInfo: Equatable {
    var width: CGFloat
    var x: CGFloat
    
    static func == (lhs: Self, rhs: Self) -> Bool {
        return lhs.x == rhs.x && lhs.width == rhs.width
    }
}

struct ScrollableViewOffsetKey: PreferenceKey {
    typealias Value = ScrollableViewOffsetInfo
    static var defaultValue = ScrollableViewOffsetInfo(width: 0, x: 0)
    static func reduce(value: inout Value, nextValue: () -> Value) {
        value.width += nextValue().width
        value.x += nextValue().x
//        value += nextValue()
    }
}

struct DownloadAudioPCMScrollView<Content>: View, Animatable where Content: View {
    @State private var offX: CGFloat = .zero
    @GestureState private var moveX: CGFloat = .zero
    @State private var contentWidth: CGFloat = .zero
    @State private var startOffX: CGFloat = .zero
    @Binding var progress: CGFloat
    let builder: () -> Content
    let onDrag: () -> Void
    let endDrag: (Float) -> Void
    let backColor: Color
    
    private var currentProgress: Float {
        return Float(-(offX - progress * contentWidth) / contentWidth)
    }
    
    init(_ backColor: Color = .gray.opacity(0.3),
         progress: Binding<CGFloat>,
         @ViewBuilder content: @escaping () -> Content,
         onDrag: @escaping () -> Void,
         endDrag: @escaping (Float) -> Void) {
        self.backColor = backColor
        self._progress = progress
        self.builder = content
        self.onDrag = onDrag
        self.endDrag = endDrag
    }
    
    var body: some View {
        ZStack {
            GeometryReader(content: { geometry in
                ZStack {
                    backColor
                    builder()
                }
                .offset(x: offX + moveX + startOffX - progress * contentWidth)
                .background(GeometryReader {
                    Color.clear
                        .preference(key: ScrollableViewOffsetKey.self,
                                    value: ScrollableViewOffsetInfo(width: $0.frame(in: .named("stack")).size.width,
                                                                    x: $0.frame(in: .named("stack")).origin.x))
                })
                .onPreferenceChange(ScrollableViewOffsetKey.self) { value in
                    contentWidth = value.width
                    startOffX = geometry.size.width / 2.0
                    Log("total OFFX: \(value.x)")
                }
                .gesture(
                    DragGesture()
                        .updating($moveX, body: { value, state, _ in
                            state = value.translation.width
                            onDrag()
                        })
                        .onEnded { value in
                            offX += value.translation.width
                            Log("offx: \(offX)")

                            let maxX: CGFloat = (geometry.size.width) / 2.0 - startOffX + progress * contentWidth
                            let minX: CGFloat = (geometry.size.width) / 2.0 - contentWidth - startOffX + progress * contentWidth

                            var isNeedAnimation = false
                            if abs(value.velocity.width) > 100 {
                                isNeedAnimation = true
                            } else if offX < minX {
                                isNeedAnimation = true
                            } else if offX > maxX {
                                isNeedAnimation = true
                            }
                                
                            if isNeedAnimation {
                                withAnimation(.spring(.smooth)) {
                                    offX += value.velocity.width * 0.1
                                    offX = max(minX, offX)
                                    offX = min(maxX, offX)
                                } completion: {
                                    let p = currentProgress
                                    offX = 0
                                    endDrag(p)
                                }
                            } else {
                                let p = currentProgress
                                offX = 0
                                endDrag(p)
                            }
                        }
                )
                .coordinateSpace(name: "stack")
            })
        }
        .clipped()
    }
}
