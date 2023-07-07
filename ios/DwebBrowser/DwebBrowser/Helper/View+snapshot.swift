//
//  SnapshotView3.swift
//  TableviewDemo
//
//  Created by ui06 on 4/18/23.
//

import SwiftUI
import Foundation
import UIKit


func printWithDate(msg: String = ""){
    let date = Date()
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    let dateString = formatter.string(from: date)
    print(dateString + "--" + msg)
}


extension View {
    func snapshot() -> UIImage? {
        // 创建UIView
        let uiView = UIView(frame: CGRect(origin: .zero, size: CGSize(width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height)))

        // 将视图添加到UIView上
        let hostingController = UIHostingController(rootView: self)
        hostingController.view.frame = uiView.bounds
        uiView.addSubview(hostingController.view)

        // 绘制屏幕可见区域
        UIGraphicsBeginImageContextWithOptions(uiView.bounds.size, false, UIScreen.main.scale)
        uiView.drawHierarchy(in: uiView.bounds, afterScreenUpdates: true)

        // 获取截图并输出
        let image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return image
    }
}

struct SnapshotViewWraperView: View {
    @State var capture: UIImage?
    let sampleView = SampleView()
    
    var body: some View {
        ScrollView {
            VStack() {
                sampleView
                Button("Capture", action: {
                    print("before snapshot5: " , Thread.current )
                    printWithDate()
                    
                    capture = sampleView.snapshot()
                    
                    print("after snapshot5: " , Thread.current)
                    printWithDate()
                    
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
                Image(uiImage: UIImage.bundleImage(name: "snapshot"))
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

struct SnapshotView3_Previews: PreviewProvider {
    static var previews: some View {
        SnapshotViewWraperView()
    }
}
