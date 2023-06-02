namespace DwebBrowser.MicroService.Message;

/**
 * <summary>
 * metaBody 可能会被多次转发，
 * 但只有第一次得到这个 metaBody 的 ipc 才是它真正意义上的 Receiver
 * </summary>
 */
public class IpcBodyReceiver : IpcBody
{
    static readonly Debugger Console = new("IpcBodyReceiver");
    public Ipc Ipc { get; set; }
    public IpcBodyReceiver(MetaBody metaBody, Ipc ipc)
    {
        MetaBody = metaBody;
        Ipc = ipc;

        if (MetaBody.Type_IsStream)
        {
            CACHE.MetaId_receiverIpc_Map.GetValueOrPut(MetaBody.MetaId, () =>
            {
                Ipc.OnClose += async (_) =>
                {
                    CACHE.MetaId_receiverIpc_Map.Remove(MetaBody.MetaId);
                };

                MetaBody.ReceiverUid = Ipc.Uid;
                return Ipc;
            });
        }
    }

    protected override BodyHubType BodyHub
    {
        get
        {
            return new Lazy<BodyHubType>(new Func<BodyHubType>(() => new BodyHubType().Also(it =>
            {
                object data;

                if (MetaBody.Type_IsStream)
                {
                    if (!CACHE.MetaId_receiverIpc_Map.TryGetValue(MetaBody.MetaId, out Ipc? ipc))
                    {
                        throw new Exception(string.Format("no found ipc by metaId: {0}", MetaBody.MetaId));
                    }

                    data = MetaToStream(MetaBody, ipc);
                }
                else
                {
                    switch (MetaBody.Type_Encoding)
                    {
                        case IPC_DATA_ENCODING.UTF8:
                            data = (string)MetaBody.Data;
                            break;
                        case IPC_DATA_ENCODING.BINARY:
                            data = (byte[])MetaBody.Data;
                            break;
                        case IPC_DATA_ENCODING.BASE64:
                            data = ((string)MetaBody.Data).ToBase64ByteArray();
                            break;
                        default:
                            throw new Exception(string.Format("invalid metaBody type {0}", MetaBody.Type));
                    }
                }

                it.Data = data;

                switch (data)
                {
                    case string str:
                        it.Text = str;
                        break;
                    case byte[] byteArray:
                        it.U8a = byteArray;
                        break;
                    case Stream stream:
                        it.Stream = stream;
                        break;
                }
            })), true).Value;
        }
    }

    public override MetaBody MetaBody { get; set; }

    public override object? Raw
    {
        get { return BodyHub.Data; }
    }

    public static IpcBody From(MetaBody metaBody, Ipc ipc) =>
        CACHE.MetaId_ipcBodySender_Map.TryGetValue(metaBody.MetaId, out IpcBody? ipcBody) ? ipcBody : new IpcBodyReceiver(metaBody, ipc);

    /**
     * <returns> {String | ByteArray | InputStream} </returns>
     */
    public static Stream MetaToStream(MetaBody metaBody, Ipc ipc)
    {
        /// metaToStream
        var stream_id = metaBody.StreamId!;
        /**
         * 默认是暂停状态
         */
        var paused = 1;
        var stream = new ReadableStream(string.Format("receiver-{0}", stream_id),
            onStart: async controller =>
            {
                /// 如果有初始帧，直接存起来
                var firstData = metaBody.Type_Encoding switch
                {
                    IPC_DATA_ENCODING.UTF8 => ((string)metaBody.Data).ToUtf8ByteArray(),
                    IPC_DATA_ENCODING.BINARY => (byte[])metaBody.Data,
                    IPC_DATA_ENCODING.BASE64 => ((string)metaBody.Data).ToBase64ByteArray(),
                    _ => null
                };
                if (firstData is not null)
                {
                    await controller.EnqueueAsync(firstData);
                }


                Signal<IpcStream, Ipc> cb = async (ipcStream, ipc, self) =>
                {
                    if (ipcStream is IpcStreamData data)
                    {
                        if (data.StreamId == stream_id)
                        {
                            Console.Log("OnStream", "receiver/StreamData/{0}/{1}/{2} {3}", ipc, controller.Stream, stream_id, data);
                            await controller.EnqueueAsync(data.Binary);
                        }
                    }
                    else if (ipcStream is IpcStreamEnd end)
                    {
                        if (end.StreamId == stream_id)
                        {
                            Console.Log("OnStream", "receiver/StreamEnd/{0}/{1} {2}", ipc, controller.Stream, end);
                            controller.Close();
                            ipc.OnStream -= self;
                        }
                    }
                };

                ipc.OnStream += cb;
            },
            onPull: async args =>
            {
                var (_, controller) = args;
                Console.Log("OnPull", "receiver/StreamPull/{0}/{1} {2}", ipc, controller.Stream, stream_id);
                if (Interlocked.CompareExchange(ref paused, 1, 0) == 1)
                {
                    await ipc.PostMessageAsync(new IpcStreamPulling(stream_id));
                }
            },
            onClose: async () =>
            {
                await ipc.PostMessageAsync(new IpcStreamAbort(stream_id));
            }
            ).Stream;

        Console.Log("MetaToStream", "receiver/{0}/{1} {0}", ipc, stream);

        return stream;
    }
}