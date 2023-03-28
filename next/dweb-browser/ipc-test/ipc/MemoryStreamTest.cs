
using Xunit.Abstractions;

namespace ipc_test.ipc;

public class MemoryStreamTest: Log
{
	public MemoryStreamTest(ITestOutputHelper output) : base(output)
	{
		//new MemoryStream().ReadExactly();
		//new MemoryStream().CanRead
		//new MemoryStream().
	}

	public Mock<MemoryStream> mock = new Mock<MemoryStream>();

	/// <summary>
	/// 初始化一个MemoryStream之后，就处于可读状态，并不需要填充数据
	/// </summary>
	[Fact]
	[Trait("Ipc", "MemoryStream")]
	public void CanRead_MemoryStreamInit_ReturnsTrue()
	{
		var stream = new MemoryStream();
        //Console.WriteLine($"CanRead: {stream.CanRead}");

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

	/// <summary>
	/// 当MemoryStream内容不够buffer的容量时，buffer读完stream返回
	/// </summary>
	[Fact]
	[Trait("Ipc", "MemoryStream")]
	public void ReadExactly_MemoryStreamFull_ReturnsExactlyLength()
	{
        var stream = new MemoryStream(new byte[] { 1, 2, 3, 4 });
		//var stream = new MemoryStream(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 });

		var buffer = new byte[10];

		long offset = 0;
		while (true)
		{
			stream.Position = 0;

			/// Read 和 ReadExactly 差异就在第三个字段
			/// 如果流的内容长度不足第三个字段指定的长度时
			/// Read可以正常读取，不受影响，而ReadExactly则会直接抛出异常
			//stream.Read(buffer, offset.toInt(), (buffer.LongLength - offset).toInt());
			stream.ReadExactly(buffer, offset.toInt(), (buffer.LongLength - offset).toInt());
			offset += stream.Position;
			Console.WriteLine($"offset: {offset}");
			Console.WriteLine($"remains: {(buffer.LongLength - offset).toInt()}");

			if (offset >= buffer.LongLength)
			{
				break;
			}
		}

		Assert.Equal(10, offset);

		Assert.Equal(new byte[10] { 1, 2, 3, 4, 1, 2, 3, 4, 1, 2 }, buffer);
	}

	[Fact]
	[Trait("Ipc", "MemoryStream")]
	public void ReadExactlyAsync_MemoryStreamDynamicWriteByte_ReturnsExactlyLength()
	{
		var stream = new MemoryStream();
		bool task1 = false, task2 = false;

		Task.Run(() =>
		{
			Console.WriteLine("Task1 start sleep");
			Thread.Sleep(5000);
            Console.WriteLine("Task1 end sleep start write");
            var buffer = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };

            stream.Write(buffer, 0, 11);
            Console.WriteLine("Task1 write end");
			task1 = true;
        });

		Task.Run(() =>
		{
			Console.WriteLine("Task2 start");
			var buffer = new byte[11];
			while (stream.Read(buffer) >= 0)
			{
				Console.WriteLine("no byte read");
			}
			Console.WriteLine("Task2 end");
			task2 = true;
		});

		while (!(task1 && task2))
		{
			
		}

		Console.WriteLine("end");
	}
}

