
namespace ipc_test.helper;

public class EventDelegateTest: Log
{
	public EventDelegateTest(ITestOutputHelper output): base(output)
	{
	}

	delegate int OnStreamHandler(int count);
	event OnStreamHandler OnStream;

	[Fact]
	public void EventDelegate_Listen_Returns()
	{
		OnStreamHandler off = null!;
		off = count =>
		{
			Console.WriteLine($"count: {count}");
			OnStream -= off;
			return count;
		};
        OnStreamHandler off2 = null!;
		off2 = count =>
        {
            Console.WriteLine($"count2: {count}");
            OnStream -= off2;
			return count;
        };

        OnStream += off;
        OnStream += off2;

		Assert.Equal(1, OnStream.Invoke(1));
	}
}

