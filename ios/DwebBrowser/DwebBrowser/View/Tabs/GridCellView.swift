//
//  GridCell.swift
//  DwebBrowser
//
//  Created by ui06 on 5/29/23.
//

import SwiftUI
import Kingfisher
import FaviconFinder

struct WebsiteIconImage: View{
    var iconUrl: URL
    var body: some View{
        KFImage.url(iconUrl)
            .fade(duration: 0.1)
            .onProgress { receivedSize, totalSize in print("dowloading icon right now \(receivedSize / totalSize) %") }
            .onSuccess { result in print("dowload icon done \(result)") }
            .onFailure { error in print("dowload icon failed \(error)") }
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 22)
    }
}

struct GridCell: View {
    @State var runCount = 0
    @ObservedObject var webCache: WebCache
    @EnvironmentObject var selectedTab: SelectedTab
    var isSelected: Bool

    var body: some View {
        Self._printChanges()
        
        return ZStack(alignment: .topTrailing){
            VStack(spacing: 0) {
                
                VStack{

                    Image(uiImage:  .snapshotImage(from: webCache.snapshotUrl))
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: gridCellW, height: gridCellH, alignment: .top)
                        .cornerRadius(gridcellCornerR)
                        .clipped()

                }
                
                .overlay( RoundedRectangle(cornerRadius: 10)
                .stroke(Color.dwebTint, lineWidth: 2)
                .opacity(isSelected ? 1:0)
                )
                HStack{
                    WebsiteIconImage(iconUrl: webCache.webIconUrl)
                        .onAppear{
                            fetchIconUrl()
                        }
                    Text(webCache.title)
                        .fontWeight(.semibold)
                        .lineLimit(1)
                }.frame(height: gridcellBottomH)
            }
            deleteButton
        }
    }
    
    func fetchIconUrl(){
        URL.downloadWebsiteIcon(iconUrl: webCache.lastVisitedUrl) { url in
            print("URL of Favicon: \(url)")
            DispatchQueue.main.async {
                webCache.webIconUrl = url
            }
        }
    }
    
    var deleteButton: some View{
        Button {
            print("delete this tab, remove data from cache")
            WebCacheMgr.shared.remove(webCache: webCache)
            if selectedTab.curIndex >= WebCacheMgr.shared.store.count{
                selectedTab.curIndex = WebCacheMgr.shared.store.count-1
            }
        } label: {
            Image("tab_close")
        }
        .padding(.top, 8)
        .padding(.trailing, 8)
    }
    
    
}
struct GridCell_Previews: PreviewProvider {
    static var previews: some View {
        //        GridCell(webCache: WebCache.example,isSelected: )
        //            .frame(width: 200,height: 300)
        Text("")
    }
}
