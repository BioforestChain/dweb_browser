
using System.Runtime.CompilerServices;

namespace DwebBrowser.Helper;

public static class TaskExtensions
{
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public static async Task ForAwait(this Task? task)
    {
        if (task is Task t)
        {
            await t;
        }
    }
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public static async Task<T>? ForAwait<T>(this Task<T>? task, T defaultValue)
    {
        T? result = defaultValue;
        if (task is Task<T> t)
        {
            result = await t;
        }
        return result;
    }



    public static void Background(this Task task, Action<AggregateException>? onCatch = default)
    {
        task.ContinueWith(t =>
        {
            if (t.IsFaulted && t.Exception is not null)
            {
                onCatch?.Invoke(t.Exception);
            }
        });
    }

}


