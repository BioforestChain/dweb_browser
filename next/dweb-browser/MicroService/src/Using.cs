global using System;
global using System.Net;
global using System.Text.Json;
global using System.Text.Json.Serialization;
global using DwebBrowser.Helper;
global using DwebBrowser.MicroService.Core;
global using DwebBrowser.MicroService.Http;
global using DwebBrowser.MicroService.Message;
global using DwebBrowser.MicroService.Sys.Http.Net;
global using static DwebBrowser.Helper.Prelude;
global using Router = System.Collections.Generic.Dictionary<
    string, System.Func<System.Collections.Generic.Dictionary<string, string>, object>>;
global using HttpHandler = System.Func<
    DwebBrowser.MicroService.Http.PureRequest,
    System.Threading.Tasks.Task<DwebBrowser.MicroService.Http.PureResponse>>;
global using RouterHandlerType = System.Func<
    DwebBrowser.MicroService.Http.PureRequest,
    DwebBrowser.MicroService.Ipc?,
    System.Threading.Tasks.Task<object?>>;
global using WebSocketHandler = System.Func<
    DwebBrowser.MicroService.Http.PureRequest,
    System.Net.WebSockets.HttpListenerWebSocketContext,
    System.Threading.Tasks.Task>;
global using MatchMode = System.String;
global using Mmid = System.String;
global using Dweb_DeepLink = System.String;
