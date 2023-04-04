namespace DwebBrowser.HelperTests;

public class PromiseOutTest 
{
    public PromiseOutTest(ITestOutputHelper output) 
    { }

    [Fact]
    [Trait("Helper", "PromiseOut")]
    public async void PromiseOut_Resolve_ReturnsSuccess()
    {
        var stopwatch = new Stopwatch();
        stopwatch.Start();
        

        Debug.WriteLine($"{stopwatch.Elapsed} start");
        var startTime = DateTime.Now;
        Debug.WriteLine($"{stopwatch.Elapsed} start: {startTime}");
        var po = new PromiseOut<bool>();
        Debug.WriteLine($"{stopwatch.Elapsed} Task start: {DateTime.Now}");
        _ = Task.Run(() => SleepResolve(1000, po));
        Debug.WriteLine($"{stopwatch.Elapsed} Task end: {DateTime.Now}");
        var b = await po.WaitPromiseAsync();
        Debug.WriteLine($"{stopwatch.Elapsed} resolve value: {b.ToString()}");
        var endTime = DateTime.Now;
        Debug.WriteLine($"{stopwatch.Elapsed} end: {endTime}");
        Assert.Equal(1, (endTime - startTime).Seconds);
    }

    internal static void SleepResolve(int s, PromiseOut<bool> po)
    {
        Debug.WriteLine($"sleepAsync start: {DateTime.Now}");
        Thread.Sleep(s);
        po.Resolve(true);
        Debug.WriteLine($"sleepAsync end: {DateTime.Now}");
    }

    [Fact]
    [Trait("Helper", "PromiseOut")]
    public async void PromiseOut_Reject_ReturnsFailure()
    {
        var po = new PromiseOut<bool>();
        try
        {
            _ = Task.Run(() => SleepReject(1000, po, "QAQ"));
            await po.WaitPromiseAsync();
        }
        catch (Exception e)
        {
            Debug.WriteLine(e.Message);
            Assert.Contains("QAQ", e.Message);
        }
    }

    internal static void SleepReject(int s, PromiseOut<bool> po, string message)
    {
        Thread.Sleep(s);
        po.Reject(message);
    }

    [Fact]
    [Trait("Helper", "PromiseOut")]
    public async void PromiseOut_MultiAwait_ReturnsResolved()
    {
        var po = new PromiseOut<bool>();

        var startTime = DateTime.Now;

        _ = Task.Run(() => SleepResolve(1000, po));
        _ = Task.Run(() => SleepResolve(1000, po));
        _ = Task.Run(() => SleepWait(po, 1));
        _ = Task.Run(() => SleepWait(po, 2));

        Debug.WriteLine("start wait 3");
        await po.WaitPromiseAsync();
        Debug.WriteLine("resolved 3");

        Assert.Equal(1, (DateTime.Now - startTime).Seconds);
    }

    internal static async void SleepWait(PromiseOut<bool> po, int sort)
    {
        Debug.WriteLine($"start wait {sort}");
        await po.WaitPromiseAsync();
        Debug.WriteLine($"resolve {sort}");
    }

    [Fact]
    [Trait("Helper", "PromiseOut")]
    public async void PromiseOut_Bench_ReturnsAtomicInteger()
    {
        var times = 10000;
        long result1 = 0, result2 = 0;

        for (int i = 0; i < times; i++)
        {
            var po = new PromiseOut<bool>();
            _ = Task.Run(async () =>
            {
                await Task.Delay(100);
                Interlocked.Increment(ref result1);
                po.Resolve(true);
            });
            _ = Task.Run(async () =>
            {
                await po.WaitPromiseAsync();
                Interlocked.Increment(ref result2);
            });
        }

        while (Interlocked.Read(ref result2) < times)
        {
            await Task.Delay(200);
            Debug.WriteLine($"times result1: {Interlocked.Read(ref result1)} result2: {Interlocked.Read(ref result2)}");
        }

        Assert.Equal(Interlocked.Read(ref result1), Interlocked.Read(ref result2));
    }
}

