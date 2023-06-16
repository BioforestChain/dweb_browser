using DwebBrowser.Helper;
using WebKit;

namespace DwebBrowser.DWebView;

public static class MainThreadExtensions
{
    public static Task InvokeOnMainThreadAsync(this WKWebView webview, Func<Task> func)
    {
        var res = new PromiseOut<Unit>();
        webview.InvokeOnMainThread(() => InvokeTaskToPromiseOut(func, res));
        return res.WaitPromiseAsync();
    }

    public static Task<R> InvokeOnMainThreadAsync<R>(this WKWebView webview, Func<Task<R>> func) {
		var res = new PromiseOut<R>();
		webview.InvokeOnMainThread(() => InvokeTaskToPromiseOut(func,res));
		return res.WaitPromiseAsync();
	}

    static async void InvokeTaskToPromiseOut(Func<Task> func, PromiseOut<Unit> po)
    {
        await func();
        po.Resolve(Unit.Default);
    }
    static async void InvokeTaskToPromiseOut<R>(Func<Task<R>> func, PromiseOut<R> po)
    {
        po.Resolve(await func());
    }
}

