namespace ipc;

/**
 * <summary>
 * metaBody 可能会被多次转发，
 * 但只有第一次得到这个 metaBody 的 ipc 才是它真正意义上的 Receiver
 * </summary>
 */
public class IpcBodyReceiver : IpcBody
{
    public Ipc Ipc { get; set; }
    public IpcBodyReceiver(SMetaBody metaBody, Ipc ipc)
    {
        MetaBody = metaBody;
        Ipc = ipc;

        var ipcMetaBodyType = new SMetaBody.IpcMetaBodyType(MetaBody.Type);
        if (ipcMetaBodyType.IsStream)
        {
            if (!CACHE.MetaId_receiverIpc_Map.TryGetValue(MetaBody.MetaId, out Ipc? cipc))
            {
                Ipc.OnClose += () =>
                {
                    CACHE.MetaId_receiverIpc_Map.Remove(MetaBody.MetaId);
                };

                MetaBody = MetaBody with { ReceiverUid = Ipc.Uid };
                CACHE.MetaId_receiverIpc_Map.AddOrUpdate(MetaBody.MetaId, Ipc);
            }
        }
    }

    protected override BodyHubType BodyHub
    {
        get
        {
            return new Lazy<BodyHubType>(new Func<BodyHubType>(() => new BodyHubType().Also(it =>
            {
                object data = default;
                var ipcMetaBodyType = new SMetaBody.IpcMetaBodyType(MetaBody.Type);

                if (ipcMetaBodyType.IsStream)
                {
                    if (!CACHE.MetaId_receiverIpc_Map.TryGetValue(MetaBody.MetaId, out Ipc? ipc))
                    {
                        throw new Exception($"no found ipc by metaId: {MetaBody.MetaId}");
                    }

                    data = MetaToStream(MetaBody, ipc);
                }
                else
                {
                    switch (ipcMetaBodyType.Encoding)
                    {
                        case IPC_DATA_ENCODING.UTF8:
                            data = (string)MetaBody.Data;
                            break;
                        case IPC_DATA_ENCODING.BINARY:
                            data = (byte[])MetaBody.Data;
                            break;
                        case IPC_DATA_ENCODING.BASE64:
                            data = ((string)MetaBody.Data).FromBase64();
                            break;
                        default:
                            throw new Exception($"invalid metaBody type {MetaBody.Type}");
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

    public override SMetaBody MetaBody { get; set; }

    public override object? Raw
    {
        get { return BodyHub.Data; }
    }

    public static IpcBody From(SMetaBody metaBody, Ipc ipc) =>
        CACHE.MetaId_ipcBodySender_Map.TryGetValue(metaBody.MetaId, out IpcBody? ipcBody) ? ipcBody : new IpcBodyReceiver(metaBody, ipc);

    /**
     * <returns> {String | ByteArray | InputStream} </returns>
     */
    public static Stream MetaToStream(SMetaBody metaBody, Ipc ipc)
    {
        /// metaToStream
        var stream_id = metaBody.StreamId!;
        /**
         * 默认是暂停状态
         */
        var paused = 1;
        var stream = new ReadableStream($"receiver-{stream_id}",
            onStart: controller =>
            {
                /// 如果有初始帧，直接存起来
                var ipcMetaBodyType = new SMetaBody.IpcMetaBodyType(metaBody.Type);

                switch (ipcMetaBodyType.Encoding)
                {
                    case IPC_DATA_ENCODING.UTF8:
                        controller.Enqueue(((string)metaBody.Data).FromUtf8());
                        break;
                    case IPC_DATA_ENCODING.BINARY:
                        controller.Enqueue((byte[])metaBody.Data);
                        break;
                    case IPC_DATA_ENCODING.BASE64:
                        controller.Enqueue(((string)metaBody.Data).FromBase64());
                        break;
                    default:
                        break;
                }


                OnMessageHandler<IpcStream, Ipc> onStream = null!;
                onStream = async (ipcStream, ipc) =>
                {
                    if (ipcStream is IpcStreamData data && data.StreamId == stream_id)
                    {
                        Console.WriteLine($"receiver/StreamData/{ipc}/{controller.Stream}", data);
                        await controller.EnqueueAsync(data.Binary);
                    }
                    else if (ipcStream is IpcStreamEnd end && end.StreamId == stream_id)
                    {
                        Console.WriteLine($"receiver/StreamEnd/{ipc}/{controller.Stream}", end);
                        controller.Close();
                        ipc.OnStreamEvent.Remove(onStream);
                    }

                };

                ipc.OnStreamEvent.Listen(onStream);

                //ipc.OnStream(async (IpcStreamMessageArgs args) =>
                //{
                //    if (args.stream is IpcStreamData data && data.StreamId == stream_id)
                //    {
                //        Console.WriteLine($"receiver/StreamData/{ipc}/{controller.Stream}", data);
                //        await controller.EnqueueAsync(data.Binary);
                //    }
                //    else if (args.stream is IpcStreamEnd end && end.StreamId == stream_id)
                //    {
                //        Console.WriteLine($"receiver/StreamEnd/{ipc}/{controller.Stream}", end);
                //        controller.Close();
                //        return SIGNAL_CTOR.OFF;
                //    }

                //    return null;
                //});
            },
            onPull: async args =>
            {
                var controller = args.Item2;
                Console.WriteLine($"receiver/StreamEnd/{ipc}/{controller.Stream}", stream_id);
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

        Console.WriteLine($"receiver/{ipc}/{stream}");

        return stream;
    }
}