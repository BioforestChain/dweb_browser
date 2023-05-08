//
//  TabsCollectionView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import SwiftUI

struct Person : Codable, Identifiable {
    var id: Int
    var firstName: String
    var lastName: String
    var imageName: String
}

struct Storage {
    static var people: [Person] = load("people.json")
    
    static func load<T: Decodable>(_ file: String) -> T {
        guard let url = Bundle.main.url(forResource: file, withExtension: nil),
              let data = try? Data(contentsOf: url),
              let typedData = try? JSONDecoder().decode(T.self, from: data) else {
            fatalError("Error while loading data from file: \(file)")
        }
        return typedData;
    }
}

struct TabsCollectionView: View {
    @EnvironmentObject var pages: WebPages
    
    var body: some View {
        Grid(pages.pages,
              columns: 2,
              columnsInLandscape: 4,
              vSpacing: 20,
              hSpacing: 20,
              vPadding: 10,
              hPadding: 20)
        { person in
            GridCell(page: person)
        }
        .background(Color(white: 0.7))
    }
}

struct GridCell: View {
    var page: WebPage
    @State var runCount = 0
//    var snapshotImage: Image
    var body: some View {
        //        GeometryReader{ geometry in
        ZStack(alignment: .topTrailing){
            VStack(spacing: 5) {
                Image(uiImage: page.snapshot)
                    .resizable()
                    .shadow(color: .secondary, radius: 3)
                    .cornerRadius(10)
                    .onTapGesture {
                        print("cell tapped")
                        let uiImage = self.snapshot()
                        print(uiImage.size)
                    }
                HStack{
                    AsyncImage(url: page.icon) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 25, height: 25)
                        
                    } placeholder: {
                        Color.clear
                            .frame(width: 25, height: 25)
                    }
                    
                    Text(page.title)
                        .fontWeight(.semibold)
                        .lineLimit(1)
                }
            }
            .aspectRatio(2.0/3.2, contentMode: .fit)
            
            
            Button {
                print("delete this tab, remove data from cache")
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
    }
}

struct TabsCollectionView_Previews: PreviewProvider {
    static var previews: some View {
        
        TabsCollectionView()
            .frame(height: 754)
        
    }
}



/*
 struct ContentView: View {
     @State private var image: UIImage?
     
     var body: some View {
         VStack {
             if let image = image {
                 Image(uiImage: image)
                     .resizable()
                     .shadow(color: .secondary, radius: 3)
                     .cornerRadius(10)
             } else {
                 Text("Tap to take a screenshot")
             }
         }
         .onTapGesture {
             if let view = UIApplication.shared.windows.first?.rootViewController?.view {
                 let renderer = UIGraphicsImageRenderer(size: view.bounds.size)
                 let image = renderer.image { ctx in
                     view.drawHierarchy(in: view.bounds, afterScreenUpdates: true)
                 }
                 self.image = image
             }
         }
     }
 }
 */
