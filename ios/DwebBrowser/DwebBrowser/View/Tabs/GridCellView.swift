//
//  GridCell.swift
//  DwebBrowser
//
//  Created by ui06 on 5/29/23.
//

import SwiftUI
import Kingfisher
import FaviconFinder

struct GridCell: View {
    @State var runCount = 0
    @ObservedObject var webCache: WebCache
    var body: some View {
        ZStack(alignment: .topTrailing){
            VStack(spacing: 0) {
                Image(uiImage:  .snapshotImage(from: webCache.snapshotUrl))
                    .resizable()
                    .aspectRatio(contentMode: .fill)

                    .frame(width: gridCellW, height: gridCellH, alignment: .top)
                    .cornerRadius(gridcellCornerR)
                    .clipped()
                HStack{
                    webIconImage
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
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 26)
            }
            .padding(.top, 8)
            .padding(.trailing, 8)
            .buttonStyle(CloseTabStyle())
            .alignmentGuide(.top) { d in
                d[.top]
            }
            .alignmentGuide(.trailing) { d in
                d[.trailing]
            }
        }
    
    var webIconImage: some View{
        KFImage.url(webCache.webIconUrl)
            .fade(duration: 0.1)
            .onProgress { receivedSize, totalSize in print("dowloading icon right now \(receivedSize / totalSize) %") }
            .onSuccess { result in print("dowload icon done \(result)") }
            .onFailure { error in print("dowload icon failed \(error)") }
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 22)
    }
}
struct GridCell_Previews: PreviewProvider {
    static var previews: some View {
        GridCell(webCache: WebCache.example)
            .frame(width: 200,height: 300)
    }
}
