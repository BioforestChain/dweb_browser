using System;
namespace ipc.extensions;

public static class TaskExtensions
{
    public static Task ForAwait(this Task? task)
    {
        return task ?? Task.CompletedTask;
    }
    public static Task<T> ForAwait<T>(this Task<T>? task, T defaultValue = default)
    {
        return task ?? Task.FromResult(defaultValue);
    }

}


