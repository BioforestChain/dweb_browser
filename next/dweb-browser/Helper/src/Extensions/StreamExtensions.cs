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

    public static string ToBase64(this byte[] self) =>
        Convert.ToBase64String(self);

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
        var ex_buf = new byte[1];
        var bytes = new byte[usize]!;
        var count = usize - 1;
        while (true)
        {
            try
            {
                await stream.ReadExactlyAsync(ex_buf, 0, 1);
            }
            catch (EndOfStreamException)
            {
                /// 流完成流
                yield break;
            }
            bytes[0] = ex_buf[0];
            var read = await stream.ReadAsync(bytes, 1, count);
            yield return bytes.AsSpan(0, read + 1).ToArray();
        }
    }
}