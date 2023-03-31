global using System;
global using System.Text.Json;
global using System.Text.Json.Serialization;
global using micro_service.ipc;
global using micro_service.ipc.ipcWeb;
global using micro_service.helper;
global using micro_service.extensions;
global using micro_service.core;

global using Mmid = System.String;

global using Router = System.Collections.Generic.Dictionary<
    string, System.Func<System.Collections.Generic.Dictionary<string, string>, object>>;