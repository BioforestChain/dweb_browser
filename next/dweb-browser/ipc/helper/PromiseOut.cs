using System.Threading.Tasks;

namespace ipc.helper;

public class PromiseOut<T>
{
    private TaskCompletionSource<T> task = new TaskCompletionSource<T>();
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

