namespace micro_service.extensions;

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


    public static async Task<int> ReadIntAsync(this Stream self)
    {
        var buffer = new byte[4];
        await self.ReadExactlyAsync(buffer);
        return buffer.ToInt();
    }
    public static async Task<byte[]> ReadBytesAsync(this Stream self,int size)
    {
        var buffer = new byte[size];
        await self.ReadExactlyAsync(buffer);
        return buffer;
    }
}