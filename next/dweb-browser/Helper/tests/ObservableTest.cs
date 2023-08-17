using System.Text.Json;

namespace DwebBrowser.HelperTests;

public class ObservableTest
{
    public class WindowState
    {
        public Observable observable = new();

        public WindowState()
        {
            a = observable.Observe("A", false);
            b = observable.Observe("B", new Base() { C = 1, D = false });
        }

        Observable.Observer a { get; init; }
        public bool A
        {
            get => a.Get();
            set => a.Set(value);
        }

        public record class Base
        {
            public int C { get; set; }
            public bool D { get; set; }
        }

        Observable.Observer b { get; init; }
        public Base B
        {
            get => b.Get();
            set => b.Set(value);
        }
    }

    [Fact]
    public void ObservableSimpleTest()
    {
        var windowState = new WindowState();

        windowState.observable.Listener.OnListener += async (change, _) =>
        {
            Debug.WriteLine(JsonSerializer.Serialize(windowState));
        };

        Debug.WriteLine(windowState.A);
        windowState.A = true;
        Debug.WriteLine(windowState.A);
    }

    [Fact]
    public void ObservableClassTest()
    {
        var windowState = new WindowState();

        windowState.observable.Listener.OnListener += async (change, _) =>
        {
            Debug.WriteLine($"2: {0}", JsonSerializer.Serialize(windowState));
        };

        Debug.WriteLine($"1: {0}", JsonSerializer.Serialize(windowState));
        windowState.B = windowState.B with { C = 2 };
        Debug.WriteLine($"3: {0}", JsonSerializer.Serialize(windowState));
        windowState.A = true;
    }

    [Fact]
    public void ObservableClassTestFail()
    {
        var windowState = new WindowState();

        windowState.observable.Listener.OnListener += async (change, _) =>
        {
            // 没触发
            Debug.WriteLine($"2: {0}", JsonSerializer.Serialize(windowState));
        };

        Debug.WriteLine($"1: {0}", JsonSerializer.Serialize(windowState));
        windowState.B.C = 2;
        Debug.WriteLine($"3: {0}", JsonSerializer.Serialize(windowState));
    }
}
