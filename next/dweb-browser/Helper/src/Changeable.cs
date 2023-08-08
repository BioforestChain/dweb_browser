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
}

