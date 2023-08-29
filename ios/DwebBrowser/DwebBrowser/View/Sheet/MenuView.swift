//
//  MenuView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/6.
//

import SwiftUI

struct MenuView: View {
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var webcacheStore: WebCacheStore
    @State private var isTraceless: Bool = TraceLessMode.shared.isON
    @State var isShare: Bool = false
    @State var isAlert: Bool = false
    private let titles: [String] = ["添加书签","分享"]
    private let imagesNames: [String] = ["bookmark", "share"]
    var webCache: WebCache { webcacheStore.cache(at: selectedTab.curIndex) }

    @State private var offsetY: CGFloat = 300
    
    var body: some View {
        
        VStack(spacing: 16) {
            
            ForEach(0..<titles.count, id: \.self) { index in
                if index == 0 {
                    Button {
                        addToBookmark()
                    } label: {
                        MenuCell(title: titles[index], imageName: imagesNames[index])
                    }
                } else if index == 1 {
                    ShareLink(item: webCache.lastVisitedUrl.absoluteString) {
                        MenuCell(title: titles[index], imageName: imagesNames[index])
                    }
                }
            }
            
            Toggle(isOn: $isTraceless ) {
                Text("无痕模式")
                    .padding(16)
                    .foregroundColor(Color.menuTitleColor)
                    .font(.system(size: 16))
            }
            .onChange(of: isTraceless, perform: { newValue in
                TraceLessMode.shared.isON = newValue
            })
            .toggleStyle(CustomToggleStyle())
            .frame(height: 50)
            .background(Color.menubkColor)
            .cornerRadius(6)
            .padding(.horizontal, 16)
            
            
            if isAlert {
                ZStack {
                    Text("已添加至书签")
                        .frame(width: 150, height: 50)
                        .background(SwiftUI.Color.black.opacity(0.35))
                        .cornerRadius(25)
                        .foregroundColor(.white)
                        .font(.system(size: 15))
                        .offset(y: offsetY)
                }
            }
        }
        .background(Color.bkColor)
        
    }
    
    private func addToBookmark() {
    
//        let webCache = WebCacheStore.shared.store[selectedTab.curIndex]
        let manager = BookmarkCoreDataManager()
        let bookmark = LinkRecord(link: webCache.lastVisitedUrl.absoluteString, imageName: webCache.webIconUrl.absoluteString, title: webCache.title, createdDate: Date().milliStamp)
        let result = manager.insertBookmark(bookmark: bookmark)
        if result {
            alertAnimation()
        }
    }
    
    private func alertAnimation() {
        isAlert.toggle()
        withAnimation {
            offsetY = safeAreaBottomHeight
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            withAnimation {
                offsetY = 300
            }
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            self.isAlert.toggle()
        }
    }
}

struct MenuView_Previews: PreviewProvider {
    static var previews: some View {
        MenuView()
    }
}


struct CustomToggleStyle: ToggleStyle {
    
    func makeBody(configuration: Configuration) -> some View {
        Toggle(configuration)
            .padding(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 12))
    }
}
