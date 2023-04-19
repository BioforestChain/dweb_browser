
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

    // Kotlin: fun <T> T.also(block: (T) -> Unit): T
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public static T Also<T>(this T self, Action<T> block)
    {
        block(self);
        return self;
    }

    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public static T SaveTo<T>(this T self, ref T to)
    {
        to = self;
        return self;
    }

}

