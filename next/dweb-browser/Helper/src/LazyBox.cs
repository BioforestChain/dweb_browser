using System;
using System.Diagnostics.CodeAnalysis;
using System.Runtime.CompilerServices;

namespace DwebBrowser.Helper;



public class LazyBox<T> //where T : notnull
{
    T? value = default;
    public LazyBox(T value)
    {
        this.value = value;
    }
    public LazyBox() { }
    bool inited = false;
    public bool HasValue => inited;

    public bool TryGetValue([MaybeNullWhen(false)] out T value)
    {
        if (HasValue)
        {
            value = this.value!;
            return true;
        }

        value = default;
        return false;
    }

    public T GetOrPut(Func<T> putter)
    {
        if (inited is false)
        {
            value = putter();
            inited = true;
        }
#pragma warning disable CS8603 // 可能返回 null 引用。
        return value;
#pragma warning restore CS8603 // 可能返回 null 引用。
    }
    public void SetValue(T value)
    {
        this.value = value;
        inited = true;
    }
    public void Reset()
    {
        if (inited)
        {
            value = default;
            inited = false;
        }
    }
}