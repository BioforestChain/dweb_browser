# CloseWatcher

参考 CloseWatcher 提案 https://github.com/WICG/close-watcher/blob/main/README.md
该提案的目的是以一种温和地方式为WebView提供 "返回" 手势的监听。

与现有Capacitor、Cordova直接将返回按钮完全拦截交给js不同，DWebview需要面向网络中未知的应用，我们必须避免拦截返回这样的行为滥用。
不同平台上对于返回有自己的定义：Android是我们熟知的导航返回按钮点击或者返回手势操作、IOS的VoiceOver的手势操作、桌面端的esc按钮、PlayStation浏览器的游戏手柄按钮按下等等，我们将它们统一定义成关闭信号。
在此信号基础上配合 UserActivation(1) 的机制，我们将它封装成 CloseWatcher API 来满足用户进行"返回"
操作的需求。

[//]: # (但目前，我们只实现 CloseWatcher 的 create 与 onClose，暂时不实现 onCancel。)

## 注解

1. UserActivation 参考资料: https://developer.mozilla.org/en-US/docs/Web/API/UserActivation。
  1.
   目前Android基本可用，IOS则是需要到16.2后才能使用。目前网络上暂无找到polyfill，因为需要自行实现，实现所需参考资料：https://html.spec.whatwg.org/multipage/interaction.html#tracking-user-activation
  2. 但是不论是Android和IOS，"消耗用户激活"
     这个接口是没有直接提供的，但是官方文档有给出一些其它能够消耗该状态的接口：https://developer.mozilla.org/en-US/docs/Web/Security/User_activation#sticky_activation
    1. 经过测试，window.open 可能是目前唯一可用的选择。因此我们需要监听各个平台的 open
       回调，来消耗 `navigator.userActivation.isActive`
