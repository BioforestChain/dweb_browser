using System;
namespace ipc.helper;

public class AdapterManager<T> where T: notnull
{
	private Dictionary<T, int> _adapterOrderMap = new Dictionary<T, int>();
	private IEnumerable<T> _orderAdapters = new List<T>();

	public IEnumerable<T> Adapters { get => _orderAdapters; }

	public Func<bool> Append(T adapter, int order = 0)
	{
		_adapterOrderMap[adapter] = order;

		_orderAdapters = from entry in _adapterOrderMap
						 orderby entry.Value ascending
						 select entry.Key;

		return () => Remove(adapter);
	}

	public bool Remove(T adapter) => _adapterOrderMap.Remove(adapter);
}

