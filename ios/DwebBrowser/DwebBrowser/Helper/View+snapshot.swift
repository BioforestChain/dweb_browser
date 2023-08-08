//
//  SnapshotView3.swift
//  TableviewDemo
//
//  Created by ui06 on 4/18/23.
//

import Foundation
import SwiftUI
import UIKit
import WebKit

func printWithDate(msg: String = "") {
    let date = Date()
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    let dateString = formatter.string(from: date)
//#if DwebBrowser
    print("~~~~" + dateString + "--" + msg)
//#else
    ConsoleSwift.inject?("~~~~" + dateString + "--" + msg)
//#endif

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

    func takeSnapshot(completion: @escaping (UIImage) -> Void) {
        let uiView = UIView(frame: CGRect(origin: .zero, size: CGSize(width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height)))

        let hostingController = UIHostingController(rootView: self)
        hostingController.view.frame = uiView.bounds
        uiView.addSubview(hostingController.view)
        guard let view = hostingController.view else {
            return
        }
        DispatchQueue.main.async {
            UIGraphicsBeginImageContextWithOptions(view.bounds.size, false, 0)
            view.drawHierarchy(in: view.bounds, afterScreenUpdates: true)
            if let image = UIGraphicsGetImageFromCurrentImageContext() {
                UIGraphicsEndImageContext()
                completion(image)
                // 现在获取的image是正确的快照
            }
        }
    }

    func takeSnapshot9(completion: @escaping (UIImage) -> Void) {
        // Create a UIView
        let uiView = UIView(frame: CGRect(origin: .zero, size: CGSize(width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height)))

        // Add the view to the UIView
        let hostingController = UIHostingController(rootView: self)
        hostingController.view.frame = uiView.bounds
        uiView.addSubview(hostingController.view)

        // Delay the snapshot process slightly
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.06) {
            // Draw the visible portion of the screen
            UIGraphicsBeginImageContextWithOptions(uiView.bounds.size, false, UIScreen.main.scale)
            uiView.drawHierarchy(in: uiView.bounds, afterScreenUpdates: true)

            // Get the screenshot and return it
            let image = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()
            guard let image = image else { return }
            completion(image)
            // Do something with the image, or you can return it here
//                 return image
        }

//            return nil // Return nil temporarily (modify this based on how you want to handle the image)
    }
}

struct SnapshotViewWraperView: View {
    @State var capture: UIImage?
    let sampleView = SampleView()
    @State var hasSnapshot = false
    var body: some View {
        ScrollView {
            VStack {
                sampleView
                Button("Capture", action: {
                    printWithDate(msg: "before snapshot5: \(Thread.current)")

                    capture = sampleView.snapshot()
                    hasSnapshot = true

                    printWithDate(msg: "after snapshot5: \(Thread.current)")

                })
                .padding()
            }
            if hasSnapshot {
                Image(uiImage: capture!)
                    .resizable()
                    .frame(width: 300, height: 500)
            } else {
                Color.green
                    .frame(width: 300, height: 500)
            }
        }
    }
}

struct SampleView: View {
    var body: some View {
        ZStack {
            Rectangle().fill(.cyan)
            VStack {
                Image(uiImage: UIImage.bundleImage(name: "snapshot"))
                    .resizable()
                    .frame(width: 200, height: 300)
                    .font(.system(size: 80))
                    .background(in: Circle().inset(by: -40))
                    .background(.blue)
                    .foregroundStyle(.white)
                    .padding(10)
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
