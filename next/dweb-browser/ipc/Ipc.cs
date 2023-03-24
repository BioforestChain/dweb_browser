using System.Net.Http;

namespace ipc;

using Mmid = String;


public abstract class Ipc
{
    private static int _uid_acc = 1;
    private static int _req_id_acc = 0;

    public int Uid { get; set; } = Interlocked.Exchange(ref _uid_acc, Interlocked.Increment(ref _uid_acc));

    /**
     * <summary>
     * 是否支持 messagePack 协议传输：
     * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 MessagePack 的编解码
     * </summary>
     */
    public bool SupportMessagePack { get; set; } = false;

    /**
     * <summary>
     * 是否支持 Protobuf 协议传输：
     * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 Protobuf 的编解码
     * </summary>
     */
    public bool SupportProtobuf { get; set; } = false;

    /**
     * <summary>
     * 是否支持结构化内存协议传输：
     * 就是说不需要对数据手动序列化反序列化，可以直接传输内存对象
     * </summary>
     */
    public bool SupportRaw { get; set; } = false;

    /** <summary>是否支持 二进制 传输</summary> */
    public bool SupportBinary { get; set; } = false;

    public abstract MicroModuleInfo Remote { get; set; }

    public interface MicroModuleInfo
    {
        public Mmid mmid { get; set; }
    }

    // TODO: MicroModule还未实现
    //public MicroModule AsRemoteInstance()

    public abstract string Role { get; set; }

    public override string ToString() => $"#i{Uid}";

    public async Task PostMessageAsync(IpcMessage message)
    {

    }

    public async Task PostResponseAsync(int req_id, HttpResponseMessage response)
    {
        await PostMessageAsync(IpcResponse.FromResponse(req_id, response, this));
    }

    protected Signal<Func<IpcMessageArgs, object?>> _messageSigal = new Signal<Func<IpcMessageArgs, object?>>();

}

