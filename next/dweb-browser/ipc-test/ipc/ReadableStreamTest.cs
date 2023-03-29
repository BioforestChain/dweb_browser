using System;
using ipc.extensions;
using Xunit.Abstractions;

namespace ipc_test.ipc
{
    public class ReadableStreamTest : Log
    {
        public ReadableStreamTest(ITestOutputHelper output) : base(output)
        {
        }

        /// <summary>
        /// 验证是否Available可以卡住，等到有数据之后才往下执行
        /// </summary>
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
                (controller) =>
                {
                    Task.Run(async () =>
                    {
                        Console.WriteLine("write task start");
                        byte i = 0;
                        while (i++ < 10)
                        {
                            await Task.Delay(1000);
                            await controller.Enqueue(new byte[] { i, (byte)(i + (byte)1), (byte)(i + (byte)2) });
                            Console.WriteLine("enqueued");
                        }

                        Console.WriteLine("write task end");
                    });
                });

            var taskRead = Task.Run(() =>
            {
                Console.WriteLine("read task start");

                var buffer = new byte[2];
                
                //if (stream.Read(buffer, 0, 30) > 0)
                if (stream.Read(buffer) > 0)
                {
                    Console.WriteLine($"buffer: {buffer.ToUtf8()}");
                    Console.WriteLine($"length: {stream.Length} position: {stream.Position}");
                }

                Assert.Equal(2, stream.Position);
                Console.WriteLine($"byte: {stream.ReadByte()}");
                Assert.Equal(0, stream.Position);
                Console.WriteLine($"length: {stream.Length} position: {stream.Position}");

                Console.WriteLine("read task end");
            });

            Task.WaitAll(taskRead);

            //Assert.Equal();
        }
    }
}

