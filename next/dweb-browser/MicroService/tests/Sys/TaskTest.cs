using System.Collections.Generic;
//using DwebBrowser.MicroService.Sys.Jmm;

namespace DwebBrowser.MicroServiceTests;

public class TaskTest
{
    [Fact]
    public async Task TaskWithLock()
    {
        _ = Task.Run(() => TryLock());
        _ = Task.Run(() => TryLock());
        await Task.Delay(2200);
    }
    Dictionary<string, int> map = new();
    async Task TryLock()
    {
        lock (map)
        {
            Debug.WriteLine("start");
            Task.Delay(1000).Wait();
            map.TryAdd("xxx", 1);
            Debug.WriteLine("end");
        }
    }
}

