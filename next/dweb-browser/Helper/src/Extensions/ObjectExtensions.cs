
using System.Runtime.CompilerServices;

namespace DwebBrowser.Helper;

public static class ObjectExtensions
{
    // Kotlin: fun <T, R> T.let(block: (T) -> R): R
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public static R Let<T, R>(this T self, Func<T, R> block)
    {
        return block(self);
    }

    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public static Unit Util<T>(this T? self)
    {
        return Unit.Default;
    }

    // Kotlin: fun <T> T.also(block: (T) -> Unit): T
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public static T Also<T>(this T self, Action<T> block)
    {
        block(self);
        return self;
    }
    public static async Task<T> AlsoAsync<T>(this T self, Func<T, Task> block)
    {
        await block(self);
        return self;
    }

    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public static T SaveTo<T>(this T self, ref T to)
    {
        to = self;
        return self;
    }

    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public static T Try<T>(this T self, Func<T, T> tryFun, Func<T, Exception, T>? catchFun = null)
    {
        try
        {
            return tryFun(self);
        }
        catch (Exception err)
        {
            Func<T, Exception, T> internalCatchFun = catchFun ?? ((self, _) => self);
            return internalCatchFun(self, err);
        }
    }

    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public static R TryReturn<R, T>(this T self, Func<T, R> tryFun, Func<T, Exception, R> catchFun)
    {
        try
        {
            return tryFun(self);
        }
        catch (Exception err)
        {
            Func<T, Exception, R> internalCatchFun = catchFun;
            return internalCatchFun(self, err);
        }
    }

}

