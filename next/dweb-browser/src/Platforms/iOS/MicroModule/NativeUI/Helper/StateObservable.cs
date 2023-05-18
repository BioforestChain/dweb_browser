using System.Collections;

#nullable enable

namespace DwebBrowser.Helper;

public class StateObservable<T>
{
    private Func<string> _getStateJson;

    public StateObservable(Func<string> getStateJson)
    {
        _getStateJson = getStateJson;
    }

    private Dictionary<Ipc, Signal> _observeIpcMap = new();

    private event Signal? _onChange;
    public Task EmitAsync() => (_onChange?.Emit()).ForAwait();

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

        return unit;
    }

    public bool StopObserve(Ipc ipc)
    {
        if (_observeIpcMap.Remove(ipc, out var onChange))
        {
            _onChange -= onChange;
            return true;
        }

        return false;
    }
}
