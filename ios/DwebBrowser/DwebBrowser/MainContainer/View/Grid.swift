//
//  MyGrid.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import SwiftUI


/// A container that presents rows of data arranged in multiple columns.
@available(iOS 13.0, OSX 10.15, *)
public struct Grid<Data, Content>: View
where Data : RandomAccessCollection, Content : View, Data.Element : Identifiable {
    private struct QGridIndex : Identifiable { var id: Int }
    
    // MARK: - STORED PROPERTIES
    
    private let columns: Int
    private let columnsInLandscape: Int
    private let vSpacing: CGFloat
    private let hSpacing: CGFloat
    private let vPadding: CGFloat
    private let hPadding: CGFloat
    private let isScrollable: Bool
    private let showScrollIndicators: Bool
    
    private let data: [Data.Element]
    private let content: (Data.Element) -> Content
    
    // MARK: - INITIALIZERS
    
    /// Creates a QGrid that computes its cells from an underlying collection of identified data.
    ///
    /// - Parameters:
    ///     - data: A collection of identified data.
    ///     - columns: Target number of columns for this grid, in Portrait device orientation
    ///     - columnsInLandscape: Target number of columns for this grid, in Landscape device orientation; If not provided, `columns` value will be used.
    ///     - vSpacing: Vertical spacing: The distance between each row in grid. If not provided, the default value will be used.
    ///     - hSpacing: Horizontal spacing: The distance between each cell in grid's row. If not provided, the default value will be used.
    ///     - vPadding: Vertical padding: The distance between top/bottom edge of the grid and the parent view. If not provided, the default value will be used.
    ///     - hPadding: Horizontal padding: The distance between leading/trailing edge of the grid and the parent view. If not provided, the default value will be used.
    ///     - isScrollable: Boolean that determines whether or not the grid should scroll
    ///     - content: A closure returning the content of the individual cell
    public init(_ data: Data,
                columns: Int,
                columnsInLandscape: Int? = nil,
                vSpacing: CGFloat = 10,
                hSpacing: CGFloat = 10,
                vPadding: CGFloat = 10,
                hPadding: CGFloat = 10,
                isScrollable: Bool = true,
                showScrollIndicators: Bool = false,
                content: @escaping (Data.Element) -> Content) {
        self.data = data.map { $0 }
        self.content = content
        self.columns = max(1, columns)
        self.columnsInLandscape = columnsInLandscape ?? max(1, columns)
        self.vSpacing = vSpacing
        self.hSpacing = hSpacing
        self.vPadding = vPadding
        self.hPadding = hPadding
        self.isScrollable = isScrollable
        self.showScrollIndicators = showScrollIndicators
    }
    
    // MARK: - COMPUTED PROPERTIES
    
    private var rows: Int {
        data.count / self.cols
    }
    
    private var cols: Int {
#if os(tvOS)
        return columnsInLandscape
#elseif os(macOS)
        return columnsInLandscape
#else
        return UIDevice.current.orientation.isLandscape ? columnsInLandscape : columns
#endif
    }
    
    /// Declares the content and behavior of this view.
    public var body : some View {
        GeometryReader { geometry in
            Group {
                if !self.data.isEmpty {
                    if self.isScrollable {
                        ScrollView(showsIndicators: self.showScrollIndicators) {
                            self.content(using: geometry)
                                .padding(.vertical, self.vPadding)
                            
                        }
                    } else {
                        self.content(using: geometry)
                            .padding(.vertical, self.vPadding)
                        
                    }
                }
            }
            .padding(.horizontal, self.hPadding)
        }
    }
    
    // MARK: - `BODY BUILDER` 💪 FUNCTIONS
    
    private func rowAtIndex(_ index: Int,
                            geometry: GeometryProxy,
                            isLastRow: Bool = false) -> some View {
        HStack(spacing: self.hSpacing) {
            ForEach((0..<(isLastRow ? data.count % cols : cols))
                .map { QGridIndex(id: $0) }) { column in
                    self.content(self.data[index + column.id])
                        .frame(width: self.contentWidthFor(geometry))
                }
            if isLastRow { Spacer() }
        }
    }
    
    private func content(using geometry: GeometryProxy) -> some View {
        VStack(spacing: self.vSpacing) {
            ForEach((0..<self.rows).map { QGridIndex(id: $0) }) { row in
                self.rowAtIndex(row.id * self.cols,
                                geometry: geometry)
            }
            // Handle last row
            if (self.data.count % self.cols > 0) {
                self.rowAtIndex(self.cols * self.rows,
                                geometry: geometry,
                                isLastRow: true)
            }
        }
    }
    
    // MARK: - HELPER FUNCTIONS
    
    private func contentWidthFor(_ geometry: GeometryProxy) -> CGFloat {
        let hSpacings = hSpacing * (CGFloat(self.cols) - 1)
        let width = geometry.size.width - hSpacings - hPadding * 2
        return width / CGFloat(self.cols)
    }
}

struct testContent: View{
    let pages = WebPages()
    
    var body: some View{
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


struct MyGrid_Previews: PreviewProvider {
    static var previews: some View {
        testContent()
    }
}
