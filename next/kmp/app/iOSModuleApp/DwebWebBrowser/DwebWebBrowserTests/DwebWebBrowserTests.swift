//
//  DwebWebBrowserTests.swift
//  DwebWebBrowserTests
//
//  Created by instinct on 2024/1/2.
//

import XCTest
import DwebWebBrowser

final class DwebWebBrowserTests: XCTestCase {

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    func testBundleImg() throws {
        let b = Bundle.browser
        let img = UIImage(named: "def_web_icon", in: Bundle.browser, with: nil)
        XCTAssert(img != nil, "img load fail")
    }
    
    func testExample() throws {
        // This is an example of a functional test case.
        // Use XCTAssert and related functions to verify your tests produce the correct results.
        // Any test you write for XCTest can be annotated as throws and async.
        // Mark your test throws to produce an unexpected failure when your test encounters an uncaught error.
        // Mark your test async to allow awaiting for asynchronous code to complete. Check the results with assertions afterwards.
    }

    func testPerformanceExample() throws {
        // This is an example of a performance test case.
        measure {
            // Put the code you want to measure the time of here.
        }
    }

}
