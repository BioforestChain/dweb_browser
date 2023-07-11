namespace DwebBrowser.Helper;

public class AdapterManager<T> where T: notnull
{
	private readonly Dictionary<T, int> _adapterOrderMap = new();
	private IEnumerable<T> _orderAdapters = new List<T>();

	public IEnumerable<T> Adapters { get => _orderAdapters; }

	/// <summary>
	///	
	/// </summary>
	/// <param name="adapter"></param>
	/// <param name="order"></param> 越大排序越靠后
	/// <returns></returns>
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

