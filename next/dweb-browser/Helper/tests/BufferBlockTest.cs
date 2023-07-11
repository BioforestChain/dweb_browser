using System;
using System.Threading.Tasks.Dataflow;

namespace DwebBrowser.HelperTests;

public class BufferBlockTest
{
    [Fact]
    public async void BaseTest()
    {
        var channel = new BufferBlock<int>(new DataflowBlockOptions { BoundedCapacity = DataflowBlockOptions.Unbounded });

        var acc = 0;
        _ = Task.Factory.StartNew(async () =>
        {
            await foreach (var data in channel.ReceiveAllAsync())
            {
                Debug.WriteLine("got data: {0}", data);
                acc += data;
            }
        }, TaskCreationOptions.LongRunning).NoThrow();

        await Task.Delay(100);

        Assert.True(await channel.SendAsync(1));
        await Task.Delay(100);
        Assert.True(await channel.SendAsync(2));
        await Task.Delay(100);
        Assert.True(await channel.SendAsync(3));
        await Task.Delay(100);
        Assert.Equal(6, acc);
    }
}

