import SwiftUI
import UIKit

struct ComposeScreen: UIViewControllerRepresentable {
    let make: () -> UIViewController

    func makeUIViewController(context: Context) -> UIViewController {
        make()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
