//
//  ViewModel.swift
//  SwiftUITest
//
//  Created by instinct on 2024/4/7.
//

import Foundation
import Observation
import SwiftUI
@Observable class ContentViewModel {
    var index: Int?
    var pageCountString: String {
        pages.count > 9 ? "9+" : "\(pages.count)"
    }
    var pageCountColor: Color {
        pages.count > 9 ? .red : .black
    }
    var pageCountFont: Font {
        pages.count < 10 ? .system(size: 12) : .system(size: 10)
    }
    
    func scrollIndex(by index: Int) -> Int? {
        guard index >= 0, index < pages.count else { return nil }
        let page = pages[index]
        return page.id.hashValue
    }
    
    // MARK: -Page items
    var pages: [PageModel] = [PageModel(), PageModel(url: .history), PageModel(url: .privacy), PageModel(url: .URL("https://www.baidu.com"))]    
    
    func page(at index: Int) -> PageModel {
        if index >= 0, index < pages.count {
            return pages[index]
        } else {
            return PageModel()
        }
    }
    
    //MARK: -more items
    var traceless = false
    @ObservationIgnored
    var moreItems: [ContainerMoreItem] {
        return [
            .bookmark(handleMoreAction),
            .tackless(false, handleMoreAction),
            .privacy(handleMoreAction),
            .scan(handleMoreAction),
            .download(handleMoreAction),
            .history(handleMoreAction)
        ]
    }

    var tracelessBinding: Binding<Bool> {
        Binding { [weak self] in
            self?.traceless ?? false
        } set: { [weak self] value in
            self?.traceless = value
        }
    }
    
    func handleMoreAction(event: ContainerMoreItem) {
        switch event {
            case .bookmark( _):
                print("bookmark")
            case .tackless(let bool, _):
                print("tackless:\(bool)")
            case .privacy( _):
                print("privacy")
            case .scan( _):
                print("scan")
            case .download( _):
                print("download")
            case .history( _):
                print("history")
        }
        
    }
}
