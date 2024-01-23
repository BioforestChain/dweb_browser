//
//  MenuView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/6.
//

import SwiftUI
struct MenuView: View {

    @EnvironmentObject var dragScale: WndDragScale
    @Environment(\.modelContext) var modelContext

    @State private var isTraceless: Bool = TracelessMode.shared.isON
    @State var toastOpacity: CGFloat = 0.0

    @State private var offsetY: CGFloat = 300
    let webCache: WebCache
    var body: some View {
        ZStack {
            ScrollView(.vertical) {
                VStack(spacing: dragScale.properValue(floor: 5, ceiling: 16)) {
                    Button {
                        addToBookmark()
                    } label: {
                        MenuCell(title: "添加书签", imageName: "bookmark")
                    }

                    ShareLink(item: webCache.lastVisitedUrl.absoluteString) {
                        MenuCell(title: "分享", imageName: "share")
                    }

                    tracelessView
                }
                .padding(.vertical, dragScale.properValue(floor: 10, ceiling: 32))
                .background(.bk)
                .frame(maxWidth: .infinity)

//                toast
//                    .opacity(toastOpacity)
//                    .scaleEffect(dragScale.onWidth)
            }
        }
    }

    var toast: some View {
        Text("已添加至书签")
            .frame(width: 150, height: 50)
            .background(.black.opacity(0.5))
            .cornerRadius(25)
            .foregroundColor(.white)
            .font(.system(size: 15))
    }

    var tracelessView: some View {
        HStack {
            Text("无痕模式")
                .foregroundColor(.primary)
                .font(.system(size: dragScale.scaledFontSize(maxSize: 16)))
                .padding(.leading, 16)

            Toggle("", isOn: $isTraceless)
                .scaleEffect(dragScale.onWidth)
        }
        .frame(height: 50 * dragScale.onWidth)
        .background(Color.menubk)
        .cornerRadius(6)
        .padding(.horizontal, 16)
        .onChange(of: isTraceless) { _, newValue in
            TracelessMode.shared.isON = newValue
        }
    }

    private func addToBookmark() {
        Task(priority: .background) {
            var imgData: Data? = nil
            if webCache.webIconUrl.isFileURL {
                imgData = try? Data(contentsOf: webCache.webIconUrl)
            } else {
                let (data, response) = try await URLSession.shared.data(from: webCache.webIconUrl)
                guard let httpResponse = response as? HTTPURLResponse, (200 ... 299).contains(httpResponse.statusCode) else {
                    Log("Invalid or unsuccessful HTTP response")
                    return
                }
                let image = UIImage(data: data)?.resize(toSize: CGSize(width: 32, height: 32))
                imgData = image?.pngData()
            }
            browserViewDataSource.addBookmark(title: webCache.title, url: webCache.lastVisitedUrl.absoluteString, icon: imgData)
        }
        
        withAnimation {
            toastOpacity = 1.0
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            withAnimation {
                toastOpacity = 0
            }
        }
    }
}
