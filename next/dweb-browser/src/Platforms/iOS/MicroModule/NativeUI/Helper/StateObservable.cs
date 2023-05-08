using System.Collections;

#nullable enable

namespace DwebBrowser.Helper;

public class StateObservable<T>
{
    private State<T> _observe;
    private Func<string> _getStateJson;

    public StateObservable(State<T> observe, Func<string> getStateJson)
    {
        _observe = observe;
        _getStateJson = getStateJson;
        _onObserveChange = async (_, _, _) =>
        {
            await (_onChange?.Emit()).ForAwait();
        };
    }

    private Dictionary<Ipc, Signal> _observeIpcMap = new();

    private event Signal? _onChange;
    public Task EmitAsync() => (_onChange?.Emit()).ForAwait();
    private Signal<T, T?> _onObserveChange;

    public Unit StartObserve(Ipc ipc)
    {
        _observeIpcMap.GetValueOrPut(ipc, () =>
        {
            Signal onChange = async (_) =>
            {
                await ipc.PostMessageAsync(IpcEvent.FromUtf8("observe", _getStateJson()));
            };

            _onChange += onChange;
            return onChange;
        });
        _observe.OnChange += _onObserveChange;

        return unit;
    }

    public bool StopObserve(Ipc ipc)
    {
        if (_observeIpcMap.Remove(ipc, out var onChange))
        {
            _observe.OnChange -= _onObserveChange;
            _onChange -= onChange;
            return true;
        }

        return false;
    }
}
