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
            _console.Log("ReadIntAsync", "start:{0}", self);
            await self.ReadExactlyAsync(buffer);
            _console.Log("ReadIntAsync", "end:{0}", self);
        }
        catch(Exception e)
        {
            _console.Error("ReadIntAsync", "exception: {0}", e);
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
        var bytes = new byte[usize]!;
        while (true)
        {
            var read = await stream.ReadAtLeastAsync(bytes, 1, false);
            if (read == 0)
            {
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