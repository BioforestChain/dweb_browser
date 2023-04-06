namespace DwebBrowser.MicroServiceTests;

public class ReadableStreamTest
{

    /// <summary>
    /// 验证是否Available可以卡住，等到有数据之后才往下执行
    /// </summary>
    [Fact]
    [Trait("Ipc", "ReadableStream")]
    public async void Available_ReadableStream_ReturnsSuccess()
    {
        Debug.WriteLine("start");
        var stream = new ReadableStream(onStart: async (controller) =>
        {
            await Task.Delay(1000);
            await controller.EnqueueAsync(new byte[] { 1, 2, 3 });
            await Task.Delay(1000);
            await controller.EnqueueAsync(new byte[] { 4, 5, 6 });
            await Task.Delay(1000);
            await controller.EnqueueAsync(new byte[] { 7, 8, 9 });
            controller.Close();
        }).Stream;

        var result = 0;

        try
        {

            while (stream.CanRead)
            {
                Debug.WriteLine($"task available:{stream.CanRead}");
                var data = await stream.ReadBytesAsync(3);
                Debug.WriteLine($"read: {data.ToUtf8()}");
                //Debug.WriteLine($"stream.Available(): {reader.BaseStream.Position}");
                Interlocked.Increment(ref result);

            }
        }
        catch { }


        Assert.Equal(3, result);
        Assert.False(stream.CanRead);
    }
    /// <summary>
    /// 验证Read是否可以卡住，读取内容少于流内容时，指针位置是否正常
    /// ReadByte继续读取是否正常，读取后指针是否正常后移，如果流读取完成，是否触发gc
    /// </summary>
    [Fact]
    [Trait("Ipc", "ReadableStream")]
    public void Read_ReadableStreamAsyncWriteByte_ReturnsReadByte()
    {
        var stream = new ReadableStream(
            null,
            async (controller) =>
            {

                Debug.WriteLine("write task start");
                byte i = 0;
                while (i++ < 10)
                {
                    await Task.Delay(100);
                    await controller.EnqueueAsync(new byte[] { i, (byte)(i + (byte)1), (byte)(i + (byte)2) });
                    Debug.WriteLine("enqueued");
                }

                Debug.WriteLine("write task end");
            }).Stream;

        var taskRead = Task.Run(async () =>
        {
            try
            {
                while (stream.CanRead)
                {

                    Debug.WriteLine("read task start");

                    var buffer = await stream.ReadBytesAsync(2);

                    //if (stream.Read(buffer, 0, 30) > 0)

                    Debug.WriteLine($"buffer: {buffer.ToUtf8()}");
                    Debug.WriteLine($"length: {stream.Length} position: {stream.Position}");


                    Assert.Equal(2, stream.Position);
                    Debug.WriteLine($"byte: {stream.ReadByte()}");
                    Assert.Equal(0, stream.Position);
                    Debug.WriteLine($"length: {stream.Length} position: {stream.Position}");

                    Debug.WriteLine("read task end");
                }
            }
            catch { }
        });

        Task.WaitAll(taskRead);

        //Assert.Equal();
    }
}

