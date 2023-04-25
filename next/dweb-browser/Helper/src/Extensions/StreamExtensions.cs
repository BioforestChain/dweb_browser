using System.IO;
using System.Runtime.CompilerServices;
using System.Xml.Linq;

namespace DwebBrowser.Helper;

public static class StreamExtensions
{
    public static byte[] ToByteArray(this Stream self)
    {
        var bytes = new byte[self.Length];
        if (bytes.Length != self.Read(bytes))
        {
            throw new IndexOutOfRangeException();
        }
        return bytes;
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

    public static async Task<byte[]> ReadBytesAsync(this Stream self, int size)
    {
        var buffer = new byte[size];
        //return new BinaryReader(self).ReadBytes(size);
        try
        {
            await self.ReadExactlyAsync(buffer);
            return buffer;
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            throw e;
        }
    }
    public static async IAsyncEnumerable<byte[]> ReadBytesStream(this Stream stream, int usize = 4096)
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
}