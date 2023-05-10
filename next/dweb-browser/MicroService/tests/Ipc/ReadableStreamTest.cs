using System.Threading;
using System.Threading.Tasks;
using DwebBrowser.MicroService.Core;

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
                Debug.WriteLine(String.Format("task available:{0}", stream.CanRead));
                var data = await stream.ReadBytesAsync(3);
                Debug.WriteLine(String.Format("read: {0}", data.ToUtf8()));
                //Debug.WriteLine(String.Format("stream.Available(): {0}", reader.BaseStream.Position));
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

                    Debug.WriteLine(String.Format("buffer: {0}", buffer.ToUtf8()));
                    Debug.WriteLine(String.Format("length: {0} position: {1}", stream.Length, stream.Position));


                    Assert.Equal(2, stream.Position);
                    Debug.WriteLine(String.Format("byte: {0}", stream.ReadByte()));
                    Assert.Equal(0, stream.Position);
                    Debug.WriteLine(String.Format("length: {0} position: {1}", stream.Length, stream.Position));

                    Debug.WriteLine("read task end");
                }
            }
            catch { }
        });

        Task.WaitAll(taskRead);

        //Assert.Equal();
    }

    record Event(ReadableStream.ReadableStreamController target, string data);
    event Signal<Event>? OnEvent;

    class M1 : NativeMicroModule
    {
        public M1() : base("m1")
        { }
        protected override Task _bootstrapAsync(IBootstrapContext bootstrapContext)
        {
            throw new NotImplementedException();
        }

        protected override Task _onActivityAsync(IpcEvent Event, Ipc ipc)
        {
            throw new NotImplementedException();
        }

        protected override Task _shutdownAsync()
        {
            throw new NotImplementedException();
        }
    }

    class M2 : NativeMicroModule
    {
        public M2() : base("m2")
        { }
        protected override Task _bootstrapAsync(IBootstrapContext bootstrapContext)
        {
            throw new NotImplementedException();
        }

        protected override Task _onActivityAsync(IpcEvent Event, Ipc ipc)
        {
            throw new NotImplementedException();
        }

        protected override Task _shutdownAsync()
        {
            throw new NotImplementedException();
        }
    }

    [Fact]
    public async Task BindIncomeStream_ReadableStreamBase_ReturnSuccess()
    {
        var i = 0;
        OnEvent += async (Event, _) =>
        {
            Debug.WriteLine(String.Format("Event {0}", Event.data));
            if (Event.data == "pull")
            {
                await Event.target.EnqueueAsync(i.ToByteArray());
            }
        };

        var m1 = new M1();
        var m2 = new M2();
        var req_ipc = new ReadableStreamIpc(m1, IPC_ROLE.CLIENT.ToString());
        var res_ipc = new ReadableStreamIpc(m2, IPC_ROLE.SERVER.ToString());

        res_ipc.BindIncomeStream(req_ipc.ReadableStream.Stream);

        res_ipc.OnRequest += async (request, ipc, _) =>
        {
            Debug.WriteLine($$"""req get request {{request}}""");
            await Task.Delay(200);
            //Debug.WriteLine(String.Format("echo after 1s {0}", request));
            await ipc.PostMessageAsync(IpcResponse.FromText(
                request.ReqId,
                200,
                new IpcHeaders(),
                String.Format("ECHO: {0}", request.Body.Text),
                ipc));
        };

        await Task.Delay(100);
        req_ipc.BindIncomeStream(res_ipc.ReadableStream.Stream);

        foreach (var j in Enumerable.Range(1, 10))
        {
            Debug.WriteLine(String.Format("开始发送 ${0}", j));
            var req = new PureRequest("https://www.baidu.com/", IpcMethod.Post, Body: new PureUtf8StringBody(String.Format("hi-{0}", j)));
            Debug.WriteLine(String.Format("req {0}", req));
            var res = await req_ipc.Request(req);
            Debug.WriteLine(String.Format("res {0}", res));
            Assert.Equal(await res.TextAsync(), String.Format("ECHO: {0}", req.Body.ToUtf8String()));
        }

        await req_ipc.Close();
    }
}

