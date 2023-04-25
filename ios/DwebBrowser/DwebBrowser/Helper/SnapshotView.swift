//
//  SnapshotView3.swift
//  TableviewDemo
//
//  Created by ui06 on 4/18/23.
//

import SwiftUI
import Foundation

func printDate(){
    let date = Date()
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    let dateString = formatter.string(from: date)
    print(dateString)
}

struct SnapshotView3_Previews: PreviewProvider {
    static var previews: some View {
        SnapshotViewWraperView()
    }
}

extension View {
    func snapshot() -> UIImage {
        let controller = UIHostingController(rootView: self)
        let view = controller.view
        
        let targetSize = controller.view.intrinsicContentSize
        //        let targetSize = controller.view.frame.size
        view?.bounds = CGRect(origin: .zero, size: targetSize)
        view?.backgroundColor = .clear
        
        let renderer = UIGraphicsImageRenderer(size: targetSize)
        
        return renderer.image { _ in
            view?.drawHierarchy(in: controller.view.bounds, afterScreenUpdates: true)
            print("in snapshot5: " , Thread.current)
            printDate()
        }
        
    }
}

struct SnapshotViewWraperView: View {
    @State var capture: UIImage?
    let sampleView = SampleView()
    
    var body: some View {
        HStack {
            ZStack(alignment: .bottom) {
                sampleView
                Button("Capture", action: {
                    print("before snapshot5: " , Thread.current )
                    printDate()
                    
                    capture = sampleView.snapshot()
                    
                    print("after snapshot5: " , Thread.current)
                    printDate()
                    
                })
                .padding()
            }
            if let image = capture {
                Image(uiImage: image)
            } else {
                Color.clear
            }
        }
    }
}

struct SampleView: View {
    var body: some View {
        ZStack {
            Rectangle().fill(.red)
            VStack {
                Image("profile")
                    .font(.system(size: 80))
                    .background(in: Circle().inset(by: -40))
                    .background(.blue)
                    .foregroundStyle(.white)
                    .padding(60)
                Text("Hello, world!")
                    .font(.largeTitle)
            }
        }
    }
}
