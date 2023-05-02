using System;
namespace DwebBrowser.HelperTests;

public class StateTests
{

    [Fact]
    public async Task UnitTypeTest()
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

}

