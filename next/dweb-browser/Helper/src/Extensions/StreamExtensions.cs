//using System.Diagnostics;

namespace DwebBrowser.Helper;

public static class StreamExtensions
{
    public static byte[] ToByteArray(this Stream self)
    {
        IEnumerable<byte> result = new byte[0];
        foreach (var bytes in self.GetEnumerator())
        {
            result = result.Concat(bytes);
        }
        return result.ToArray();
    }
    public static async Task<byte[]> ToByteArrayAsync(this Stream self)
    {
        IEnumerable<byte> result = new byte[0];
        await foreach (var bytes in self.ReadBytesStream())
        {
            result = result.Concat(bytes);
        }
        return result.ToArray();
    }

    public static BinaryReader GetBinaryReader(this Stream self) =>
        new(self);

    private static readonly LazyBox<Debugger> _lazyConsole = new();
    private static Debugger _console
    {
        get => _lazyConsole.GetOrPut(() => new("StreamExtensions"));
    }

    //[MethodImpl(MethodImplOptions.AggressiveInlining)]
    public static async Task<int> ReadIntAsync(this Stream self)
    {
        var buffer = new byte[4];
        try
        {
            await self.ReadExactlyAsync(buffer);
        }
        catch (Exception e)
        {
            if (e is EndOfStreamException)
            {
                _console.Warn("ReadIntAsync", "exception: {0}/{1}", self, e);
                self.Close();
            }
            else
            {
                _console.Error("ReadIntAsync", "exception: {0}/{1}", self, e);
            }
        }
        return buffer.ToInt();
    }

    public static async Task<byte[]> ReadBytesAsync(this Stream self, long size)
    {
        var buffer = new byte[size];
        //return new BinaryReader(self).ReadBytes(size);
        // try
        // {
        await self.ReadExactlyAsync(buffer);
        return buffer;
        // }
        // catch 
        // {
        //     throw;
        // }
    }
    private const int DefaultStreamChunkSize = 64 * 1024; // 64kb

    public static async IAsyncEnumerable<byte[]> ReadBytesStream(this Stream stream, long usize = DefaultStreamChunkSize)
    {
        _console.Log("RR", "START/{0}", stream);
        var bytes = new byte[usize]!;
        while (true)
        {
            var read = await stream.ReadAtLeastAsync(bytes, 1, false);
            if (read == 0)
            {
                _console.Log("RR", "END/{0}", stream);
                /// 流完成流
                yield break;
            }
            yield return bytes.AsSpan(0, read).ToArray();
        }
    }

    public static IEnumerable<byte[]> GetEnumerator(this Stream stream, long usize = DefaultStreamChunkSize)
    {
        var bytes = new byte[usize]!;
        while (true)
        {
            var read = stream.ReadAtLeast(bytes, 1, false);
            if (read == 0)
            {
                /// 流完成流
                yield break;
            }
            yield return bytes.AsSpan(0, read).ToArray();
        }
    }
}