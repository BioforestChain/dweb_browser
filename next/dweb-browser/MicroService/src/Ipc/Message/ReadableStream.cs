using System.IO.Pipes;

namespace DwebBrowser.MicroService.Message;

public class ReadableStream
{
    static Debugger Console = new("ReadableStream");
    static int sidAcc = 0;
    string sid = "ReadableStream@" + Interlocked.Increment(ref sidAcc);
    public override string ToString()
    {
        return sid;
    }
    private AnonymousPipeServerStream pipeServer;
    private AnonymousPipeClientStream pipeClient;

    private ReadableStreamController controller = null!;
    public PipeStream Stream { init; get; }

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
                input.Close();
                onWriteClose?.Invoke();
            }
        }
    }

    public class PipeStream : Stream
    {
        internal static int sidAcc = 0;
        internal int id { init; get; }
        internal string sid { init; get; }
        internal string output_sid = "";
        public override string ToString()
        {
            return sid + output_sid;
        }

        private AnonymousPipeClientStream pipeStream;
        private Action onStartRead;
        public PipeStream(AnonymousPipeClientStream pipeStream, Action onStartRead)
        {
            this.id = Interlocked.Increment(ref sidAcc);
            this.sid = "R::Stream@" + id;
            this.pipeStream = pipeStream;
            this.onStartRead = onStartRead;
        }
        public override bool CanRead => !isEndRead && pipeStream.CanRead;
        public override bool CanSeek => pipeStream.CanSeek;
        public override bool CanWrite => pipeStream.CanWrite;
        public override long Length => pipeStream.Length;
        public override long Position { get => pipeStream.Position; set => pipeStream.Position = value; }
        public override void Flush() => pipeStream.Flush();

        private bool hasFirstRead = false;
        private bool isEndRead = false;
        private bool isClose = false;

        public override int Read(byte[] buffer, int offset, int count)
        {
            if (isEndRead)
            {
                return 0;
            }
            if (hasFirstRead is false)
            {
                hasFirstRead = true;
                onStartRead();
            }

            var bufferForWriter = offset switch
            {
                0 => count == buffer.Length ? buffer : buffer.AsSpan(0, count),
                _ => buffer.AsSpan(offset, count),
            };
            var readLen = pipeStream.ReadAtLeast(bufferForWriter, 1, false);
            if (readLen == 0)
            {
                isEndRead = true;
            }
            return readLen;
        }

        public override long Seek(long offset, SeekOrigin origin) => pipeStream.Seek(offset, origin);
        public override void SetLength(long value) => pipeStream.SetLength(value);
        public override void Write(byte[] buffer, int offset, int count) => pipeStream.Write(buffer, offset, count);
        public override void Close()
        {
            if (isClose && isEndRead)
            {
                return;
            }
            isClose = true;
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
        Stream = new PipeStream(pipeClient, delegate
        {
            onPull?.Invoke((1, controller));
        });
        controller = new ReadableStreamController(pipeServer, Stream, onClose);

        onStart?.Invoke(controller);
        this.onClose = onClose;
    }
}
