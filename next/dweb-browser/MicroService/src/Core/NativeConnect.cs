
namespace DwebBrowser.MicroService.Core;

/**
 * <summary>
 * 两个模块的连接结果：
 *
 * 1. fromIpc 是肯定有的，这个对象是我们当前的上下文发起连接得来的通道，要与 toMM 通讯都需要通过它
 * 1. toIpc 则不一定，远程模块可能是自己创建了 Ipc，我们的上下文拿不到这个内存对象
 * </summary>
 */

using ConnectAdapter = Func<MicroModule, MicroModule, HttpRequestMessage, Task<ConnectResult?>>;
public record ConnectResult(Ipc IpcForFromMM, Ipc? IpcForToMM);


public static class NativeConnect
{
    public static readonly AdapterManager<ConnectAdapter> ConnectAdapterManager = new();

    public static async Task<ConnectResult> ConnectMicroModulesAsync(MicroModule fromMM, MicroModule toMM, HttpRequestMessage reason)
    {
        foreach (ConnectAdapter connectAdapter in ConnectAdapterManager.Adapters)
        {
            var connectResult = await connectAdapter(fromMM, toMM, reason);

            if (connectResult is not null)
            {
                return connectResult;
            }
        }

        throw new Exception($"no support connect MicroModules, from:{fromMM.Mmid} to:{toMM.Mmid}");
    }
}

