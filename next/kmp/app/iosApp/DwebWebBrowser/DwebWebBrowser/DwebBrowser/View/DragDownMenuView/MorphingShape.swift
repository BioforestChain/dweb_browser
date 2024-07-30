//
//  EggShapView.swift
//  PullMenu
//
//  Created by ui06 on 7/16/24.
//

import SwiftUI


struct MorphingShape: Shape {
    var xDragOffset: Double // 0.0 (圆形) 到 1.0 (椭圆形)
    var morphProgress: Double {
        if xDragOffset > 0 {
            return min(1.0, xDragOffset / minXOffsetToChangeAction)
        } else {
            return max(-1.0, xDragOffset / minXOffsetToChangeAction)
        }
    }

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let width = min(rect.width, rect.height)
        let height = width
        let centerX = rect.width / 2
        let centerY = rect.height / 2
        let radius = height / 2 // 增加纵向半径以形成椭圆形

        for angle in stride(from: 0, to: 360, by: 1) {
            let angleInRadians = Angle(degrees: Double(angle)).radians
            var x: CGFloat = 0.0

            if morphProgress > 0 {
                x = angle < 180 ? centerX + CGFloat(sin(angleInRadians)) * radius + width * CGFloat(morphProgress) * 0.4 : centerX + CGFloat(sin(angleInRadians)) * radius
            } else {
                x = angle > 180 ? centerX + CGFloat(sin(angleInRadians)) * radius + width * CGFloat(morphProgress) * 0.4 : centerX + CGFloat(sin(angleInRadians)) * radius
            }

            let y = centerY + CGFloat(cos(angleInRadians)) * radius

            if angle == 0 {
                path.move(to: CGPoint(x: x, y: y))
            } else {
                path.addLine(to: CGPoint(x: x, y: y))
            }
        }
        path.closeSubpath()
        return path
    }
}


struct ShowView: View {
    @State private var morphProgress: Double = 0.0
    
    var body: some View {
        VStack {
            MorphingShape(xDragOffset: morphProgress)
                .frame(width: 50, height: 50)
            
            Slider(value: $morphProgress, in: -80...80)
                .padding()
            
            Text("Morph Progress: \(morphProgress, specifier: "%.2f")")
        }
    }
}

#Preview {
    ShowView()
}

