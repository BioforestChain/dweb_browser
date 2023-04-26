using System;
using System.Runtime.CompilerServices;

namespace DwebBrowser.Helper;



public class LazyBox<T> where T : notnull
{
    T? value = default;
    public LazyBox(T value)
    {
        this.value = value;
    }
    public LazyBox() { }
    bool inited = false;

    public T GetOrPut(Func<T> putter)
    {
        if (inited is false || value is null)
        {
            inited = true;
            value = putter();
        }
        return value;
    }
}