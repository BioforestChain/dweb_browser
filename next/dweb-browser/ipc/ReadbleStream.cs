using System.Threading.Tasks.Dataflow;
using Open.Observable;

namespace ipc;

public class ReadbleStream : MemoryStream
{
    public string? Cid { get; set; }
    //public Action<ReadableStreamController> OnStart { get; set; }
    public Action<(int, ReadableStreamController)> OnPull { get; set; }
    public Action OnClose { get; set; }

    private Mutex _dataLock = new Mutex(false);

    private ObservableValue<int> _dataChangeObserver = ObservableValue.Create<int>(0);

    public ReadbleStream(
        string? cid = null,
        Action<ReadableStreamController> onStart = default,
        Action<(int, ReadableStreamController)> onPull = default,
        Action onClose = default) : base()
    {
        Cid = cid;
        OnPull = onPull;
        OnClose = onClose;

        Task.Run(() => onStart(_controller)).Wait();

        Task.Run(async () =>
        {
            // 一直等待数据
            await foreach (byte[] chunk in _bufferBlock.ReceiveAllAsync())
            {
                _dataLock.WaitOne();

                await WriteAsync(chunk, 0, chunk.Length);
                Console.WriteLine($"DATA-IN/{Uid}", $"+{chunk.Length} ~> {this.Length}");

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

    public Int64 CanReadSize { get { return this.Length; } }

    public class ReadableStreamController
    {
        private BufferBlock<byte[]> _bufferBlock { get; set; }
        public Func<ReadbleStream> GetStream { get; set; }

        public ReadableStreamController(BufferBlock<byte[]> bufferBlock, Func<ReadbleStream> getStream)
        {
            _bufferBlock = bufferBlock;
            GetStream = getStream;
        }

        public ReadbleStream Stream
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
    private ReadableStreamController _controller
    {
        get
        {
            return new Lazy<ReadableStreamController>(
                new Func<ReadableStreamController>(() =>
                    new ReadableStreamController(
                        _bufferBlock,
                        new Func<ReadbleStream>(() => this))), true).Value;
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

    private bool _requestData(int requestSize, bool waitFull)
    {
        var size = waitFull ? requestSize : 1;

        // 如果下标满足条件，直接返回
        if (this.Length >= size)
        {
            return true;
        }

        Task.Run(() =>
        {
            var wait = new PromiseOut<bool>();
            CancellationTokenSource source = new CancellationTokenSource();

            var task = Task.Run(() =>
            {
                _dataChangeObserver.Subscribe(count =>
                {
                    if (count == -1)
                    {
                        Console.WriteLine($"REQUEST-DATA/END/{Uid}", $"{CanReadSize}/{size}");
                        wait.Resolve(true);
                    }
                    else if (CanReadSize >= size)
                    {
                        Console.WriteLine($"REQUEST-DATA/CHANGED/{Uid}", $"{CanReadSize}/{size}");
                        wait.Resolve(false);
                    }
                    else
                    {
                        Console.WriteLine($"REQUEST-DATA/WAITING-&-PULL/{Uid}", $"{CanReadSize}/{size}");

                        Task.Run(() =>
                        {
                            int desiredSize = (int)(size - CanReadSize);
                            OnPull((desiredSize, _controller));
                        });
                    }
                });
            }, source.Token);

            wait.WaitPromise();
            source.Cancel();
            Console.WriteLine($"REQUEST-DATA/DONE/{Uid}", $"{this.Length}");
        }).Wait();

        return this.Length > 0;
    }

    public int Available()
    {
        return _requestData(1, true) ? (int)this.Length : 0;
    }
}

