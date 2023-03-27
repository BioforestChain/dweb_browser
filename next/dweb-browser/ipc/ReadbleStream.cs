using System.Threading.Tasks.Dataflow;

namespace ipc;

public class ReadbleStream: MemoryStream
{
	public string? Cid { get; set; }
	public Action<ReadableStreamController> OnStart { get; set; }
	public Action<(int, ReadableStreamController)> OnPull { get; set; }
    public Action OnClose { get; set; }

    public ReadbleStream(
		string? cid = null,
		Action<ReadableStreamController> onStart = default,
		Action<(int, ReadableStreamController)> onPull = default,
		Action onClose = default) : base()
	{
		Cid = cid;
		OnStart = onStart;
		OnPull = onPull;
		OnClose = onClose;
	}

	public class ReadableStreamController
	{
		private BufferBlock<byte[]> _bufferBlock { get; set; }
		public Func<ReadbleStream> GetStream { get; set; }

		public ReadableStreamController(BufferBlock<byte[]> bufferBlock, Func<ReadbleStream> getStream)
		{
			_bufferBlock = bufferBlock;
			GetStream = getStream;
		}

		public ReadbleStream RStream
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
}

