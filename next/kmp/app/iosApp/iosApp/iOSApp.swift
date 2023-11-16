import DwebShared
import Network
import SwiftUI
import UIKit
import WebKit
import WebRTC

let DWEB_OS = true

struct DwebFrameworkComposeView: UIViewControllerRepresentable {
  var vc: UIViewController
  func makeUIViewController(context: Context) -> UIViewController {
    vc
  }
  func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

actor Channel<T> {
  private var data: T? = nil
  private var cb: (T?) -> Void = { _ in }

  func send(_ u: T) {
    data = u
    cb(u)
  }
  func listen(cb: @escaping (T?) -> Void) {
    self.cb = cb
  }
  func get() -> T? {
    data
  }
}

let vcChannel = Channel<DwebRootUIViewController?>()
let createHook = { (id: KotlinInt) -> KotlinUnit in

  DispatchQueue.main.async {
    let vc = DwebRootUIViewController(vcId: Int(truncating: id))
    Task {
      await vcChannel.send(vc)
    }
  }
  return KotlinUnit()
}
let updateHook = { (updateVC: UIViewController) -> KotlinUnit in
  let vc = updateVC as? DwebRootUIViewController
  Task {
    await vcChannel.send(vc)
  }
  return KotlinUnit()
}

@main
struct iOSApp: App {
  @UIApplicationDelegateAdaptor(DwebAppDelegate.self) var appDelegate
  @StateObject private var networkManager = NetworkManager()
  @State private var isNetworkSegmentViewPresented = false

  var body: some Scene {
    WindowGroup {
      DwebFrameworkContentView() .ignoresSafeArea(.all, edges: .bottom)  // Compose has own keyboard handler

    }
  }
  init() {

    Main_iosKt.dwebRootUIViewController_setCreateHook(createHook)
    Main_iosKt.dwebRootUIViewController_setUpdateHook(updateHook)
    Main_iosKt.startDwebBrowser(app: UIApplication.shared) { dnsNmm, error in
      if let error = error {
        print(error)
      } else {
        print("OKK \(dnsNmm)")
      }
    }
  }
}

struct DwebFrameworkContentView: View {
  @State var vc: DwebRootUIViewController? = nil
  var body: some View {
    ZStack {
      if let vc = vc {
        DwebFrameworkComposeView(vc: vc)
          .ignoresSafeArea(.all, edges: .bottom)  // Compose has own keyboard handler

      } else {
        Text("Loading...")
      }
    }.task {
      await vcChannel.listen(cb: { newVc in
        DispatchQueue.main.async {
          if let newVc = newVc {
            vc = newVc
          } else {
            vc = nil
          }
        }
      })
    }
  }
}

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
//    override func showDetailViewController(_ vc: UIViewController, sender: Any?) {
//        print("showDetailViewController vc:\(vc) sender:\(sender)")
//        super.showDetailViewController(vc, sender: sender)
//    }
//    override func show(_ vc: UIViewController, sender: Any?) {
//        print("show vc:\(vc) sender:\(sender)")
//        super.show(vc, sender: sender)
//    }
  deinit {
    Main_iosKt.dwebRootUIViewController_onDestroy(id: Int32(vcId))  // onDestroy
  }
}
