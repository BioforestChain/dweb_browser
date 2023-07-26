//
//  SnapshotView3.swift
//  TableviewDemo
//
//  Created by ui06 on 4/18/23.
//

import SwiftUI
import Foundation
import UIKit
import WebKit


func printWithDate(msg: String = ""){
    let date = Date()
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    let dateString = formatter.string(from: date)
    print(dateString + "--" + msg)
}

extension UIView {

    func asImage(rect: CGRect) -> UIImage {
        let renderer = UIGraphicsImageRenderer(bounds: rect)
        let image = renderer.image { context in
            layer.render(in: context.cgContext)
        }
        return image
    }
}

extension View {
    func snapshot2() -> UIImage? {
            // 创建UIView
            let uiView = UIView(frame: CGRect(origin: .zero, size: CGSize(width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height)))

            // 将视图添加到UIView上
            let hostingController = UIHostingController(rootView: self)
            hostingController.view.frame = uiView.bounds
            uiView.addSubview(hostingController.view)

            // 保存 WebView 的加载状态
            var webViewWasLoading = false
            if let webView = hostingController.view.subviews.first as? WKWebView {
                webViewWasLoading = webView.isLoading
                webView.stopLoading() // 停止加载
            }

            // 绘制屏幕可见区域
            UIGraphicsBeginImageContextWithOptions(uiView.bounds.size, false, UIScreen.main.scale)
            uiView.drawHierarchy(in: uiView.bounds, afterScreenUpdates: true)

            // 获取截图并输出
            let image = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()

            // 恢复 WebView 的加载状态
            if let webView = hostingController.view.subviews.first as? WKWebView, webViewWasLoading {
                webView.reload() // 重新加载
            }

            return image
        }
    
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
    
    func snapshot3() -> UIImage? {
        // 创建UIView
        let uiView = UIView(frame: CGRect(origin: .zero, size: CGSize(width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height)))

        // 将视图添加到UIView上
        let hostingController = UIHostingController(rootView: self)
        hostingController.view.frame = uiView.bounds
        uiView.addSubview(hostingController.view)

        // 获取CALayer
        guard let layer = uiView.layer.sublayers?.first else {
            return nil
        }

        // 绘制屏幕可见区域
        UIGraphicsBeginImageContextWithOptions(uiView.bounds.size, false, UIScreen.main.scale)
        guard let context = UIGraphicsGetCurrentContext() else {
            return nil
        }
        layer.render(in: context)

        // 获取截图并输出
        let image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return image
    }
}

struct SnapshotViewWraperView: View {
    @State var capture: UIImage?
    let sampleView = SampleView()
    @State var hasSnapshot = false
    var body: some View {
        ScrollView {
            VStack() {
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
                    .frame(width: 300,height: 500)
            } else {
                Color.green
                    .frame(width: 300,height: 500)
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
