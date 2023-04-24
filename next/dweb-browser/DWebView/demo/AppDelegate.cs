using System.Net;
using System.Net.Http.Headers;
using DwebBrowser.DWebView;
using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Message;
using DwebBrowser.MicroService.Sys.Http;
using DwebBrowser.MicroService.Sys.Http.Net;

namespace demo;

[Register("AppDelegate")]
public class AppDelegate : UIApplicationDelegate
{
    public override UIWindow? Window
    {
        get;
        set;
    }

    class TestNMM : NativeMicroModule
    {
        public TestNMM() : base("test.sys.dweb")
        {
        }

        protected override Task _bootstrapAsync(IBootstrapContext bootstrapContext)
        {
            throw new NotImplementedException();
        }

        protected override Task _onActivityAsync(IpcEvent Event, Ipc ipc)
        {
            throw new NotImplementedException();
        }

        protected override Task _shutdownAsync()
        {
            throw new NotImplementedException();
        }
    }

    public override bool FinishedLaunching(UIApplication application, NSDictionary launchOptions)
    {
        // create a new window instance based on the screen size
        Window = new UIWindow(UIScreen.MainScreen.Bounds);

        // create a UIViewController with a single UILabel
        var vc = new UIViewController();
        var localeNmm = new TestNMM();
        Func<HttpRequestMessage, Task<HttpResponseMessage>> httpHanlder = async (request) =>
        {
            if (request.RequestUri?.OriginalString is "localhost:20222")
            {
                if (request.RequestUri.AbsolutePath.StartsWith(HttpNMM.X_DWEB_HREF))
                {
                    request.RequestUri = new Uri(request.RequestUri.AbsolutePath.Substring(HttpNMM.X_DWEB_HREF.Length));
                }
            }
            if (request.RequestUri?.AbsolutePath is "/index.html")
            {
                var response = new HttpResponseMessage(HttpStatusCode.OK);
                response.Content = new StringContent($$"""
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <h1>你好!!</h1>
                    <img src='./hi.png'/>
                    <script>
                    var a = 1;
                    addEventListener('message',(event)=>{
                        const ele = document.createElement("h1");
                        ele.style.color = 'red';
                        ele.innerHTML = [event.data,...event.ports].join(" ");
                        document.body.insertBefore(ele, document.body.firstChild);
                    });
                    </script>
                    """, new MediaTypeHeaderValue("text/html", "utf-8"));
                return response;
            }
            else if (request.RequestUri?.AbsolutePath is "/hi.png")
            {
                var response = new HttpResponseMessage(HttpStatusCode.OK);
                response.Content = new StringContent("data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzMzIiBoZWlnaHQ9IjIxMCIgdmlld0JveD0iMCAwIDMzMyAyMTAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxwYXRoIGQ9Ik05MC45NDI5IDMxLjA3NzlWOTYuMjk5NSIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiIHN0cm9rZS1kYXNoYXJyYXk9IjQgNiIvPgo8cGF0aCBkPSJNMjA2LjgwNiAxNzEuMjY2TDE4Ni43MzMgMTg4Ljg1OEwyNy42MzI2IDkzLjM4OTFMNDcuNjk4MyA3NS44MDRIMjA2LjgwNlYxNzEuMjY2WiIgZmlsbD0id2hpdGUiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0xODYuNzMzIDkzLjM4OTJIMjcuNjMyNlYxODguODUxSDE4Ni43MzNWOTMuMzg5MloiIGZpbGw9IndoaXRlIiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIvPgo8cGF0aCBkPSJNMTA3LjE4NSAxNjIuOTA2QzEyMC45MSAxNjIuOTA2IDEzMi4wMzYgMTUzLjE1NSAxMzIuMDM2IDE0MS4xMjdDMTMyLjAzNiAxMjkuMDk5IDEyMC45MSAxMTkuMzQ4IDEwNy4xODUgMTE5LjM0OEM5My40NjA2IDExOS4zNDggODIuMzM0NSAxMjkuMDk5IDgyLjMzNDUgMTQxLjEyN0M4Mi4zMzQ1IDE1My4xNTUgOTMuNDYwNiAxNjIuOTA2IDEwNy4xODUgMTYyLjkwNloiIGZpbGw9IiM3QUY3QzIiLz4KPHBhdGggZD0iTTExOS41MjIgMTM0LjIwNUwxMDMuNzQxIDE0OC4wNDJMOTUuODUwMyAxNDEuMTI3IiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS13aWR0aD0iNSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0xODYuNzMzIDkzLjM4OTFMMjA2LjgwNyA3NS44MDQiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0yMjYuODczIDc1LjgwNEwyNDYuOTQ2IDU4LjIxMjJMMjA2LjgwNyA3NS44MDRMMTg2LjczMyA5My4zODkyTDIyNi44NzMgNzUuODA0WiIgZmlsbD0id2hpdGUiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBzdHJva2UtbGluZWpvaW49InJvdW5kIi8+CjxwYXRoIGQ9Ik0yOTEuNDk5IDE5MS4zODRMMjcxLjQzMyAyMDguOTY5TDE1Mi40NjQgMTM5Ljg4NEwxNzIuNTMgMTIyLjI5OUgyOTEuNDk5VjE5MS4zODRaIiBmaWxsPSJ3aGl0ZSIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTI3MS40MzMgMTM5Ljg4NEgxNTIuNDY0VjIwOC45NjlIMjcxLjQzM1YxMzkuODg0WiIgZmlsbD0id2hpdGUiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0yMTQuMDg3IDE5MC40MjVDMjI0LjE2OSAxOTAuNDI1IDIzMi4zNDIgMTgzLjI2MiAyMzIuMzQyIDE3NC40MjZDMjMyLjM0MiAxNjUuNTkxIDIyNC4xNjkgMTU4LjQyOCAyMTQuMDg3IDE1OC40MjhDMjA0LjAwNSAxNTguNDI4IDE5NS44MzIgMTY1LjU5MSAxOTUuODMyIDE3NC40MjZDMTk1LjgzMiAxODMuMjYyIDIwNC4wMDUgMTkwLjQyNSAyMTQuMDg3IDE5MC40MjVaIiBmaWxsPSIjMDBDMUE0Ii8+CjxwYXRoIGQ9Ik0yMjMuMTU3IDE2OS4zNDhMMjExLjU2IDE3OS41MTJMMjA1Ljc2NSAxNzQuNDI2IiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS13aWR0aD0iNCIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0zMTEuNTY1IDEyMi4yOTlMMzMxLjYzOCAxMDQuNzA3TDI5MS40OTkgMTIyLjI5OUwyNzEuNDMzIDEzOS44ODRMMzExLjU2NSAxMjIuMjk5WiIgZmlsbD0id2hpdGUiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBzdHJva2UtbGluZWpvaW49InJvdW5kIi8+CjxwYXRoIGQ9Ik0xMjUuODMzIDEyMi4yOTlMMTQ1Ljg5OSAxMDQuNzA3TDE3Mi41MyAxMjIuMjk5TDE1Mi40NjUgMTM5Ljg4NEwxMjUuODMzIDEyMi4yOTlaIiBmaWxsPSJ3aGl0ZSIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiLz4KPHBhdGggZD0iTTI3LjYzMjMgOTMuMzg5M0w0Ny42OTgxIDc1LjgwNDJIMTczLjg1NlY5My4zODkzSDI3LjYzMjNaIiBmaWxsPSIjMjEyMTIxIi8+CjxwYXRoIGQ9Ik0xMDMuMjcxIDkyLjUxMTRWNTIuMDczN0g1Ny4xMjkzVjkyLjUxMTRIMTAzLjI3MVoiIGZpbGw9IiNGRkExMDAiLz4KPHBhdGggZD0iTTE2MS4yODkgNTQuMjA5M0wxNzMuODU5IDQzLjE5MjlMMTYxLjI4OSAzMi4xNzY1TDE0OC43MTkgNDMuMTkyOUwxNjEuMjg5IDU0LjIwOTNaIiBmaWxsPSIjMjEyMTIxIiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIvPgo8cGF0aCBkPSJNOTQuMjgzMyAyOS43NzU2TDk0LjI4ODggMjkuNzcwOUM5Ni4zNzcyIDI3Ljk0MDYgOTYuMzc3MyAyNC45NzMxIDk0LjI4ODkgMjMuMTQyOEw5NC4yODM0IDIzLjEzOEM5Mi4xOTUgMjEuMzA3OCA4OC44MDg4IDIxLjMwNzkgODYuNzIwNCAyMy4xMzgxTDg2LjcxNDkgMjMuMTQyOUM4NC42MjY1IDI0Ljk3MzIgODQuNjI2NSAyNy45NDA2IDg2LjcxNSAyOS43NzA4TDg2LjcyMDQgMjkuNzc1NkM4OC44MDg5IDMxLjYwNTkgOTIuMTk0OSAzMS42MDU5IDk0LjI4MzMgMjkuNzc1NloiIGZpbGw9IiMyMTIxMjEiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0xMjYuMTU4IDkyLjUxMTJDMTI2LjE1OCA4Ni45MzAzIDEyOC42ODggODEuNTc4IDEzMy4xOTEgNzcuNjMxOEMxMzcuNjk0IDczLjY4NTUgMTQzLjgwMSA3MS40Njg1IDE1MC4xNjkgNzEuNDY4NUMxNTYuNTM4IDcxLjQ2ODUgMTYyLjY0NSA3My42ODU1IDE2Ny4xNDggNzcuNjMxOEMxNzEuNjUxIDgxLjU3OCAxNzQuMTggODYuOTMwMyAxNzQuMTggOTIuNTExMiIgZmlsbD0iI0ZGNzZDNCIvPgo8cGF0aCBkPSJNMjgwLjIxOCAxMzEuOTdIMTk5Ljc0N1Y1OC4xMjQzQzE5OS43NDcgNTUuNzk1OSAyMDAuODAyIDUzLjU2MjkgMjAyLjY4MSA1MS45MTY1QzIwNC41NTkgNTAuMjcwMiAyMDcuMTA3IDQ5LjM0NTIgMjA5Ljc2NCA0OS4zNDUySDI3MC4xNzdDMjcyLjgzNCA0OS4zNDUyIDI3NS4zODIgNTAuMjcwMiAyNzcuMjYgNTEuOTE2NUMyNzkuMTM5IDUzLjU2MjkgMjgwLjE5NCA1NS43OTU5IDI4MC4xOTQgNTguMTI0M0wyODAuMjE4IDEzMS45N1oiIGZpbGw9IndoaXRlIi8+CjxwYXRoIGQ9Ik0yNzEuMDE5IDE0MC4yMzRWMzIuMjIxMiIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTI0NS4wNiAxNDAuMjM1VjguNTM0MTgiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0yMTguMDIgMTQwLjIzNVY4LjUzNDE4IiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIvPgo8cGF0aCBkPSJNMjgwLjc1NCAxMjYuMDIySDE3My42NzQiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0yODAuNzUzIDEwMi4zMzVIMTc2LjkxOCIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTI4MC43NTMgNzguNjQ3OUgxNjguMjY2IiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIvPgo8cGF0aCBkPSJNMjc5LjY3MiA1NS45MDg0SDE5OS42MzIiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0yODAuMjE3IDMxLjk4MjlIMjAwLjU1NiIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTI1OC4xIDEwNS45ODdMMjU4LjEwNSAxMDUuOTgyQzI2MC4xOTMgMTA0LjE1MiAyNjAuMTkzIDEwMS4xODUgMjU4LjEwNSA5OS4zNTQyTDI1OC4xIDk5LjM0OTVDMjU2LjAxMSA5Ny41MTkyIDI1Mi42MjUgOTcuNTE5MiAyNTAuNTM3IDk5LjM0OTRMMjUwLjUzMSA5OS4zNTQyQzI0OC40NDMgMTAxLjE4NCAyNDguNDQzIDEwNC4xNTIgMjUwLjUzMSAxMDUuOTgyTDI1MC41MzcgMTA1Ljk4N0MyNTIuNjI1IDEwNy44MTcgMjU2LjAxMSAxMDcuODE3IDI1OC4xIDEwNS45ODdaIiBmaWxsPSIjMjEyMTIxIiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIvPgo8cGF0aCBkPSJNMjgwLjIxOSAxMzEuOTdMMjcxLjQyNyAxMzkuODg0SDE5OS43NjNWNTguMTI0M0MxOTkuNzYzIDU1Ljc5NTkgMjAwLjgxOSA1My41NjI5IDIwMi42OTcgNTEuOTE2NUMyMDQuNTc2IDUwLjI3MDIgMjA3LjEyNCA0OS4zNDUyIDIwOS43ODEgNDkuMzQ1MkgyNzAuMTk0QzI3Mi44NTEgNDkuMzQ1MiAyNzUuMzk5IDUwLjI3MDIgMjc3LjI3NyA1MS45MTY1QzI3OS4xNTYgNTMuNTYyOSAyODAuMjExIDU1Ljc5NTkgMjgwLjIxMSA1OC4xMjQzTDI4MC4yMTkgMTMxLjk3WiIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTI1NC4zMiAxMzkuODc4SDE3My44NDlWNzIuNDc0NkMxNzMuODQ5IDcwLjE0NjMgMTc0LjkwNSA2Ny45MTMzIDE3Ni43ODMgNjYuMjY2OUMxNzguNjYyIDY0LjYyMDUgMTgxLjIxIDYzLjY5NTYgMTgzLjg2NyA2My42OTU2SDI0NC4yODdDMjQ2Ljk0NCA2My42OTU2IDI0OS40OTIgNjQuNjIwNSAyNTEuMzcxIDY2LjI2NjlDMjUzLjI0OSA2Ny45MTMzIDI1NC4zMDUgNzAuMTQ2MyAyNTQuMzA1IDcyLjQ3NDZMMjU0LjMyIDEzOS44NzhaIiBmaWxsPSJ3aGl0ZSIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTE4Ny4xMDMgODQuOTM0MUgyMjQuNDQ1IiBzdHJva2U9IiMyMTIxMjEiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIvPgo8cGF0aCBkPSJNMTg3LjEwMyAxMDQuMjg5SDIzNy4yNTIiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0xODcuMTAzIDk0LjYxMTNIMjEzLjM3MiIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiLz4KPHBhdGggZD0iTTIzNy4yNDQgMTEyLjAxNEgxODcuMTAzVjEyNi4wMTNIMjM3LjI0NFYxMTIuMDE0WiIgZmlsbD0iIzAwODFGRiIvPgo8cGF0aCBkPSJNMSA3NS44MDRMMjEuMDY1NyA1OC4yMTIyTDQ3LjY5NjggNzUuODA0TDI3LjYzMSA5My4zODkyTDEgNzUuODA0WiIgZmlsbD0id2hpdGUiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBzdHJva2UtbGluZWpvaW49InJvdW5kIi8+CjxwYXRoIGQ9Ik0xMTkuNjg0IDEwLjU4MjNWNzUuMTU1NiIgc3Ryb2tlPSIjMjEyMTIxIiBzdHJva2UtbWl0ZXJsaW1pdD0iMTAiIHN0cm9rZS1kYXNoYXJyYXk9IjQgNiIvPgo8cGF0aCBkPSJNMTE5LjY4NCA5Mi41MTExVjc1LjE1NTUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS1taXRlcmxpbWl0PSIxMCIgc3Ryb2tlLWRhc2hhcnJheT0iNCA2Ii8+CjxwYXRoIGQ9Ik0xNTIuNDY0IDEzOS44ODRMMTcyLjUzIDEyMi4yOTlIMTczLjg1NVYxMzkuODg0SDE1Mi40NjRaIiBmaWxsPSIjMjEyMTIxIi8+CjxwYXRoIGQ9Ik0yODAuMjE5IDEyMi4yOTlWMTMxLjk3TDI5MS41IDEyMi4yOTlIMjgwLjIxOVoiIGZpbGw9IiMyMTIxMjEiLz4KPHBhdGggZD0iTTExMy4xOTYgMTEuMzY1NUwxMjYuMTY1IDExLjM2NTVWLTMuMDUxNzZlLTA1TDExMy4xOTYgLTMuMDUxNzZlLTA1VjExLjM2NTVaIiBmaWxsPSIjMDBDQUZGIi8+CjxwYXRoIGQ9Ik0xOTkuODYzIDEyNi4wMTNDMTk5Ljg2MyAxMjYuMDEzIDIwMy4yNzcgMTE5LjAxIDIxNC45ODIgMTE5LjAxQzIyNi42ODcgMTE5LjAxIDIyNi44OTUgMTEyLjAxNCAyMjYuODk1IDExMi4wMTQiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+CjxwYXRoIGQ9Ik0yMTguMTI2IDU4Ljk0MTRDMjIwLjE1MiA1OC45NDE0IDIyMS43OTQgNTcuNTAyMyAyMjEuNzk0IDU1LjcyNjlDMjIxLjc5NCA1My45NTE2IDIyMC4xNTIgNTIuNTEyNSAyMTguMTI2IDUyLjUxMjVDMjE2LjEgNTIuNTEyNSAyMTQuNDU4IDUzLjk1MTYgMjE0LjQ1OCA1NS43MjY5QzIxNC40NTggNTcuNTAyMyAyMTYuMSA1OC45NDE0IDIxOC4xMjYgNTguOTQxNFoiIGZpbGw9IiNGRjAwNzYiIHN0cm9rZT0iIzIxMjEyMSIgc3Ryb2tlLW1pdGVybGltaXQ9IjEwIi8+Cjwvc3ZnPgo=");
                return response;
            }

            return new HttpResponseMessage(HttpStatusCode.NotFound);

        };
        NativeFetch.NativeFetchAdaptersManager.Append(async (mm, request) =>
        {
            if (request.RequestUri?.Host is "test.sys.dweb")
            {
                return await httpHanlder(request);
            }
            return null;
        });
        NetServer.HttpCreateServer(new ListenOptions(20222), httpHanlder);
        var dwebview = new DWebView(vc.View?.Frame, localeNmm, localeNmm, new DWebView.Options("https://test.sys.dweb/index.html"), null);
        vc.View!.AddSubview(dwebview);

        var btn = new UIButton();
        btn.SetTitle("创建Channel", UIControlState.Normal);
        btn.SetTitleColor(UIColor.Blue, UIControlState.Normal);
        btn.Frame = new CGRect(100, 100, 100, 30);
        btn.AddTarget(new EventHandler(async (sender, e) =>
        {
            var channel = await dwebview.CreateWebMessageChannelC();

            channel.Port2.OnMessage += async (messageEvent, _) =>
            {
                Console.WriteLine("port2 on message: {0}", messageEvent.Data.ToString());
            };
            _ = Task.Run(async () =>
            {
                var i = 0;
                while (i++ < 5)
                {
                    Console.WriteLine("postMessage {0}", i);
                    await channel.Port1.PostMessage(new WebMessage(new NSString("你好" + i)));
                    await Task.Delay(100);
                    if (i >= 3)
                    {
                        await channel.Port2.Start();
                    }
                }
                await dwebview.PostMessage("你好", new WebMessagePort[] { channel.Port1 });
            });
        })
        , UIControlEvent.TouchUpInside);

        vc.View.AddSubview(btn);

        Window.RootViewController = vc;

        // make the window visible
        Window.MakeKeyAndVisible();

        return true;
    }
}

