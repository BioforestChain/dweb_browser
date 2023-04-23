using System.Runtime.CompilerServices;

namespace DwebBrowser.MicroService.Message;

public abstract class IpcBody
{
    public class CACHE
    {
        /**
         * <summary>
         * 任意的 RAW 背后都会有一个 IpcBodySender/IpcBodyReceiver
         * 将它们缓存起来，那么使用这些 RAW 确保只拿到同一个 IpcBody，这对 RAW-Stream 很重要，流不可以被多次打开读取
         * </summary>
         */
        public static readonly ConditionalWeakTable<object, IpcBody> Raw_ipcBody_WMap = new();

        /**
         * <summary>
		 * 每一个 metaBody 背后，都会有第一个 接收者IPC，这直接定义了它的应该由谁来接收这个数据，
		 * 其它的 IPC 即便拿到了这个 metaBody 也是没有意义的，除非它是 INLINE
		 * </summary>
		 */
        public static readonly Dictionary<string, Ipc> MetaId_receiverIpc_Map = new();

        /**
         * <summary>
		 * 每一个 metaBody 背后，都会有一个 IpcBodySender,
		 * 这里主要是存储 流，因为它有明确的 open/close 生命周期
		 * </summary>
		 */
        public static readonly Dictionary<string, IpcBody> MetaId_ipcBodySender_Map = new();
    }

    protected internal class BodyHubType
    {
        public string? Text { get; set; } = null;
        public Stream? Stream { get; set; } = null;
        public byte[]? U8a { get; set; } = null;
        public object? Data { get; set; } = null;
    }

    protected abstract BodyHubType BodyHub { get; }

    public abstract SMetaBody MetaBody { get; set; }
    public abstract object? Raw { get; }

    private Lazy<byte[]> _u8a;
    public byte[] U8a { get { return _u8a.Value; } }
    private static byte[] InitU8A(IpcBody ipcBody)
    {
        var BodyHub = ipcBody.BodyHub;
        var u8a = BodyHub.U8a
        ?? BodyHub.Stream?.ToByteArray()
        ?? BodyHub.Text?.FromBase64()
        ?? throw new Exception("invalid body type");

        CACHE.Raw_ipcBody_WMap.TryAdd(u8a, ipcBody);
        return u8a;
    }


    private Lazy<Stream> _stream;
    public Stream Stream { get { return _stream.Value; } }
    private static Stream InitStream(IpcBody ipcBody)
    {
        var BodyHub = ipcBody.BodyHub;
        var stream = BodyHub.Stream ?? new MemoryStream(ipcBody.U8a);
        CACHE.Raw_ipcBody_WMap.TryAdd(stream, ipcBody);
        return stream;
    }

    private Lazy<string> _text;
    public string Text { get { return _text.Value; } }
    private static string InitText(IpcBody ipcBody)
    {
        var BodyHub = ipcBody.BodyHub;
        var text = BodyHub.Text ?? ipcBody.U8a.ToUtf8();
        CACHE.Raw_ipcBody_WMap.TryAdd(text, ipcBody);
        return text;
    }

    public IpcBody()
    {
        _u8a = new Lazy<byte[]>(() => InitU8A(this), true);
        _stream = new Lazy<Stream>(() => InitStream(this), true);
        _text = new Lazy<string>(() => InitText(this), true);
    }
}

