using System;
using DwebBrowser.Helper;

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

    class StatusBar
    {
        public bool Visible { get; set; }
        public bool Overlay { get; set; }
    }

    [Fact]
    public async Task OnChange_Test()
    {
        var visible = new State<bool>(false);
        var overlay = new State<bool>(false);
        //var statusBar = new StatusBar()
        //{
        //    Visible = visible.Get(),
        //    Overlay = overlay.Get()
        //};

        var statusBarState = new State<StatusBar>(() => new StatusBar()
        {
            Visible = visible.Get(),
            Overlay = overlay.Get()
        });

        statusBarState.OnChange += async (value, oldValue, _) =>
        {
            Debug.WriteLine("onChange visible value: " + value.Visible);
            Debug.WriteLine("onChange visible oldValue: " + oldValue?.Visible);
            Debug.WriteLine("onChange overlay value: " + value.Overlay);
            Debug.WriteLine("onChange overlay oldValue: " + oldValue?.Overlay);
        };

        visible.Set(true);
        var newStatusBar = statusBarState.Get();
        Debug.WriteLine("visible: {0}, overlay: {1}", newStatusBar.Visible, newStatusBar.Overlay);
    }

    struct NavigationBar
    {
        public bool Visible { get; set; }
        public bool Overlay { get; set; }
    }

    #region 用于测试是否update方法仅适用于引用类型

    [Fact]
    public async Task Update_RefType_Test()
    {
        var statusBarState = new State<StatusBar>(new StatusBar() { Visible = false, Overlay = false });
        statusBarState.Update(cache => { cache!.Visible = true; });
        Assert.True(statusBarState.Get().Visible);
    }

    [Fact]
    public async Task Update_ValueType_Test()
    {
        var navigationBarState = new State<NavigationBar>(new NavigationBar() { Visible = false, Overlay = false  });
        navigationBarState.Update(cache => { cache!.Visible = true; });
        Assert.False(navigationBarState.Get().Visible);
    }

    #endregion
}

