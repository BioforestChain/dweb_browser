namespace ipc.extensions;

static class StreamExtensions
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
}