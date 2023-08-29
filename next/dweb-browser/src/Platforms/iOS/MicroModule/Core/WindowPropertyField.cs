
namespace DwebBrowser.MicroService.Core;

public class WindowPropertyField<T>
{
	public WindowPropertyKeys FieldKey { get; init; }
	public Type FieldType { get; init; }
	public bool IsOptional { get; init; }
	public T? InitValue { get; init; }
    public WindowPropertyField(WindowPropertyKeys fieldKey, Type fieldType, bool isOptional, T? initValue)
	{
		FieldKey = fieldKey;
        FieldType = fieldType;
        IsOptional = isOptional;
        InitValue = initValue;
    }
}

public class Required<T> : WindowPropertyField<T>
{
    public Required(WindowPropertyKeys fieldKey, Type fieldType, T initValue) : base(fieldKey, fieldType, false, initValue)
	{
	}

	public Observable.Observer ToObserve(Observable observer)
	{
		return observer.Observe(FieldKey.FieldName, InitValue!);
    }
}

public class Optional<T> : WindowPropertyField<T>
{
    public Optional(WindowPropertyKeys fieldKey, Type fieldType, T initValue) : base(fieldKey, fieldType, true, initValue)
	{
	}

	public Observable.Observer ToObserve(Observable observer)
	{
		return observer.Observe(FieldKey.FieldName, InitValue);
    }
}

