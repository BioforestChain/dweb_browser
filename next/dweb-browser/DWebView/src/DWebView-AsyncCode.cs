using System;
using DwebBrowser.Helper;
using WebKit;

namespace DwebBrowser.DWebView;

public partial class DWebView : WKWebView
{
    static private int asyncCodeIdAcc = 0;
    static private Dictionary<int, PromiseOut<NSObject>> asyncTaskMap = new();
    static string JS_ASYNC_KIT = "__native_async_callback_kit__";
    static string asyncCodePrepareCode = $$"""
    {{JS_ASYNC_KIT}} = {
        resolve(id,res){
            webkit.messageHandlers.asyncCode.postMessage([1,id,res])
        },
        reject(id,err){
            console.error(err);
            webkit.messageHandlers.asyncCode.postMessage([0,id,"QQQQ:"+(err instanceof Error?(err.stack||err.message):String(err))])
        }
    };
    void 0;
    """;
    //internal static readonly WKContentWorld asyncCodeContentWorld = WKContentWorld.DefaultClient; // WKContentWorld.Create("async-code");
    readonly AsyncCodeMessageHanlder asyncCodeMessageHanlder = new();

    internal class AsyncCodeMessageHanlder : WKScriptMessageHandler
    {
        [Export("userContentController:didReceiveScriptMessage:")]
        public override void DidReceiveScriptMessage(WKUserContentController userContentController, WKScriptMessage messageEvent)
        {
            var message = (NSArray)messageEvent.Body;

            var isSuccess = (bool)message.GetItem<NSNumber>(0);
            var id = (int)message.GetItem<NSNumber>(1);
            if (asyncTaskMap.Remove(id, out var asyncTask))
            {
                if (isSuccess)
                {
                    asyncTask.Resolve(message.GetItem<NSObject>(2));
                }
                else
                {
                    asyncTask.Reject((string)message.GetItem<NSString>(2));
                }
            }
        }

    }
    public async Task<NSObject> EvaluateAsyncJavascriptCode(string script, Func<Task>? afterEval = default)
    {
        /// 页面可能会被刷新，所以需要重新判断：函数可不可用
        var asyncCodeInited = (bool)(NSNumber)await base.EvaluateJavaScriptAsync("typeof " + JS_ASYNC_KIT + "==='object'");
        if (!asyncCodeInited)
        {
            await base.EvaluateJavaScriptAsync(new NSString(asyncCodePrepareCode));
            base.Configuration.UserContentController.AddScriptMessageHandler(asyncCodeMessageHanlder, "asyncCode");
        }
        var id = Interlocked.Increment(ref asyncCodeIdAcc);
        var asyncTask = new PromiseOut<NSObject>();
        asyncTaskMap.Add(id, asyncTask);
        var wrapCode = $$"""
            void (async()=>{return ({{script}})})()
                .then(res=>{{JS_ASYNC_KIT}}.resolve({{id}},res))
                .catch(err=>{{JS_ASYNC_KIT}}.reject({{id}},err));
            """;
        await base.EvaluateJavaScriptAsync(wrapCode);

        if (afterEval is not null)
        {
            await afterEval();
        }

        return await asyncTask.WaitPromiseAsync();
    }
}

