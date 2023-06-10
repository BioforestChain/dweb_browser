//
//  DownloadButtonView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/26.
//

import SwiftUI
import Combine

struct DownloadButtonView: View {
    
    @Binding private var content: String
    @Binding private var btn_width: CGFloat
    @Binding private var backColor: SwiftUI.Color
    @Binding private var isRotate: Bool
    @Binding private var isWaiting: Bool
    @Binding private var progress: CGFloat
    @Binding private var isLoading: Bool
    @State var oldContent: String = ""
    
    init(content: Binding<String>, btn_width: Binding<CGFloat>, backColor: Binding<SwiftUI.Color>, isRotate: Binding<Bool>, isWaiting: Binding<Bool>, isLoading: Binding<Bool>, progress: Binding<CGFloat>) {
        self._content = content
        self._btn_width = btn_width
        self._backColor = backColor
        self._isRotate = isRotate
        self._isWaiting = isWaiting
        self._progress = progress
        self._isLoading = isLoading
    }
    
    var body: some View {
        
        if progress > 0 {
            CircleProgressView()
        } else {
            if isWaiting {
                LoadingWaitCircle()
            } else {
                Button {
                    if content == "获取" || content == "更新"{
                        self.oldContent = content
                        withAnimation {
                            btn_width = 50
                            content = ""
                            backColor = .clear
                        }
                    } else {
                        let dict = ["type": "open"]
                        NotificationCenter.default.post(name: NSNotification.Name.downloadApp, object: nil, userInfo: dict)
                    }
                } label: {
                    Text(content)
                        .font(.system(size: 18))
                        .frame(width: btn_width, height: 30)
                        .foregroundColor(.white)
                        .background(backColor)
                        .font(.title3.bold())
                        .animation(.easeInOut, value: btn_width)
                        .cornerRadius(15)
                        .onAnimationCompleted(for: btn_width) {
                            if btn_width == 50 {
                                self.isWaiting = true
                            }
                        }
                }
            }
        }
    }
    
    @ViewBuilder
    func CircleProgressView() -> some View {
        
        ZStack {
            Circle()
                .stroke(lineWidth: 4)
                .opacity(0.5)
                .foregroundColor(SwiftUI.Color.primary.opacity(0.2))
            
            Circle()
                .trim(from: 0.0, to: CGFloat(progress))
                .stroke(style: StrokeStyle(lineWidth: 4, lineCap: .round, lineJoin: .round))
                .foregroundColor(.blue)
                .rotationEffect(Angle(degrees: -90.0))
                .animation(.easeOut(duration: 0.25), value: progress)
                .onAppear {
                    NotificationCenter.default.addObserver(forName: Notification.Name.downloadComplete, object: nil, queue: .main) { _ in
                        self.progress = 0
                        self.isWaiting = false
                        self.isLoading = false
                        withAnimation {
                            btn_width = 80
                            content = "打开"
                            backColor = SwiftUI.Color.blue
                        }
                    }
                    
                    NotificationCenter.default.addObserver(forName: Notification.Name.downloadFail, object: nil, queue: .main) { _ in
                        self.progress = 0
                        self.isWaiting = false
                        self.isLoading = false
                        withAnimation {
                            btn_width = 80
                            content = self.oldContent
                            backColor = SwiftUI.Color.blue
                        }
                    }
                }
                .onDisappear {
                    NotificationCenter.default.removeObserver(self)
                }
        }
        .frame(width: 30, height: 30)
    }
    
    @ViewBuilder
    func LoadingWaitCircle() -> some View {
        
        Circle()
            .trim(from: 0, to: 0.8)
            .stroke(lineWidth: 4)
            .foregroundColor(SwiftUI.Color.primary.opacity(0.2))
            .frame(width: 30, height: 30)
            .rotationEffect(.degrees(isRotate ? 360 : 0.0))
            .animation(SwiftUI.Animation.linear(duration: 1).repeatForever(autoreverses: false), value: isRotate)
            .onAppear {
                isRotate = true
                if !isLoading {
                    print("download")
                    isLoading = true
                    DispatchQueue.main.async {
                        let dict = ["type": "download"]
                        NotificationCenter.default.post(name: NSNotification.Name.downloadApp, object: nil, userInfo: dict)
                    }
                }
            }
    }
}

extension View {

    func onAnimationCompleted<Value: VectorArithmetic>(for value: Value, completion: @escaping () -> Void) -> ModifiedContent<Self, AnimationCompletionObserverModifier<Value>> {
        return modifier(AnimationCompletionObserverModifier(observedValue: value, completion: completion))
    }
}

struct AnimationCompletionObserverModifier<Value>: AnimatableModifier where Value: VectorArithmetic {

    /// While animating, SwiftUI changes the old input value to the new target value using this property. This value is set to the old value until the animation completes.
    var animatableData: Value {
        didSet {
            notifyCompletionIfFinished()
        }
    }

    /// The target value for which we're observing. This value is directly set once the animation starts. During animation, `animatableData` will hold the oldValue and is only updated to the target value once the animation completes.
    private var targetValue: Value

    /// The completion callback which is called once the animation completes.
    private var completion: () -> Void

    init(observedValue: Value, completion: @escaping () -> Void) {
        self.completion = completion
        self.animatableData = observedValue
        targetValue = observedValue
    }

    /// Verifies whether the current animation is finished and calls the completion callback if true.
    private func notifyCompletionIfFinished() {
        guard animatableData == targetValue else { return }

        /// Dispatching is needed to take the next runloop for the completion callback.
        /// This prevents errors like "Modifying state during view update, this will cause undefined behavior."
        DispatchQueue.main.async {
            self.completion()
        }
    }

    func body(content: Content) -> some View {
        /// We're not really modifying the view so we can directly return the original input value.
        return content
    }
}
