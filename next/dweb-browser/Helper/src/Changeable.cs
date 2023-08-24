namespace DwebBrowser.Helper;

public class Changeable<T>
{
    private T Self { get; init; }
    public Listener<T> Listener = Listener<T>.New();

    public Changeable(T self)
    {
        Self = self;
        Watch = new(Listener, self);
    }

    public Watcher Watch { get; init; }

    public record Watcher(Listener<T> Listener, T Arg)
    {
        public Task AddWatch(Signal<T> cb) => Listener.AddListenerAndEmitFirst(Arg, cb);
    }

    public Task EmitChangeAsync() => Listener.Emit(Self);

    public void EmitChange()
    {
        _ = EmitChangeAsync();
    }

    /// <summary>
    /// 这里一定要判断size，因为 CoroutineScope 可能已经被 cancel 了，所以只要外部代码正确 onDispose 移除监听，那么这里就不会引发问题
    /// 否则如果listener的size不为空，导致执行了runBlockingCatching带来异常，就说明代码有生命周期问题
    /// </summary>
    public void EmitChangeBackground(T? changes)
    {
        changes ??= Self;

        if (Listener.Size > 0)
        {
            _ = Listener.Emit(changes);
        }
    }
}

