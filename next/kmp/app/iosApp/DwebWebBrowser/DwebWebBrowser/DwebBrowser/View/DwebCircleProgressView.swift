//
//  DwebCircleProgressView.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/12.
//

import Foundation
import SwiftUI

struct DwebCircleProgressView: View {
    let pauseAction: ()->Void
    let pause: Bool
    let progress: Float
    var body: some View {
        let _ = Self._printChanges()
        ProgressView(value: progress, total: 1)  {
            GeometryReader(content: { geometry in
                Button {
                    pauseAction()
                } label: {
                    if pause {
                        Image(systemName: "pause.circle" )
                            .font(.system(size: min(geometry.size.width/1.5, geometry.size.height/1.5)))
                            .tint(Color.black)
                    } else {
                        Color.clear
                    }
                }
                .frame(maxWidth: .infinity, maxHeight:.infinity)
            })
        }
        .progressViewStyle(DwebCircleProgressStyle(color: Color.blue.opacity(0.3), clockwise: false))
    }
}

struct DwebCircleProgressStyle: ProgressViewStyle {
    let color: Color
    let clockwise: Bool
    func makeBody(configuration: Configuration) -> some View {
        VStack {
            GeometryReader(content: { geo in
                let length = min(geo.size.width, geo.size.height)
                ZStack {
                    
                    Path { path in
                        path.move(to: CGPoint(x: geo.size.width/2.0,
                                              y: geo.size.height/2.0))
                        
                        path.addArc(center: CGPoint(x: geo.size.width/2.0,
                                                    y: geo.size.height/2.0),
                                    radius: length/2.0,
                                    startAngle: .degrees(0),
                                    endAngle: .degrees(360 * (configuration.fractionCompleted ?? 0.0)),
                                    clockwise: !clockwise)
                    }
                    .rotation(.degrees(-90))
                    .fill(color.opacity(0.5))
                    
                    Path { path in
                        path.move(to: CGPoint(x: geo.size.width/2.0,
                                              y: geo.size.height/2.0))
                        
                        path.addArc(center: CGPoint(x: geo.size.width/2.0,
                                                    y: geo.size.height/2.0),
                                    radius: length/2.0,
                                    startAngle: .degrees(0),
                                    endAngle: .degrees(360 * (configuration.fractionCompleted ?? 0.0)),
                                    clockwise: clockwise)
                    }
                    .rotation(.degrees(-90))
                    .fill(color)
                    
                    configuration
                        .label
                }
            })
        }
        .aspectRatio(1.0, contentMode: .fit)
    }
}
