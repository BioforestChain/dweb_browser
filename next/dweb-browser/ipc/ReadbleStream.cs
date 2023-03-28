using System.Threading.Tasks.Dataflow;
using Open.Observable;

namespace ipc;

public class ReadableStream : MemoryStream
{
    public string? Cid { get; set; }
    //public Action<ReadableStreamController> OnStart { get; set; }
    public Action<(int, ReadableStreamController)> OnPull { get; set; }
    public Action OnClose { get; set; }

    // 数据源
    private byte[] _data = new byte[0];
    // 指针
    private int ptr = 0;

    private Mutex _dataLock = new Mutex(false);

    private ObservableValue<int> _dataChangeObserver = ObservableValue.Create<int>(0);

    public ReadableStream(
        string? cid = null,
        Action<ReadableStreamController> onStart = default,
        Action<(int, ReadableStreamController)> onPull = default,
        Action onClose = default) : base()
    {
        Cid = cid;
        OnPull = onPull;
        OnClose = onClose;

        _lazyController = new Lazy<ReadableStreamController>(() =>
             new ReadableStreamController(_bufferBlock, () => this), true);

        Task.Run(() => onStart(_controller)).Wait();

        Task.Run(async () =>
        {
            // 一直等待数据
            await foreach (byte[] chunk in _bufferBlock.ReceiveAllAsync())
            {
                _dataLock.WaitOne();

                //await WriteAsync(chunk, 0, chunk.Length);
                _data = _data.Combine(chunk);
                Console.WriteLine($"DATA-IN/{Uid} +{chunk.Length} ~> {this.Length}");

                _dataLock.ReleaseMutex();

                // 收到数据了，尝试解锁通知等待者
                _dataChangeObserver.OnNext(_dataChangeObserver.Value + 1);
            }

            // 关闭数据通道了，尝试解锁通知等待者
            _dataChangeObserver.OnNext(-1);

            // 执行关闭
            _closePo.Resolve(true);
            // 执行生命周期回调
            onClose();
        });
    }

    private PromiseOut<bool> _closePo = new PromiseOut<bool>();

    public void AfterClosed() => _closePo.WaitPromise();

    public bool IsClosed { get { return _closePo.IsFinished; } }

    public long CanReadSize { get { return _data.Length - ptr; } }

    public class ReadableStreamController
    {
        private BufferBlock<byte[]> _bufferBlock { get; set; }
        public Func<ReadableStream> GetStream { get; set; }

        public ReadableStreamController(BufferBlock<byte[]> bufferBlock, Func<ReadableStream> getStream)
        {
            _bufferBlock = bufferBlock;
            GetStream = getStream;
        }

        public ReadableStream Stream
        {
            get { return GetStream(); }
        }

        public Task Enqueue(byte[] byteArray) => _bufferBlock.SendAsync(byteArray);

        public void Close() => _bufferBlock.Complete();

        public void Error(Exception e)
        {
            Console.WriteLine($"ReadableStreamController Error: {e.Message}");
            _bufferBlock.Complete();
        }
    }

    private BufferBlock<byte[]> _bufferBlock = new BufferBlock<byte[]>();
    private Lazy<ReadableStreamController> _lazyController;
    private ReadableStreamController _controller
    {
        get
        {
            return _lazyController.Value;
        }
    }

    private static int s_id_acc = 1;

    public string Uid
    {
        get
        {
            return $"#{Interlocked.Exchange(ref s_id_acc, Interlocked.Increment(ref s_id_acc))}" + ((Cid is not null) ? Cid! : "");
        }
    }

    public override string ToString() => Uid;

    //private int _requestData(int requestSize, bool waitFull)
    private byte[] _requestData(int requestSize, bool waitFull)
    {
        var size = waitFull ? requestSize : 1;

        // 如果下标满足条件，直接返回
        if (CanReadSize >= size)
        {
            //return CanReadSize.toInt();
            return _data;
        }

        Task.Run(() =>
        {
            var wait = new PromiseOut<bool>();
            CancellationTokenSource source = new CancellationTokenSource();

            var task = Task.Run(() =>
            {
                _dataChangeObserver.Subscribe(count =>
                {
                    Console.WriteLine($"count: {count} CanReadSize: {CanReadSize}");
                    Console.WriteLine($"Length: {Length} Position: {Position}");
                    if (count == -1)
                    {
                        Console.WriteLine($"REQUEST-DATA/END/{Uid} {CanReadSize}/{size}");
                        wait.Resolve(true);
                    }
                    else if (CanReadSize >= size)
                    {
                        Console.WriteLine($"REQUEST-DATA/CHANGED/{Uid} {CanReadSize}/{size}");
                        wait.Resolve(false);
                    }
                    else
                    {
                        Console.WriteLine($"REQUEST-DATA/WAITING-&-PULL/{Uid} {CanReadSize}/{size}");

                        Task.Run(() =>
                        {
                            int desiredSize = (size - CanReadSize).toInt();
                            OnPull((desiredSize, _controller));
                        });
                    }
                });
            }, source.Token);

            wait.WaitPromise();
            _dataChangeObserver.Dispose();
            source.Cancel();
            Console.WriteLine($"REQUEST-DATA/DONE/{Uid} {this.Length}");
        }).Wait();

        //return CanReadSize.toInt();
        return _data;
    }

    public int Available()
    {
        //return _requestData(1, true);
        return _requestData(1, true).Length;
    }

    public override int Read(Span<byte> buffer)
    {
        //var len = _requestData(1, true);
        var data = _requestData(1, true);
        // 读取到没有数据后，会返回-1
        //return len > 0 ? _data[ptr++] : -1;
        //data.CopyTo(buffer, 0);
        new Span<byte>(data, ptr, buffer.Length).CopyTo(buffer);
        return ptr < _data.Length ? _data[ptr++] : -1;
    }

    public override int Read(byte[] buffer, int offset, int count)
    {
        try
        {
            var remain = _requestData(count, false);

            if (ptr >= this.Length || count < 0)
            {
                // 流已读完
                return -1;
            }

            if (count == 0)
            {
                return 0;
            }

            //return base.Read(buffer, offset, count);
            var len = CanReadSize > count ? count : CanReadSize.toInt();
            _data.CopyTo(buffer, 0);

            ptr += len;

            // 返回读取的长度
            return len;
        }
        finally
        {
            _gc();
        }
    }

    /**
     * <summary>
     * 执行垃圾回收
     * 10kb 的垃圾起，开始回收
     * </summary>
     */
    private void _gc()
    {
        Task.Run(() =>
        {
            _dataLock.WaitOne();

            if (ptr >= 1 /*10240*/ || IsClosed)
            {
                Console.WriteLine($"GC/{Uid}");
                _data = _data.Skip(ptr).ToArray();
                ptr = 0;
            }

            _dataLock.ReleaseMutex();
        }).Wait();
    }

    public override void Close()
    {
        if (IsClosed)
        {
            return;
        }

        Console.WriteLine($"CLOSE/{Uid}");
        _closePo.Resolve(true);
        _controller.Close();
        // 关闭的时候不会马上清空数据，还是能读出来最后的数据的

        base.Close();
    }
}

