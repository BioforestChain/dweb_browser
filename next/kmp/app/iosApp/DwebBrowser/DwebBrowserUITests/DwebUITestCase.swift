//
//  DwebUITestCase.swift
//  DwebBrowserUITests
//
//  Created by instinct on 2024/2/28.
//  Copyright © 2024 orgName. All rights reserved.
//

import XCTest

class DwebUITestCase: XCTestCase {
    
    let app = XCUIApplication()
    
    override func setUpWithError() throws {
        app.launchEnvironment = ["DWEB_TEST_MODE" : "DwebUITesting"]
//        app.launch()

//        XCTAssertFalse(app.staticTexts["loading"].waitForExistence(timeout: 5))
//        
//        let url = URL(string: "dweb://openinbrowser")
//        app.open(url!)
    }
}
