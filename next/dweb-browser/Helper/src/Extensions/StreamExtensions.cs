using System.IO;
using System.Runtime.CompilerServices;
using System.Xml.Linq;

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
        new BinaryReader(self);


    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public static async Task<int> ReadIntAsync(this Stream self)
    {
        var buffer = new byte[4];
        await self.ReadExactlyAsync(buffer);
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

    public static async IAsyncEnumerable<byte[]> ReadBytesStream(this Stream stream, long usize = 4096)
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

    public static IEnumerable<byte[]> GetEnumerator(this Stream stream, long usize = 4096)
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