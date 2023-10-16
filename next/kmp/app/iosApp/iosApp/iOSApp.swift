import SwiftUI
import WebRTC

@main
struct iOSApp: App {
	var body: some Scene {
		WindowGroup {
            Text("xxx")
			ContentView()
		}
	}
    
    init(){
        
        let url = URL(string: "http://know.webhek.com/wp-content/uploads/svg/Ghostscript_Tiger.svg")
        let request = URLRequest(url: url!)
        URLSession.shared.dataTask(with: request) { data, response, error in
              print("data?.count: \(data?.count)")
        }.resume()
        
    }
}
