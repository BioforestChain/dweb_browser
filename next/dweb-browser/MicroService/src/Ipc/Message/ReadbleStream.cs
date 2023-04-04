using System.IO.Pipes;

namespace DwebBrowser.MicroService.Message;

//public class ReadableStream : MemoryStream
//{
//    public string? Cid { get; set; }
//    public Action<(int, ReadableStreamController)> OnPull { get; set; }
//    public Action OnClose { get; set; }

//    // 数据源
//    private byte[] _data = new byte[0];
//    // 指针
//    private int ptr = 0;

//    private Mutex _dataLock = new Mutex(false);


//    delegate void DataChangeEvent(long size);
//    private event DataChangeEvent _dataChangeObserver;

//    public ReadableStream(
//        string? cid = null,
//        Action<ReadableStreamController> onStart = default,
//        Action<(int, ReadableStreamController)> onPull = default,
//        Action onClose = default) : base()
//    {
//        Cid = cid;
//        OnPull = onPull;
//        OnClose = onClose;

//        _lazyController = new Lazy<ReadableStreamController>(() =>
//             new ReadableStreamController(_bufferBlock, () => this), true);

//        Task.Run(() => onStart(_controller)).Wait();

//        Task.Run(async () =>
//        {
//            // 一直等待数据
//            await foreach (byte[] chunk in _bufferBlock.ReceiveAllAsync())
//            {
//                _dataLock.WaitOne();

//                //await WriteAsync(chunk, 0, chunk.Length);
//                _data = _data.Combine(chunk);
//                Console.WriteLine($"DATA-IN/{Uid} +{chunk.Length} ~> {_data.Length}");

//                _dataLock.ReleaseMutex();

//                // 收到数据了，尝试解锁通知等待者
//                _dataChangeObserver?.Invoke(_data.Length);
//            }

//            // 关闭数据通道了，尝试解锁通知等待者
//            _dataChangeObserver?.Invoke(-1);

//            // 执行关闭
//            _closePo.Resolve(true);
//            // 执行生命周期回调
//            onClose();
//        });
//    }

//    private PromiseOut<bool> _closePo = new PromiseOut<bool>();

//    public Task<bool> AfterClosed() => _closePo.WaitPromiseAsync();

//    public bool IsClosed { get { return _closePo.IsFinished; } }

//    public long CanReadSize { get { return _data.Length - ptr; } }

//    public class ReadableStreamController
//    {
//        private BufferBlock<byte[]> _bufferBlock { get; set; }
//        public Func<ReadableStream> GetStream { get; set; }

//        public ReadableStreamController(BufferBlock<byte[]> bufferBlock, Func<ReadableStream> getStream)
//        {
//            _bufferBlock = bufferBlock;
//            GetStream = getStream;
//        }

//        public ReadableStream Stream
//        {
//            get { return GetStream(); }
//        }

//        public Task Enqueue(byte[] byteArray) => _bufferBlock.SendAsync(byteArray);

//        public void Close() => _bufferBlock.Complete();

//        public void Error(Exception e)
//        {
//            Console.WriteLine($"ReadableStreamController Error: {e.Message}");
//            _bufferBlock.Complete();
//        }
//    }

//    private BufferBlock<byte[]> _bufferBlock = new BufferBlock<byte[]>();
//    private Lazy<ReadableStreamController> _lazyController;
//    private ReadableStreamController _controller
//    {
//        get
//        {
//            return _lazyController.Value;
//        }
//    }

//    private static int s_id_acc = 1;

//    public string Uid
//    {
//        get
//        {
//            return $"#{Interlocked.Exchange(ref s_id_acc, Interlocked.Increment(ref s_id_acc))}" + ((Cid is not null) ? Cid! : "");
//        }
//    }

//    public override string ToString() => Uid;

//    private byte[] _requestData(int requestSize, bool waitFull)
//    {
//        var size = waitFull ? requestSize : 1;

//        // 如果下标满足条件，直接返回
//        if (CanReadSize >= size)
//        {
//            return _data;
//        }

//        Task.Run(async () =>
//        {
//            var wait = new PromiseOut<bool>();
//            DataChangeEvent ob = count =>
//            {
//                Console.WriteLine($"count: {count} CanReadSize: {CanReadSize}");
//                Console.WriteLine($"Length: {Length} Position: {Position}");
//                if (count == -1)
//                {
//                    Console.WriteLine($"REQUEST-DATA/END/{Uid} {CanReadSize}/{size}");
//                    wait.Resolve(true);
//                }
//                else if (CanReadSize >= size)
//                {
//                    Console.WriteLine($"REQUEST-DATA/CHANGED/{Uid} {CanReadSize}/{size}");
//                    wait.Resolve(false);
//                }
//                else
//                {
//                    Console.WriteLine($"REQUEST-DATA/WAITING-&-PULL/{Uid} {CanReadSize}/{size}");

//                    Task.Run(() =>
//                    {
//                        int desiredSize = (size - CanReadSize).ToInt();
//                        OnPull((desiredSize, _controller));
//                    });
//                }
//            };
//            _dataChangeObserver += ob;

//            await wait.WaitPromiseAsync();
//            _dataChangeObserver -= ob;
//            Console.WriteLine($"REQUEST-DATA/DONE/{Uid} {this.Length}");
//        }).Wait();

//        return _data;
//    }

//    public int Available()
//    {
//        return _requestData(1, true).Length;
//    }

//    public override long Length => _data.Length;
//    public override long Position { get => ptr; set => ptr = value.ToInt(); }

//    public override int Read(Span<byte> buffer)
//    {
//        try
//        {
//            var data = _requestData(1, true);
//            // 读取到没有数据后，会返回-1
//            var len = data.Length >= buffer.Length ? buffer.Length : data.Length;
//            new Span<byte>(data, ptr, len).CopyTo(buffer);
//            return ptr < _data.Length ? _data[ptr += len] : -1;
//        }
//        finally
//        {
//            _gc();
//        }
//    }

//    public override int Read(byte[] buffer, int offset, int count)
//    {
//        try
//        {
//            var data = _requestData(count, false);

//            if (ptr >= data.Length || count < 0)
//            {
//                // 流已读完
//                return -1;
//            }

//            if (count == 0)
//            {
//                return 0;
//            }

//            //return base.Read(buffer, offset, count);
//            var len = CanReadSize > count ? count : CanReadSize.ToInt();
//            _data.CopyTo(buffer, 0);

//            ptr += len;

//            // 返回读取的长度
//            return len;
//        }
//        finally
//        {
//            _gc();
//        }
//    }

//    public override int ReadByte()
//    {
//        try
//        {
//            var data = _requestData(1, true);

//            // 流已读完
//            if (ptr >= data.Length)
//            {
//                return -1;
//            }

//            return data[ptr++];
//        }
//        finally
//        {
//            _gc();
//        }
//    }

//    /**
//     * <summary>
//     * 执行垃圾回收
//     * 10kb 的垃圾起，开始回收
//     * </summary>
//     */
//    private void _gc()
//    {
//        Task.Run(() =>
//        {
//            _dataLock.WaitOne();

//            if (ptr >= 1 /*10240*/ || IsClosed)
//            {
//                Console.WriteLine($"GC/{Uid}");
//                _data = _data.Skip(ptr).ToArray();
//                ptr = 0;
//            }

//            _dataLock.ReleaseMutex();
//        }).Wait();
//    }

//    public override void Close()
//    {
//        if (IsClosed)
//        {
//            return;
//        }

//        Console.WriteLine($"CLOSE/{Uid}");
//        _closePo.Resolve(true);
//        _controller.Close();
//        // 关闭的时候不会马上清空数据，还是能读出来最后的数据的

//        base.Close();
//    }
//}


public class ReadableStream
{
    private AnonymousPipeServerStream pipeServer;
    private AnonymousPipeClientStream pipeClient;
    private PipeStream myStream;

    private ReadableStreamController controller = null!;
    public Stream Stream { get => myStream; }

    public class ReadableStreamController
    {
        private AnonymousPipeServerStream output;
        private PipeStream input;
        public Stream Stream { get => input; }
        private Action? onWriteClose;
        public ReadableStreamController(AnonymousPipeServerStream output, PipeStream input, Action? onWriteClose)
        {
            this.output = output;
            this.input = input;
            this.onWriteClose = onWriteClose;
        }
        public void Enqueue(byte[] data)
        {
            output.Write(data);
        }
        public Task EnqueueAsync(byte[] data, CancellationToken cancellationToken = default)
        {
            return output.WriteAsync(data, cancellationToken).AsTask();
        }
        private bool isClosed = false;
        public void Close()
        {
            lock (output)
            {
                if (isClosed) { return; }
                isClosed = true;
                output.Close();
                onWriteClose?.Invoke();
            }
        }
    }

    public class PipeStream : Stream
    {
        private AnonymousPipeClientStream pipeStream;
        private Action onStartRead;
        public PipeStream(AnonymousPipeClientStream pipeStream, Action onStartRead)
        {
            this.pipeStream = pipeStream;
            this.onStartRead = onStartRead;
        }

        public override bool CanRead => pipeStream.CanRead && !isEndRead;

        public override bool CanSeek => pipeStream.CanSeek;

        public override bool CanWrite => pipeStream.CanWrite;

        public override long Length => pipeStream.Length;

        public override long Position { get => pipeStream.Position; set => pipeStream.Position = value; }

        public override void Flush()
        {
            pipeStream.Flush();
        }

        private bool isFirstRead = true;
        private bool isEndRead = false;

        public override int Read(byte[] buffer, int offset, int count)
        {
            if (isFirstRead)
            {
                isFirstRead = false;
                onStartRead();
            }
            try
            {
                var readLen = pipeStream.ReadAtLeast(buffer.AsSpan(offset, count), 1);
                return readLen;
            }
            catch
            {
                Close();
                throw new ArgumentOutOfRangeException();
            }
        }

        public override long Seek(long offset, SeekOrigin origin)
        {
            return pipeStream.Seek(offset, origin);
        }

        public override void SetLength(long value)
        {
            pipeStream.SetLength(value);
        }

        public override void Write(byte[] buffer, int offset, int count)
        {
            pipeStream.Write(buffer, offset, count);
        }
        public override void Close()
        {
            if (isEndRead)
            {
                return;
            }
            isEndRead = true;
            base.Close();
        }
    }

    Action? onClose;
    public ReadableStream(
        String? cid = default,
        Action<ReadableStreamController>? onStart = default,
        Action<(int, ReadableStreamController)>? onPull = default,
        Action? onClose = default)
    {
        pipeServer = new AnonymousPipeServerStream(PipeDirection.Out);
        pipeClient = new AnonymousPipeClientStream(pipeServer.GetClientHandleAsString());
        myStream = new PipeStream(pipeClient, delegate
        {
            onPull?.Invoke((1, controller));
        });
        controller = new ReadableStreamController(pipeServer, myStream, onClose);

        onStart?.Invoke(controller);
        this.onClose = onClose;
    }
}
