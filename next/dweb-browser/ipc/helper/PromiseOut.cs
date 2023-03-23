namespace ipc.helper;

public class PromiseOut<T>: TaskCompletionSource<T>
{
	public T? Value { get; set; }

	public PromiseOut()
	{
		_isFinished = new Lazy<bool>(new Func<bool>(() => this.Task.IsCompleted));
		_isCanceled = new Lazy<bool>(new Func<bool>(() => this.Task.IsCanceled));
	}

	public void Resolve(T value)
	{
		Value = value;
		TrySetResult(value);
		IsResolved = true;
	}

	public void Reject(string msg)
	{
		TrySetException(new Exception(msg));
	}

	public bool IsResolved { get; set; } = false;
	public Lazy<bool> _isFinished { get; set; }
	public bool IsFinished
	{
		get { return _isFinished.Value; }
	}

	public Lazy<bool> _isCanceled { get; set; }
	public bool IsCanceled
	{
		get { return _isCanceled.Value; }
	}

	public void Cancel() => source.Cancel();

    private CancellationTokenSource source = new CancellationTokenSource();

	public T WaitPromise() => this.Task.Result;

    public async Task<T> WaitPromiseAsync() => await this.Task.WaitAsync(source.Token);
}

