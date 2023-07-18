using System.IO;
using System.IO.Pipes;

namespace DwebBrowser.MicroService.Message;

public class ReadableStream
{
    static int sidAcc = 0;
    readonly string sid = "ReadableStream@" + Interlocked.Increment(ref sidAcc);
    public override string ToString()
    {
        return sid;
    }
    private readonly AnonymousPipeServerStream pipeServer;
    private readonly AnonymousPipeClientStream pipeClient;

    private readonly ReadableStreamController controller = null!;
    public PipeStream Stream { init; get; }

    public class ReadableStreamController
    {
        private readonly AnonymousPipeServerStream writer;
        private readonly PipeStream stream;
        public Stream Stream { get => stream; }
        private readonly Action? onWriteClose;
        public ReadableStreamController(AnonymousPipeServerStream writer, PipeStream stream, Action? onWriteClose)
        {
            this.writer = writer;
            this.stream = stream;
            this.onWriteClose = onWriteClose;
        }
        public void Enqueue(byte[] data)
        {
            if (stream.IsClose) {
                throw new IOException("Cannot enqueue a chunk into a readable stream that is closed or has been requested to be closed.");
            }
            writer.Write(data);
        }
        public Task EnqueueAsync(byte[] data, CancellationToken cancellationToken = default)
        {
            if (stream.IsClose)
            {
                throw new IOException("Cannot enqueue a chunk into a readable stream that is closed or has been requested to be closed.");
            }
            return writer.WriteAsync(data, cancellationToken).AsTask();
        }
        public void Close()
        {
            lock (writer)
            {
                stream.Close();
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

        private readonly AnonymousPipeServerStream writer;
        private readonly AnonymousPipeClientStream reader;
        private readonly Action onStartRead;
        private readonly Action? onWriteClose;
        public PipeStream(AnonymousPipeServerStream writer, AnonymousPipeClientStream reader, Action onStartRead, Action? onWriteClose)
        {
            this.id = Interlocked.Increment(ref sidAcc);
            this.sid = "R::Stream@" + id;
            this.writer = writer;
            this.reader = reader;
            this.onStartRead = onStartRead;
            this.onWriteClose = onWriteClose;
        }
        public override bool CanRead => !isEndRead && reader.CanRead;
        public override bool CanSeek => reader.CanSeek;
        public override bool CanWrite => reader.CanWrite;
        public override long Length => reader.Length;
        public override long Position { get => reader.Position; set => reader.Position = value; }
        public override void Flush() => reader.Flush();

        private bool hasFirstRead = false;
        private bool isEndRead = false;
        public bool IsClose { get; private set; }

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
            var readLen = reader.ReadAtLeast(bufferForWriter, 1, false);
            if (readLen == 0)
            {
                isEndRead = true;
                writer.Close();
            }
            return readLen;
        }

        public override async Task<int> ReadAsync(byte[] buffer, int offset, int count, CancellationToken cancellationToken = default)
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
                0 => count == buffer.Length ? buffer : buffer.AsMemory(0, count),
                _ => buffer.AsMemory(offset, count),
            };
            var readLen = await reader.ReadAtLeastAsync(bufferForWriter, 1, false);
            if (readLen == 0)
            {
                isEndRead = true;
                base.Close();
            }
            return readLen;
        }

        public override long Seek(long offset, SeekOrigin origin) => reader.Seek(offset, origin);
        public override void SetLength(long value) => reader.SetLength(value);
        public override void Write(byte[] buffer, int offset, int count) => reader.Write(buffer, offset, count);
        public override void Close()
        {
            if (IsClose)
            {
                return;
            }
            lock (writer)
            {
                IsClose = true;
                writer.Close();
                onWriteClose?.Invoke();
            }
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
        Stream = new PipeStream(pipeServer, pipeClient, delegate
        {
            onPull?.Invoke((1, controller));
        }, onClose);
        controller = new ReadableStreamController(pipeServer, Stream, onClose);

        onStart?.Invoke(controller);
        this.onClose = onClose;
    }
}
