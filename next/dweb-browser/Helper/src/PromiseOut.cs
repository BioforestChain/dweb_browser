
namespace DwebBrowser.Helper;

public class PromiseOut<T>
{
    public static PromiseOut<T> StaticResolve(T value) => new PromiseOut<T>().Also(it => it.Resolve(value));
    public static PromiseOut<T> StaticReject(string msg) => new PromiseOut<T>().Also(it => it.Reject(msg));

    /// <summary>
    /// 使用 TaskCreationOptions.RunContinuationsAsynchronously，这会将延续分派到线程池上，而不是内联执行。
    /// </summary>
    /// <seealso cref="https://github.com/davidfowl/AspNetCoreDiagnosticScenarios/blob/master/AsyncGuidance.md#always-create-taskcompletionsourcet-with-taskcreationoptionsruncontinuationsasynchronously"/>
    private readonly TaskCompletionSource<T> task = new(TaskCreationOptions.RunContinuationsAsynchronously);
    public T? Value { get; set; }

    public void Resolve(T value)
    {
        if (task.TrySetResult(value))
        {
            Value = value;
        }

    }

    public void Reject(string msg)
    {
        task.TrySetException(new Exception(msg));
    }

    public bool IsResolved { get { lock (task) { return task.Task.IsCompletedSuccessfully; } } }

    public bool IsFinished { get { lock (task) { return task.Task.IsCompleted; } } }

    public bool IsCanceled { get { lock (task) { return task.Task.IsCanceled; } } }

    //public T WaitPromise() => task.Task.Result;
    public Task<T> WaitPromiseAsync() => task.Task.WaitAsync(CancellationToken.None);
}

