
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
    public static async Task<T> ForAwait<T>(this Task<T>? task, T defaultValue)
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

    public static async Task NoThrow(this Task task)
    {
        await new NoThrowAwaiter(task);
    }
}

/// <summary>
/// 允许以一种不抛出任何异常的方式等待一个Task任务完成
/// </summary>
/// <seealso cref="https://github.com/dotnet/aspnetcore/blob/main/src/SignalR/common/Http.Connections/src/Internal/TaskExtensions.cs"/>
internal readonly struct NoThrowAwaiter : ICriticalNotifyCompletion
{
    static Debugger Console = new("NoThrowAwaiter");
    private readonly Task _task;
    public NoThrowAwaiter(Task task) { _task = task; }
    public NoThrowAwaiter GetAwaiter() => this;
    public bool IsCompleted => _task.IsCompleted;
    // Observe exception
    public void GetResult()
    {
        _task.Exception?.Flatten().Handle(ex =>
        {
            Console.Error("Handle", "{0}", ex);
            return true;
        });
    }
    public void OnCompleted(Action continuation) => _task.GetAwaiter().OnCompleted(continuation);
    public void UnsafeOnCompleted(Action continuation) => _task.GetAwaiter().UnsafeOnCompleted(continuation);
}