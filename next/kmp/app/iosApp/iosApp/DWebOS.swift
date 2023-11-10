import DwebShared
import SwiftUI
import UIKit

struct DWebOS: View {
    let greet = Greeting().greet()
    var body: some View {
        ZStack {
            ComposeView()
                .ignoresSafeArea(.all) // Compose has own keyboard handler

        }.preferredColorScheme(.dark)
    }
}

struct ComposeView: UIViewControllerRepresentable {

    @State var size: CGSize = CGSize(width: 350, height: 500)
    func makeUIViewController(context: Context) -> UIViewController {
        let controller = Main_iosKt.MainViewController(iosView: UIHostingController(rootView: BrowserView(size: $size)).view) { (w,h) in
            size.width = CGFloat(truncating: w)
            size.height = CGFloat(truncating: h)
            Log("changing size, size: \(size)")
        }
        controller.overrideUserInterfaceStyle = .light
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        print(context)
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        DWebOS()
    }
}


class SizeModel: ObservableObject {
  @Published var size: CGSize = .zero
}
