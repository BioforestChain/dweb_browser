using System;
using Xunit.Abstractions;

namespace ipc_test.ipc
{
    public class ReadableStreamTest : Log
    {
        public ReadableStreamTest(ITestOutputHelper output) : base(output)
        {
        }

        //[Fact]
        //[Trait("Ipc", "ReadableStream")]
        //public void Read_ReadableStreamDynamicWriteByte_ReturnsLength()
        //{
            
        //}

        [Fact]
        [Trait("Ipc", "ReadableStream")]
        public void Available_ReadableStream_ReturnsSuccess()
        {
            Console.WriteLine("start");
            var stream = new ReadableStream(
                null,
                (controller) =>
                {
                    Task.Run(async () =>
                    {
                        await Task.Delay(1000);
                        await controller.Enqueue(new byte[] { 1, 2, 3 });
                        Console.WriteLine("enqueued");
                    });
                });

            var result = 0;

            for (var i = 0; i < 10; i++)
            {
                Task.Run(() =>
                {
                    Console.WriteLine("task available");
                    Thread.Sleep(100);
                    var len = stream.Available();
                    Console.WriteLine($"stream.Available(): {len}");
                    Interlocked.Add(ref result, len);
                });
            }

            Thread.Sleep(10000);

            Assert.Equal(10*3, result);
        }
    }
}

