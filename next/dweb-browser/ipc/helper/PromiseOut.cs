namespace ipc.helper;

public class PromiseOut<T>
{
    private TaskCompletionSource<T> task = new TaskCompletionSource<T>();
    public T? Value { get; set; }

    public void Resolve(T value)
    {
        Value = value;
        task.TrySetResult(value);
        IsResolved = true;
    }

    public void Reject(string msg)
    {
        task.TrySetException(new Exception(msg));
    }

    public bool IsResolved { get; set; } = false;
    public bool IsFinished
    {
        get { return new Lazy<bool>(new Func<bool>(() => task.Task.IsCompleted)).Value; }
    }

    public bool IsCanceled
    {
        get
        {
            return new Lazy<bool>(new Func<bool>(() =>
                task.Task.IsCanceled || source.IsCancellationRequested)).Value;
        }
    }

    public void Cancel() => source.Cancel();

    private CancellationTokenSource source = new CancellationTokenSource();

    public T WaitPromise() => task.Task.Result;

    public Task<T> WaitPromiseAsync() => task.Task.WaitAsync(source.Token);
}

