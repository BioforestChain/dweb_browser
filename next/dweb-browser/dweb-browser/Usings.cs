global using System;
global using ipc;
global using ipc.helper;
global using ipc.extensions;
global using ipc.ipcWeb;

global using Mmid = System.String;

global using Router = System.Collections.Generic.Dictionary<
    string, System.Func<System.Collections.Generic.Dictionary<string, string>, object>>;
