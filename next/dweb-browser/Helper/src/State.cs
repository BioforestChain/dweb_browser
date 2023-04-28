using System;
namespace DwebBrowser.Helper;


public class State<T>
{
    [ThreadStatic]
    static List<dynamic> ObsStack = new();

    HashSet<dynamic> Deps = new();

    public bool AddDep(dynamic dep) => Deps.Add(dep);
    public bool RmDep(dynamic dep) => Deps.Remove(dep);

    Func<T> getter;
    Func<T, bool> setter;
    T cache;
    bool hasCache = false;
    public State(Func<T> getter, Func<T, bool> setter)
    {
        this.getter = getter;
        this.setter = setter;
    }

    public State(Func<T> getter) : this(getter, (_) => false)
    {
    }

    public State(T defaultValue) : this(() => defaultValue, (newValue) =>
    {
        if (defaultValue.Equals(newValue))
        {
            return false;
        }
        defaultValue = newValue;
        return true;
    })
    {
        cache = defaultValue;
        hasCache = true;
    }

    event Signal<T, T?> OnChange;

    public T Get()
    {
        if (hasCache)
        {
            return cache;
        }
        return UpdateCache();
    }

    public T UpdateCache()
    {
        /// 向自己的调用者加入依赖
        ((State<dynamic>?)ObsStack.LastOrDefault())?.AddDep(this);
        /// 自己也将作为调用者
        ObsStack.Add(this);
        /// 调用之前，清空自己的依赖，重新收集依赖
        Deps.Clear();
        try
        {
            var oldValue = cache;
            cache = getter();
            hasCache = true;
            _ = OnChange.Emit(cache, oldValue);
            return cache;
        }
        finally
        {
            /// 移除自己作为调用者的身份
            ObsStack.Remove(this);
        }
    }
    public void Set(T value)
    {
        if (setter(value))
        {
            /// 向自己的调用者发去通知
            foreach (var dep in Deps)
            {
                dep.UpdateCache();
            }
        }
    }
}