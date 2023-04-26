# DWebView

```csharp
/// 创建，该对象直接继承于 WKWebView，所以可以直接作为 UIView 使用
var dwebview = DWebView.Create();
/// 同 Android 的 createWebMessageChannel 接口一样，创建出 WebMessageChannel，差别在于，Android 是同步函数，这里是异步函数。
var channel = await dwebview.CreateWebMessageChannel();
/// 事件监听，Signal<WebMessage>
channel.Port2.OnMessage += async (messageEvent, _) =>
{
    Debug.WriteLine("port2 on message: {0}", messageEvent.Data.ToString());
};
/// 执行 start，才能使得数据开始接收
await channel.Port2.Start();

_ = Task.Run(async () =>
{
    var i = 0;
    while (i++ < 3)
    {
        Debug.WriteLine("postMessage {0}", i);
        /// 发送消息，可以简写成 WebMessage.From("你好" + i)
        await channel.Port1.PostMessage(new WebMessage(new NSString("你好" + i)));
        await Task.Delay(500);
    }

    /// 和Android一样，可以执行 PostMessage，并传入 WebMessagePort 对象。相对应的，我们可以在 window.addEventMessage('message', event=>{}) 捕捉到这个消息
    await dwebview.PostMessage("你好", new WebMessagePort[] { channel.Port1 });
});
```
