//
//  SSSSS.swift
//  iosApp
//
//  Created by instinct on 2023/11/17.
//  Copyright © 2023 orgName. All rights reserved.
//

import UIKit
import DwebShared

class DwebRootUIViewController: UIViewController {
  var vcId: Int = -1
  init(vcId: Int) {
    self.vcId = vcId
    super.init(nibName: nil, bundle: nil)
    Main_iosKt.dwebRootUIViewController_onInit(id: Int32(vcId), vc: self)  // onInit
    self.view.backgroundColor = .yellow
  }
  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }
  override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
    super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
  }
  override func viewDidLoad() {
    super.viewDidLoad()
    Main_iosKt.dwebRootUIViewController_onCreate(id: Int32(vcId))  // onCreate
  }
  override func viewWillAppear(_ animated: Bool) {
    super.viewWillAppear(animated)
    Main_iosKt.dwebRootUIViewController_onResume(id: Int32(vcId))  // onResume
  }
  override func viewDidDisappear(_ animated: Bool) {
    super.viewDidDisappear(animated)
    Main_iosKt.dwebRootUIViewController_onPause(id: Int32(vcId))  // onPause
  }

  deinit {
    Main_iosKt.dwebRootUIViewController_onDestroy(id: Int32(vcId))  // onDestroy
  }
}
