using System.IO.Pipes;
using System.Text;

namespace DwebBrowser.HelperTests;


public class NamedPipeTest
{

    public NamedPipeTest(ITestOutputHelper output) 
    {
    }

    [Fact]
    void testNamedPipe()
    {
        AnonymousPipeServerStream pipeServer = new AnonymousPipeServerStream(PipeDirection.Out);

        AnonymousPipeClientStream pipeClient = new AnonymousPipeClientStream(PipeDirection.In, pipeServer.GetClientHandleAsString());



        var task2 = Task.Run(async delegate
        {
            for (int i = 1; i <= 10; i++)
            {
                await Task.Delay(100);
                var message = $"Hi{i}";
                var buffer = Encoding.UTF8.GetBytes(message);
                pipeServer.Write(buffer, 0, buffer.Length);
                Debug.WriteLine("已发送消息：{0}", message);
            }
            //pipeServer.DisposeLocalCopyOfClientHandle();
            //pipeClient.Close();
            pipeServer.Close();
            //var bufferx = Encoding.UTF8.GetBytes($"Hi{99}");
            //pipeServer.Write(bufferx, 0, bufferx.Length);
        });

        var task1 = Task.Run(async delegate
        {

            try
            {

                while (pipeClient.IsConnected)
                {
                    await Task.Delay(500);
                    var buffer = new byte[4000];
                    var len = await pipeClient.ReadAtLeastAsync(buffer, 1);
                    var message = buffer.ToUtf8(0,len);
                    Debug.WriteLine("收到回复消息：{0} {1}", message, pipeClient.IsConnected);
                }

                Debug.WriteLine($"正常关闭了 {pipeClient.IsConnected}/{pipeClient.CanRead}");
            }
            catch (EndOfStreamException e)
            {
                Debug.WriteLine($"异常关闭了EndOfStreamException {pipeClient.IsConnected}/{pipeClient.CanRead}");
            }
            catch (ObjectDisposedException e)
            {
                Debug.WriteLine($"异常关闭了ObjectDisposedException {pipeClient.IsConnected}/{pipeClient.CanRead}");
            }
        });


        //try
        //{
        Task.WaitAll(task1, task2);
        //}
        //catch { }
    }
}

