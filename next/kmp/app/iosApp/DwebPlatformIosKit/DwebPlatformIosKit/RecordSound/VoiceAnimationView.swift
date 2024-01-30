//
//  VoiceAnimationView.swift
//  55
//
//  Created by ios on 2024/1/27.
//

import SwiftUI

struct VoiceAnimationView: View {
    
    @Binding var heights: [CGFloat]
    
    var body: some View {
        HStack(spacing: 3) {
            ForEach(0..<heights.count, id: \.self) { index in
                let height = heights[index]
                Rectangle()
                    .fill(height > 0 ? Color.red : Color.white)
                    .cornerRadius(0.5)
                    .frame(width: 1, height: height)
            }
        }
    }
}


