//
//  TabsCollectionView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import SwiftUI
import QGrid

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
    @ObservedObject var historyStore = HistoryStore()
    
    var body: some View {
        QGrid(Storage.people,
              columns: 2,
              columnsInLandscape: 4,
              vSpacing: 20,
              hSpacing: 20,
              vPadding: 15,
              hPadding: 20) { person in
            GridCell(person: person)
        }
    }
}

struct GridCell: View {
    var person: Person
    @State var runCount = 0
    var body: some View {
//        GeometryReader{ geometry in
            VStack() {
                Image(person.imageName)
                    .resizable()
                    .scaledToFit()
                
                    .shadow(color: .primary, radius: 5)
                    .padding([.horizontal, .top], 7)
                    .onTapGesture {
//                        print("VStack was tapped!")
                    }
                Text(person.lastName).lineLimit(1)
            }
            .padding(.bottom, 60)
            .background(.cyan)
            
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(Color.blue, lineWidth: 3)
            )
            .cornerRadius(10)
            .onTapGesture {
                
                DispatchQueue.main.asyncAfter(deadline: .now() + 1, execute: {
                    runCount += 1
                })
                print("cell tapped")
                let uiImage = self.snapshot()
                print(uiImage.size)
//                print(geometry.frame(in: .global))
            }
        }
        
//    }
}

struct TabsCollectionView_Previews: PreviewProvider {
    static var previews: some View {
        
        TabsCollectionView()
            .frame(height: 754)
        
    }
}
