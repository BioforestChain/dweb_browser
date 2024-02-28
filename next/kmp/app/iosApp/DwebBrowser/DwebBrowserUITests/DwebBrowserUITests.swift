//
//  DwebBrowserUITests.swift
//  DwebBrowserUITests
//
//  Created by instinct on 2024/2/28.
//  Copyright © 2024 orgName. All rights reserved.
//

import XCTest

final class DwebBrowserUITests: DwebUITestCase {

    override func setUpWithError() throws {
        try super.setUpWithError()
    }

    override func tearDownWithError() throws {
        
    }

    func testLaunchPerformance() throws {
        if #available(macOS 10.15, iOS 13.0, tvOS 13.0, watchOS 7.0, *) {
            // This measures how long it takes to launch your application.
            measure(metrics: [XCTApplicationLaunchMetric()]) {
                XCUIApplication().launch()
            }
        }
    }
    
    func testWebWebBrowser() throws {
  
//        app.launch()
        
//        XCTAssertFalse(app.staticTexts["loading"].waitForExistence(timeout: 5))
        
        let url = URL(string: "dweb://openinbrowser")
//        app.open(url!)
        
//        XCUIDevice.shared.system.open(url!)
        
        print("FUck")
//        let app = XCUIApplication()
//        XCTAssert(app.textFields["搜索或输入网址"].waitForExistence(timeout: 5))
    }
}
