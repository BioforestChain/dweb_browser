//
//  View+AnimationObserver.swift
//  DwebBrowser
//
//  Created by ui06 on 5/13/23.
//

import SwiftUI

struct AnimationObserverModifier<Value: VectorArithmetic>: AnimatableModifier {
    // this is the view property that drives the animation - offset, opacity, etc.
    private let observedValue: Value
    private let onChange: ((Value) -> Void)?
    private let onComplete: (() -> Void)?
    
    // SwiftUI implicity sets this value as the animation progresses
    public var animatableData: Value {
        didSet {
            notifyProgress()
        }
    }
    
    public init(for observedValue: Value,
                onChange: ((Value) -> Void)?,
                onComplete: (() -> Void)?) {
        self.observedValue = observedValue
        self.onChange = onChange
        self.onComplete = onComplete
        animatableData = observedValue
    }
    
    public func body(content: Content) -> some View {
        content
    }
    
    private func notifyProgress() {
        DispatchQueue.main.async {
            onChange?(animatableData)
            if animatableData == observedValue {
                onComplete?()
            }
        }
    }
}

public extension View {
    func animationObserver<Value: VectorArithmetic>(for value: Value,
                                                    onChange: ((Value) -> Void)? = nil,
                                                    onComplete: (() -> Void)? = nil) -> some View {
        self.modifier(AnimationObserverModifier(for: value,
                                                onChange: onChange,
                                                onComplete: onComplete))
    }
}

struct AnimationObserverTest: View {
    @State private var offset = 0.0
    @State private var offsetSpan: ClosedRange<Double> = 0...1
    @State private var progressPercentage = 0.0
    @State private var isDone = false
    
    var body: some View {
        GeometryReader { geo in
            VStack {
                Text("Loading: \(progressPercentage)%")
                Rectangle()
                    .foregroundColor(.blue)
                    .frame(height: 50)
                    .offset(x: offset)
                    .animationObserver(for: offset) { progress in // HERE
                        progressPercentage = 100 * abs(progress - offsetSpan.lowerBound)
                        / (offsetSpan.upperBound - offsetSpan.lowerBound)
                    } onComplete: {
                        isDone = true
                    }
                
                if isDone {
                    Text("Done!")
                } else if progressPercentage >= 50 {
                    Text("Woooooah, we're half way there...")
                }
                
                Button("Reload") {
                    isDone = false
                    offset = -geo.size.width
                    offsetSpan = offset...0
                    withAnimation(.easeIn(duration: 5)) {
                        offset = 0
                    }
                }
            }
        }
    }
}



struct MyAnimationObserverModifier: AnimatableModifier {
    // this is the view property that drives the animation - offset, opacity, etc.
    private let observedValue: Int
    private let onChange: ((Int) -> Void)?
    private let onComplete: (() -> Void)?
    
    // SwiftUI implicity sets this value as the animation progresses
    public var animatableData: Int {
        didSet {
            notifyProgress()
        }
    }
    
    public init(for observedValue: Int,
                onChange: ((Int) -> Void)?,
                onComplete: (() -> Void)?) {
        self.observedValue = observedValue
        self.onChange = onChange
        self.onComplete = onComplete
        animatableData = observedValue
    }
    
    public func body(content: Content) -> some View {
        content
    }
    
    private func notifyProgress() {
        DispatchQueue.main.async {
            onChange?(animatableData)
            if animatableData == observedValue {
                onComplete?()
            }
        }
    }
}

public extension View {
    func myAnimationObserver(for value: Int,
                             onChange: ((Int) -> Void)? = nil,
                             onComplete: (() -> Void)? = nil) -> some View {
        self.modifier(MyAnimationObserverModifier(for: value,
                                                  onChange: onChange,
                                                  onComplete: onComplete))
    }
}


struct MyAnimationObserverTest: View {
    @State private var showRound = 0
    @State private var isDone = false
    @Namespace private var shapechange
    var body: some View {
        VStack {
            
            if showRound == 1{
                Circle()
                    .foregroundColor(.red)
                    .frame(width: 80, height: 80)
                
            }else{
                Rectangle()
                    .fill(Color.blue)
                    .frame(width: 200, height: 100)
                
            }
            Spacer()
            Button("change shape") {
                
                withAnimation(.easeIn(duration: 0.5)) {
                    showRound = abs(1 - showRound)
                }
            }
            
            if isDone{
                Text("now it's done")
            }else{
                Text("not yet")
                
            }
            
            if showRound == 1{
                Text("it's round")
            }else{
                Text("it's retangle")
            }
            
        }
        .myAnimationObserver(for: (showRound)){showR in
            //                if showR{
//            showRound = true
            isDone = true
            print(showR)
            //                }
        } onComplete: {
            isDone = true
        }
    }
}


struct View_AnimationObserver_Previews: PreviewProvider {
    static var previews: some View {
        AnimationObserverTest()
        Rectangle()
            .foregroundColor(.blue)
            .frame(height: 50)
            .offset(x: -50)
        
        MyAnimationObserverTest()
    }
}
