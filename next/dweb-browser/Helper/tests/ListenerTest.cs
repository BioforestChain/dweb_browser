namespace DwebBrowser.HelperTests;

public class ListenerTest
{
    [Fact]
    public void ListenerCancelTest()
    {
        var n = 0;
        var listener = Listener.New();
        var task1 = Task.Run(async () =>
        {
            Debug.WriteLine("task1 start");
            await listener.ToFlow().Collect(() =>
            {
                Debug.WriteLine("collect");
                n++;
            });
            Debug.WriteLine("task1 end");
        });

        var task2 = Task.Run(async () =>
        {
            await Task.Delay(200);
            await listener.Emit();
            await Task.Delay(100);
            await listener.Emit();
            await Task.Delay(300);
            await listener.Emit();
            await Task.Delay(400);
            await listener.Emit();
            await Task.Delay(500);
            await listener.Emit();
            await Task.Delay(1000);
            await listener.Emit();
        });

        Task.WhenAll(new Task[] { task1, task2 });

        while (true)
        {
            if (n > 5)
            {
                listener.Cancel();
                break;
            }
        }

        Assert.Equal(6, n);
    }
}

