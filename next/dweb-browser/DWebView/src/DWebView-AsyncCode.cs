using DwebBrowser.Helper;
using WebKit;

namespace DwebBrowser.DWebView;

public partial class DWebView : WKWebView
{
    static private int asyncCodeIdAcc = 0;
    static private readonly Dictionary<int, PromiseOut<NSObject>> asyncTaskMap = new();
    static readonly string JS_ASYNC_KIT = "__native_async_callback_kit__";
    static readonly string asyncCodePrepareCode = $$"""
    {{JS_ASYNC_KIT}} = {
        resolve(id,res){
            webkit.messageHandlers.asyncCode.postMessage([1,id,res])
        },
        reject(id,err){
            console.error(err);
            webkit.messageHandlers.asyncCode.postMessage([0,id,"QQQQ:"+(err instanceof Error?(err.message+"\n"+err.stack):String(err))])
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

