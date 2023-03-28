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

    public PromiseOut()
    {
        _isCanceled = new Lazy<bool>(() =>
            task.Task.IsCanceled || ((_token is not null) && _token!.Value.IsCancellationRequested), true);

        _isFinished = new Lazy<bool>(() => task.Task.IsCompleted, true);
    }

    public bool IsResolved { get; set; } = false;

    private Lazy<bool> _isFinished;
    public bool IsFinished
    {
        get { return _isFinished.Value; }
    }

    private Lazy<bool> _isCanceled;
    public bool IsCanceled
    {
        get
        {
            return _isCanceled.Value;
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

