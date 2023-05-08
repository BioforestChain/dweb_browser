global using System;
global using System.Net;
global using System.Text.Json;
global using System.Text.Json.Serialization;
global using DwebBrowser.MicroService;
global using DwebBrowser.MicroService.Message;
global using DwebBrowser.MicroService.Http;
global using DwebBrowser.MicroService.Core;
global using DwebBrowser.MicroService.Sys.Http.Net;
global using DwebBrowser.Helper;
global using static DwebBrowser.Helper.Prelude;
global using MatchMode = System.String;

global using Mmid = System.String;

global using Router = System.Collections.Generic.Dictionary<
    string, System.Func<System.Collections.Generic.Dictionary<string, string>, object>>;

global using HttpHandler = System.Func<
    DwebBrowser.MicroService.Http.PureRequest,
    System.Threading.Tasks.Task<DwebBrowser.MicroService.Http.PureResponse>>;

global using RouterHandlerType = System.Func<
    DwebBrowser.MicroService.Http.PureRequest,
    DwebBrowser.MicroService.Ipc?,
    System.Threading.Tasks.Task<object?>>;