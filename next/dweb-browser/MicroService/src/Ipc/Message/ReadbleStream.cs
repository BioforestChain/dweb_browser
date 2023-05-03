using System.IO.Pipes;

namespace DwebBrowser.MicroService.Message;

public class ReadableStream
{
    static int sidAcc = 0;
    string sid = "ReadableStream" + Interlocked.Increment(ref sidAcc);
    public override string ToString()
    {
        return sid;
    }
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
        static int sidAcc = 0;
        string sid = "R::Stream" + Interlocked.Increment(ref sidAcc);
        public override string ToString()
        {
            return sid;
        }

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
