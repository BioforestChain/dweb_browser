using System;
namespace DwebBrowser.HelperTests;

public class StateTests
{

    [Fact]
    public async Task BaseTest()
    {
        var state1 = new State<int>(1);
        var state2 = new State<int>(1);
        var state3 = new State<int>(() => state1.Get() + state2.Get());

        Assert.Equal(2, state3.Get());
        state3.OnChange += async (value, oldValue, _) =>
        {
            Debug.WriteLine("state3:{0}", value);
        };
        state2.Set(2);
        Assert.Equal(3, state3.Get());
        Assert.Equal(1, state1.Refs.Count);
        Assert.Equal(1, state2.Refs.Count);
        Assert.Equal(2, state3.Deps.Count);

        await Task.Delay(100);
    }

    [Fact]
    public async Task ComplexTest()
    {
        var state1 = new State<bool>(false);
        var state2 = new State<int>(2);
        var state3 = new State<int>(1);
        var state4 = new State<int>(() =>
        {
            if (state1.Get())
            {
                return state2.Get();
            }
            else
            {
                return state3.Get();
            }
        });

        Assert.Equal(1, state4.Get());
        state4.OnChange += async (value, oldValue, _) =>
        {
            Debug.WriteLine("state4:{0}", value);
        };
        state1.Set(true);
        Assert.Equal(2, state4.Get());

        await Task.Delay(100);
    }

}

