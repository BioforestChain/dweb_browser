﻿using System.Text.Json;
using System.Text.Json.Serialization;
using DwebBrowser.DWebView;
using DwebBrowser.IpcWeb;
using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Message;
using Foundation;

namespace DwebBrowser.WebModule.Js;

public class JsProcessWebApi
{
    public DWebView.DWebView DWebView { get; init; }

    public JsProcessWebApi(DWebView.DWebView dWebView)
    {
        DWebView = dWebView;
    }


    public class ProcessInfo
    {
        [JsonPropertyName("process_id")]
        public int ProcessId { get; set; }
        public ProcessInfo(int process_id)
        {
            ProcessId = process_id;
        }
    }

    public record ProcessHandler(ProcessInfo Info, MessagePortIpc Ipc);

    public async Task<ProcessHandler> CreateProcess(
        string env_script_url,
        string metadata_json,
        string env_json,
        Ipc.MicroModuleInfo remoteModule,
        string host)
    {
        var channel = await this.DWebView.CreateWebMessageChannelC();
        var port1 = channel.Port1;
        var port2 = channel.Port2;
        var metadata_json_str = JsonSerializer.Serialize(metadata_json);
        var env_json_str = JsonSerializer.Serialize(env_json);

        var processInfo_json = await this.DWebView.EvaluateJavaScriptAsync(@"""
           new Promise((resolve,reject)=>{
                addEventListener(""message"", async event => {
                    if (event.data === ""js-process/create-process"") {
                        const fetch_port = event.ports[0];
                        try{
                            resolve(await createProcess(`$env_script_url`,$metadata_json_str,$env_json_str,fetch_port,`$host`))
                        }catch(err){
                            reject(err)
                        }
                    }
                }, { once: true })
            })
        """.Trim());

        try
        {
            await this.DWebView.PostMessage("js-process/create-process", new WebMessagePort[] { port1 });
        }
        catch (Exception e)
        {
            Console.WriteLine(e.StackTrace);
        }
        Console.WriteLine($"processInfo {processInfo_json}");

        try
        {
            if (processInfo_json is NSData data)
            {
                var info = JsonSerializer.Deserialize<ProcessInfo>(data.AsStream());
                return new ProcessHandler(info, new MessagePortIpc(port2, remoteModule, IPC_ROLE.CLIENT));
            }

            throw new Exception("processInfo_json 类型错误，无法进行序列化");
        }
        catch(Exception err)
        {
            throw new Exception($"CreateProcess JsonDeserialize ProcessInfo error: {err.Message}");
        }
    }

    public record RunProcessMainOptions(string MainUrl);

    public Task RunProcessMain(int process_id, RunProcessMainOptions options) =>
        this.DWebView.EvaluateJavaScriptAsync($"runProcessMain({process_id}, {{main_url:`{options.MainUrl}`}})".Trim());

    public Task DestroyProcess(int process_id) =>
        this.DWebView.EvaluateJavaScriptAsync($"destroy({process_id})".Trim());

    public async Task<int> CreateIpc(int process_id, Mmid mmid)
    {
        var channel = await this.DWebView.CreateWebMessageChannelC();
        var port1 = channel.Port1;
        var port2 = channel.Port2;
        await this.DWebView.EvaluateJavaScriptAsync($$"""
        new Promise((resolve,reject)=>{
            addEventListener("message", async event => {
                if (event.data === "js-process/create-ipc") {
                    const ipc_port = event.ports[0];
                    try{
                        resolve(await createIpc($process_id, `$mmid`, ipc_port))
                    }catch(err){
                        reject(err)
                    }
                }
            }, { once: true })
        })
        """.Trim());

        await this.DWebView.PostMessage("js-process/create-pic", new WebMessagePort[] { port1 });

        return IpcWebMessageCache.SaveNative2JsIpcPort(port2);
    }

    public void Destroy()
    { }
}

