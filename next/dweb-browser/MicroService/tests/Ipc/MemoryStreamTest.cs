using System.IO;

namespace DwebBrowser.MicroServiceTests;

public class MemoryStreamTest
{
    /// <summary>
    /// 初始化一个MemoryStream之后，就处于可读状态，并不需要填充数据
    /// </summary>
    [Fact]
    [Trait("Ipc", "MemoryStream")]
    public void CanRead_MemoryStreamInit_ReturnsTrue()
    {
        var stream = new MemoryStream();
        //Debug.WriteLine($"CanRead: {stream.CanRead}");

        Assert.True(stream.CanRead);
    }

    /// <summary>
    /// MemoryStream关闭之后，流处于不可读状态
    /// </summary>
    [Fact]
    [Trait("Ipc", "MemoryStream")]
    public void Close_MemoryStreamInit_ReturnsFalse()
    {
        var stream = new MemoryStream();

        stream.Close();

        Assert.False(stream.CanRead);
    }

    /// <summary>
    /// 当MemoryStream内容多于读取字节数时，buffer填满就返回
    /// </summary>
    [Fact]
    [Trait("Ipc", "MemoryStream")]
    public void ReadExactly_MemoryStreamMore_ReturnsExactlyLength()
    {
        var stream = new MemoryStream(new byte[] { 1, 2, 3, 4, 5 });

        var buffer = new byte[4];
        stream.Position = 0;
        stream.ReadExactly(buffer);
        //stream.Read(buffer);

        Assert.Equal(new byte[4] { 1, 2, 3, 4 }, buffer);

        // 剩余一个字节，为5
        Assert.Equal(5, stream.ReadByte());
    }

}

