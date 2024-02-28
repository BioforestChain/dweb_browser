//
//  DwebBrowserUITestsLaunchTests.swift
//  DwebBrowserUITests
//
//  Created by instinct on 2024/2/28.
//  Copyright © 2024 orgName. All rights reserved.
//

import XCTest

final class DwebBrowserUITestsLaunchTests: DwebUITestCase {

    override class var runsForEachTargetApplicationUIConfiguration: Bool {
        true
    }

    override func setUpWithError() throws {
        try super.setUpWithError()
        continueAfterFailure = false
    }

    func testLaunch() throws {
        let app = XCUIApplication()
        app.launch()

        // Insert steps here to perform after app launch but before taking a screenshot,
        // such as logging into a test account or navigating somewhere in the app

        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "Launch Screen"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
