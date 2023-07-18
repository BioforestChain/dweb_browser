using System;
namespace DwebBrowser.MicroServiceTests.Sys;

public class EventTest
{
    class QAQ
    {
        public  Action? onData = default;
    }


    [Fact]
    public async Task TestBinding()
    {
        var qaq = new QAQ();
        var T = 1000;
        var tasks = new List<Task>();
        var t = 0;
        foreach (var i in Enumerable.Range(0, T))
        {
            tasks.Add(Task.Run(() =>
            {
                Interlocked.Increment(ref t);
                qaq.onData += () => { };
            }));
        }
        Task.WaitAll(tasks.ToArray());
        Assert.Equal(T, t);
        Assert.Equal(T, qaq.onData?.GetInvocationList().Length);
    }

}

