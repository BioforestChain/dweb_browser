
namespace ipc;

/**
 * <summary>
 * IpcBodySender 本质上是对 ReadableStream 的再次封装。
 * 我们知道 ReadableStream 本质上是由 stream 与 controller 组成。二者分别代表着 reader 与 writer 两个角色。
 *
 * 而 IpcBodySender 则是将 controller 给一个 ipc 来做写入，将 stream 给另一个 ipc 来做接收。
 * 而关键点就在于这两个 ipc 很可能不是对等关系
 *
 * 因为 IpcBodySender 会被 IpcRequest/http4kRequest、IpcResponse/http4kResponse 对象转换的时候传递，
 * 中间被很多个 ipc 所持有过，而每一个持有过它的人都有可能是这个 stream 的读取者。
 *
 * 因此我们定义了两个集合，一个是 ipc 的 usableIpcBodyMap；一个是 ipcBodySender 这边的 usedIpcMap
 * </summary>
 */

public class IpcBodySender : IpcBody
{
    public override object? Raw { get; }
    public Ipc SenderIpc { get; set; }

    private Lazy<bool> _isStream { get; set; }
    public bool IsStream
    {
        get { return _isStream.Value; }
    }

    public bool IsStreamClosed
    {
        get { return IsStream ? _isStreamClosed : true; }
    }

    private bool _isStreamOpenedValue = false;
    private bool _isStreamOpened
    {
        get { return _isStreamOpenedValue; }
        set
        {
            if (_isStreamOpenedValue != value)
            {
                _isStreamOpenedValue = value;
            }
        }
    }

    private bool _isStreamClosedValue = false;
    private bool _isStreamClosed
    {
        get { return _isStreamClosedValue; }
        set
        {
            if (_isStreamClosedValue != value)
            {
                _isStreamClosedValue = value;
            }
        }
    }

    private Lazy<BodyHubType> _bodyHub { get; set; }
    protected override BodyHubType BodyHub
    {
        get { return _bodyHub.Value; }
    }
    public override SMetaBody MetaBody { get; set; }

    public IpcBodySender(object raw, Ipc ipc): base()
    {
        CACHE.Raw_ipcBody_WMap.Add(raw, this);


        Raw = raw;
        SenderIpc = ipc;

        _isStream = new Lazy<bool>(new Func<bool>(() => raw is Stream));

        MetaBody = BodyAsMeta(Raw, SenderIpc);

        _bodyHub = new Lazy<BodyHubType>(new Func<BodyHubType>(() => new BodyHubType().Also(it =>
            {
                it.Data = raw;
                switch (raw)
                {
                    case string value:
                        it.Text = value;
                        break;
                    case byte[] value:
                        it.U8a = value;
                        break;
                    case Stream value:
                        it.BodyStream = value;
                        break;
                }
            })));
    }


    public static IpcBodySender From(object raw, Ipc ipc) => new IpcBodySender(raw, ipc);

    private SMetaBody BodyAsMeta(object body, Ipc ipc)
    {
        switch (body)
        {
            case string value:
                return SMetaBody.FromText(ipc.Uid, value);
            case byte[] value:
                return SMetaBody.FromBinary(ipc, value);
            case Stream value:
                return StreamAsMeta(value, ipc);
            default:
                throw new Exception($"invalid body type {body}");
        }
    }

    private SMetaBody StreamAsMeta(Stream stream, Ipc ipc)
    {
        return new SMetaBody();
    }
}

