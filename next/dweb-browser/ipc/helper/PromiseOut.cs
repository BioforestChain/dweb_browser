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
                task.Task.IsCanceled || ((_token is not null).Let(_ =>
                    _token!.Value.IsCancellationRequested)))).Value;
        }
    }

    public void Cancel() => task.TrySetCanceled();

    private CancellationToken? _token { get; set; }

    public T WaitPromise() => task.Task.Result;

    public Task<T> WaitPromiseAsync(CancellationToken token)
    {
        _token = token;
        return task.Task.WaitAsync(_token!.Value);
    }
}

