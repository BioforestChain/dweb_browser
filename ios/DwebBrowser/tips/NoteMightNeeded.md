#  生命周期

SwiftUI中还有其他一些生命周期修饰符，可以在视图的生命周期内执行某些操作。以下是一些常用的生命周期修饰符：
  
- `.onDisappear`：当视图即将从屏幕上消失时执行代码。通常用于清理视图状态或取消网络请求等操作。

- `.onChange`：当指定的绑定值发生更改时执行代码。通常用于响应用户输入或更新视图状态。

- `.onReceive`：当接收到指定类型的发布者时执行代码。通常用于响应异步操作或更新视图状态。

- `.onTapGesture`：当视图被点击时执行代码。通常用于响应用户输入或导航到其他视图。

- `.onLongPressGesture`：当视图被长按时执行代码。通常用于响应用户输入或显示上下文菜单等操作。

- `.onAppear(perform:)`：当视图出现时执行代码。与`.onAppear`不同，它允许您指定要执行的代码块。

- `.onDisappear(perform:)`：当视图消失时执行代码。与`.onDisappear`不同，它允许您指定要执行的代码块。

- `.onPreferenceChange(_:perform:)`：当指定类型的偏好值发生更改时执行代码。通常用于在视图层次结构中共享数据。

这些生命周期修饰符可以用于执行各种操作，例如更新视图状态、响应用户输入或执行异步操作等。您可以根据需要使用这些修饰符来扩展视图的功能。


# 动画的修改器

还有一些其他的动画效果，以下是一些常用的动画效果及其应用场景：
除了.scale和.opacity，SwiftUI中还有许多其他的动画效果可以使用，以下是一些常用的动画效果及其应用场景：

1. .rotationEffect：旋转视图。应用场景：用于旋转图像、文本或图形。

2. .offset：移动视图。应用场景：用于创建平移效果，例如将视图从屏幕左侧滑动到右侧。

3. .blur：模糊视图。应用场景：用于创建模糊效果，例如在显示敏感信息时。

4. .scaleEffect：缩放视图。应用场景：用于创建缩放效果，例如将视图从小变大或从大变小。

5. .animation：自定义动画。应用场景：用于在动画中添加自定义的过渡效果，例如在视图之间添加渐变效果。

6. .transition：视图过渡。应用场景：用于在视图之间添加过渡效果，例如在切换视图时添加淡入淡出效果。

7. .gesture：手势动画。应用场景：用于在用户与应用程序交互时添加动画效果，例如在用户拖动视图时添加平滑的移动效果。

8. .spring：弹簧效果。应用场景：用于创建弹性效果，例如在用户拖动视图时添加弹性动画。

9. .foregroundColor：前景色动画。应用场景：用于在文本或图形上添加颜色动画效果。

10. .background：背景色动画。应用场景：用于在视图背景上添加颜色动画效果。

11. .clipShape：裁剪形状动画。应用场景：用于在视图上添加裁剪形状动画效果。

12. .onAppear和.onDisappear：视图出现和消失动画。应用场景：用于在视图出现和消失时添加动画效果。

13. .scaleEffect和.scale都可以用于缩放视图，但它们的实现方式和效果略有不同。

.scaleEffect是一个Modifier，它可以将视图的宽度和高度同时缩放，可以通过传递一个CGFloat值来指定缩放比例。例如，.scaleEffect(0.5)将视图缩小到原来的一半大小。

.scale是一个ViewModifier，它可以将视图沿着X、Y和Z轴进行缩放。可以通过传递一个CGFloat值来指定每个轴的缩放比例。例如，.scale(x: 0.5, y: 0.5, z: 1)将视图沿着X和Y轴缩小到原来的一半大小，但不会影响Z轴。

因此，.scaleEffect更适合用于简单的视图缩放，而.scale更适合用于更复杂的3D缩放效果。

总之，SwiftUI提供了丰富的动画效果，可以根据不同的应用场景选择合适的动画效果来创建更加生动的用户界面。



# 坐标系

在SwiftUI中，.global是一个CoordinateSpace类型的枚举值，表示全局坐标系。在使用GeometryReader或者ScrollViewReader等视图时，我们可以使用该枚举值获取视图相对于全局坐标系的位置和大小。

在上面的代码中，scrollviewGeometry是一个GeometryProxy类型的对象，表示ScrollView的几何属性。调用.frame(in: .global)方法可以获取ScrollView相对于全局坐标系的位置和大小。

除了.global，还有其他的CoordinateSpace枚举值，如：

.local：表示本地坐标系，即视图自身的坐标系。

.named(name: String)：表示命名坐标系，可以自定义命名坐标系并在不同视图之间共享。

.parent：表示父级坐标系，即视图的直接父级坐标系。

.root：表示根坐标系，即整个视图层次结构的根坐标系。

我们可以根据需要选择不同的坐标系来获取视图的位置和大小。

# ScrollView 和 GeometryReader
在swiftui里，为什么ScrollView里面添加GeometryReader之后，拖拽就出此案问题了

在SwiftUI中，GeometryReader是一个非常有用的视图，可以用来获取其他视图的大小和位置信息。但是，在ScrollView中使用GeometryReader可能会导致拖拽问题，原因是GeometryReader会尝试获取ScrollView的大小和位置信息，并且ScrollView也会尝试获取GeometryReader的大小和位置信息，这样就会导致循环依赖和布局问题

要解决这个问题，可以考虑使用其他方法来获取视图的大小和位置信息，例如使用frame或者alignmentGuide。或者，可以将GeometryReader放在ScrollView外面，以便在ScrollView内部使用。另外，如果必须在ScrollView内部使用GeometryReader，可以尝试将其包装在一个ZStack中，以便在布局时不会与其他视图发生冲突。


# 滚动时会和statusbar重合的bug
                    Color.clear.frame(height: 0.1)  //如果没有这个 向上滚动的时候会和状态栏重合


# @State 的更新机制 
struct ReloadView: View {
    @State var reloadCount = 0
    var body: some View {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1, execute: {
            reloadCount += 1
    })
    return Text(" reload count \(reloadCount)")
    }
}
以上代码会循环执行，每次reloadCount改变，会刷新some View， 又回执行block更新reloadCount，如此循环

# @ObjectBinding
class MyModel: BindableObject {
    var didChange = PassthroughSubject<Void, Never>()

    var count = 0 {
        didSet {
            didChange.send()
        }
    }

    func increment() {
        count += 1
    }
}

struct MyView: View {
    @ObjectBinding var model: MyModel

    var body: some View {
        VStack {
            Text("Count: \(model.count)")
            Button(action: {
                self.model.increment()
            }) {
                Text("Increment")
            }
        }
    }
}

struct ContentView: View {
    @State var model = MyModel()

    var body: some View {
        MyView(model: model)
    }
}


#打印指针

print(Unmanaged.passUnretained(self.webViewStore).toOpaque())
