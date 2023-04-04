global using System;
global using System.Text.Json;
global using System.Text.Json.Serialization;
global using DwebBrowser.MicroService;
global using DwebBrowser.MicroService.Message;
global using DwebBrowser.Helper;
global using DwebBrowser.MicroService.Core;

global using Mmid = System.String;

global using Router = System.Collections.Generic.Dictionary<
    string, System.Func<System.Collections.Generic.Dictionary<string, string>, object>>;