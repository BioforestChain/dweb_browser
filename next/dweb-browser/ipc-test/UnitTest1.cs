using Xunit.Abstractions;

namespace ipc_test;

public class UnitTest1 : Log
{
    public UnitTest1(ITestOutputHelper output) : base(output)
    {
    }

    [Fact]
    public void Test1()
    {
        //var ipc = new Ipc();
        //Console.WriteLine($"XIXIXI:{ipc.Add(3, 4)}");
        //Assert.Equal(7, ipc.Add(3, 4));
        var ipcReqMessage = new IpcReqMessage(
            0,
            IpcMethod.Connect,
            "https://www.baidu.com",
            new Dictionary<string, string> { { "content-type", "application/json" }, { "encoding", "utf-8" } },
            new SMetaBody(SMetaBody.IPC_META_BODY_TYPE.STREAM_ID, 0, "111", "222", 1)
            );

        //Assert.Equal("")
    }
}
