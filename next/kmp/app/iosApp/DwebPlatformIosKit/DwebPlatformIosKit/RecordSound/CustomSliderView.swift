//
//  CustomSliderView.swift
//  55
//
//  Created by ios on 2024/1/24.
//

import SwiftUI

struct CustomSliderView: View {
    
    @Binding var progress: CGFloat
    var totalTime: Int64 = 0
    @Binding var isDrag: Bool
    private let sliderWidth: CGFloat = 10
    
    var body: some View {
        
        GeometryReader(content: { geometry in
            ZStack(alignment: .leading) {
                Color.blue
                    .frame(height: 4)
                    .cornerRadius(2)
                    .frame(width: geometry.size.width)
                Color.gray
                    .frame(height: 4)
                    .cornerRadius(2)
                    .offset(x: progress * (geometry.size.width - sliderWidth))
                    .frame(width: geometry.size.width * (1 - progress) + sliderWidth * progress)
                    .animation(.smooth, value: progress)
                Circle()
                    .fill(.red)
                    .frame(width: sliderWidth, height: sliderWidth)
                    .offset(x: progress * (geometry.size.width - sliderWidth))
                    .animation(.smooth, value: progress)
                    .gesture(
                        DragGesture(minimumDistance: 0.0)
                            .onChanged({ value in
                                self.isDrag = true
                                let offset = value.location.x
                                if offset > 0 && offset < geometry.size.width - sliderWidth {
                                    self.progress = value.location.x / geometry.size.width
                                }
                            })
                            .onEnded({ _ in
                                self.isDrag = false
                            })
                    )
            }
        })
        
        
    }
}


