//
//  HotSearchData.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/24.
//

import SwiftUI
//import SwiftSoup

final class HotSearchDataManager: ObservableObject {
    
    private let manager = HotSearchCoreDataManager()
    private let urlString = "https://www.sinovision.net/portal.php?mod=center"
    
    func loadHotSearchData()  {
        
        Task {
            await loadNetworkHotSearchData()
        }
    }
    
    private func loadNetworkHotSearchData() async {
        guard let url = URL(string: urlString) else { return }
        let resposne = try? await URLSession.shared.data(for: URLRequest(url: url))
        guard let data = resposne?.0 else { return }
        let list = parseHTMLContent(data: data)
        guard list.count > 0 else { return }
        
        do {
            try await manager.deleteTotalHotSearchBatch()
            try await manager.addHotSearchBatch(with: list)
        } catch {
            print("insert hotSearch coredata fial")
        }
    }
    
    //解析html生成热搜对象
    private func parseHTMLContent(data: Data) -> [HotSearchModel] {
        var hotSearchs: [HotSearchModel] = []
        guard let html = String(data: data, encoding: .utf8) else { return hotSearchs }
      /*  do {
            let doc: Document = try SwiftSoup.parse(html)
            let hots: Elements? = try doc.getElementsByClass("t9_title item_div").select("a[href]")
            guard hots != nil else { return hotSearchs }
            let array = hots!.enumerated().reversed().count > 10 ? Array(hots!.enumerated().reversed()[0..<10]) : hots!.enumerated().reversed()
            
            for i in 0..<array.count {
                let (_, nodeEle) = array[i]
                let hot = HotSearchModel(link: try nodeEle.attr("href"), title: try nodeEle.attr("title"), index: i + 1)
                hotSearchs.append(hot)
            }
        } catch {
            print("parse HTML error")
        }*/
        return hotSearchs
    }
}
