namespace ipc;

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
        public static readonly Dictionary<object, IpcBody> Raw_ipcBody_WMap = new Dictionary<object, IpcBody>();

        /**
         * <summary>
		 * 每一个 metaBody 背后，都会有第一个 接收者IPC，这直接定义了它的应该由谁来接收这个数据，
		 * 其它的 IPC 即便拿到了这个 metaBody 也是没有意义的，除非它是 INLINE
		 * </summary>
		 */
        public static readonly Dictionary<string, Ipc> MetaId_receiverIpc_Map = new Dictionary<string, Ipc>();

        /**
         * <summary>
		 * 每一个 metaBody 背后，都会有一个 IpcBodySender,
		 * 这里主要是存储 流，因为它有明确的 open/close 生命周期
		 * </summary>
		 */
        public static readonly Dictionary<string, IpcBody> MetaId_ipcBodySender_Map = new Dictionary<string, IpcBody>();
	}

	protected internal class BodyHubType
	{
		public string? Text { get; set; } = null;
		public Stream? BodyStream { get; set; } = null;
		public byte[]? U8a { get; set; } = null;
		public object? Data { get; set; } = null;
	}

	protected abstract BodyHubType BodyHub { get; }

	public abstract SMetaBody MetaBody { get; set; }
    public abstract object? Raw { get; }

	private Lazy<byte[]> _u8a
    {
        get
        {
            return new Lazy<byte[]>(new Func<byte[]>(() =>
                BodyHub.U8a
                    ?? BodyHub.BodyStream?.Let(it =>
                    {
                        using (var memoryStream = new MemoryStream())
                        {
                            it.CopyTo(memoryStream);
                            return memoryStream.ToArray();
                        }
                    })
                    ?? BodyHub.Text?.Let(it => Convert.FromBase64String(it))
                    ?? throw new Exception("invalid body type").Also(it => CACHE.Raw_ipcBody_WMap.Add(it, this))));
        }
    }

	public byte[] U8a() => _u8a.Value;

	private Lazy<Stream> _stream
    {
        get
        {
            return new Lazy<Stream>(new Func<Stream>(() =>
                (BodyHub.BodyStream ?? _u8a.Let(it => new MemoryStream().Let(s =>
                {
                    s.Write(it.Value, 0, it.Value.Length);
                    return s;
                }))))).Also(it => CACHE.Raw_ipcBody_WMap.Add(it, this));
        }
    }

	public Stream BodyStream() => _stream.Value;

	private Lazy<string> _text
    {
        get
        {
            return new Lazy<string>(new Func<string>(() =>
                (BodyHub.Text ?? _u8a.Let(it => System.Text.UTF8Encoding.UTF8.GetString(it.Value)))))
                .Also(it => CACHE.Raw_ipcBody_WMap.Add(it, this));
        }
    }

	public string Text() => _text.Value;
}

