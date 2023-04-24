
namespace DwebBrowser.HelperTests;

public class EventDelegateTest
{
	public EventDelegateTest(ITestOutputHelper output)
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
			Debug.WriteLine(String.Format("count: {0}", count));
			OnStream -= off;
			return count;
		};
        OnStreamHandler off2 = null!;
		off2 = count =>
        {
            Debug.WriteLine(String.Format("count2: {0}", count));
            OnStream -= off2;
			return count;
        };

        OnStream += off;
        OnStream += off2;

		Assert.Equal(1, OnStream.Invoke(1));
	}
}

