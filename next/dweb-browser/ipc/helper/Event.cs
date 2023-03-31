
namespace ipc.helper;

public delegate Task OnMessageHandler<T1, T2>(T1 ipcMessage, T2 ipc);
public delegate Task OnSingleMessageHandler<T>(T ipcMessage);
public delegate void OnSimpleMessageHandler();

public class Event<T1, T2>
{
	public event OnMessageHandler<T1, T2> OnMessage;

	public void Listen(OnMessageHandler<T1, T2> cb) => OnMessage += cb;

	public void Emit(T1 ipcMessage, T2 ipc)
	{
		if (OnMessage is null) return;
		foreach (OnMessageHandler<T1, T2> cb in OnMessage.GetInvocationList().Cast<OnMessageHandler<T1, T2>>())
		{
			cb(ipcMessage, ipc);
		}
	}

    public void Remove(OnMessageHandler<T1, T2> cb) => OnMessage -= cb;

	public void Clear()
	{
		if (OnMessage is null) return;
        foreach (OnMessageHandler<T1, T2> cb in OnMessage.GetInvocationList().Cast<OnMessageHandler<T1, T2>>())
        {
            Remove(cb);
        }
    }
}

public class SingleEvent<T>
{
    public event OnSingleMessageHandler<T> OnMessage;

    public void Listen(OnSingleMessageHandler<T> cb) => OnMessage += cb;

    public void Emit(T ipcMessage)
    {
        if (OnMessage is null) return;
        foreach (OnSingleMessageHandler<T> cb in OnMessage.GetInvocationList().Cast<OnSingleMessageHandler<T>>())
        {
            cb(ipcMessage);
        }
    }

    public void Remove(OnSingleMessageHandler<T> cb) => OnMessage -= cb;

    public void Clear()
    {
        if (OnMessage is null) return;
        foreach (OnSingleMessageHandler<T> cb in OnMessage.GetInvocationList().Cast<OnSingleMessageHandler<T>>())
        {
            Remove(cb);
        }
    }
}

public class SimpleEvent
{
	public event OnSimpleMessageHandler OnMessage;

	public void Listen(OnSimpleMessageHandler cb) => OnMessage += cb;

	public void Emit()
	{
        if (OnMessage is null) return;
        foreach (OnSimpleMessageHandler cb in OnMessage.GetInvocationList().Cast<OnSimpleMessageHandler>())
        {
            cb();
        }
    }

    public void Remove(OnSimpleMessageHandler cb) => OnMessage -= cb;

    public void Clear()
    {
        if (OnMessage is null) return;
        foreach (OnSimpleMessageHandler cb in OnMessage.GetInvocationList().Cast<OnSimpleMessageHandler>())
        {
            Remove(cb);
        }
    }
}
