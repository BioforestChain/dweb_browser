using System;
namespace DwebBrowser.Helper;


public class State<T>
{
    [ThreadStatic]
    static List<dynamic> ObsStack = new();

    /// <summary>
    /// 我的依赖：当依赖更新，我需要重新执行更新
    /// </summary>
    HashSet<dynamic> _Deps = new();
    public HashSet<dynamic> Deps { get => _Deps; }
    /// <summary>
    /// 我的引用：当我更新，我需要去通知它们执行更新
    /// </summary>
    HashSet<dynamic> _Refs = new();
    public HashSet<dynamic> Refs { get => _Refs; }

    public bool AddDep(dynamic dep)
    {
        var success = _Deps.Add(dep);
        if (success)
        {
            dep.AddRef((dynamic)this);
        }
        return success;
    }
    public bool AddRef(dynamic @ref)
    {
        var success = _Refs.Add(@ref);
        if (success)
        {
            @ref.AddDep((dynamic)this);
        }
        return success;
    }
    public bool RmDep(dynamic dep)
    {
        var success = _Deps.Remove(dep);
        if (success)
        {
            dep.RmRef((dynamic)this);
        }
        return success;
    }
    public bool RmRef(dynamic @ref)
    {
        var success = _Refs.Remove(@ref);
        if (success)
        {
            @ref.RmDep((dynamic)this);
        }
        return success;
    }
    public void ClearDeps()
    {
        foreach (var dep in _Deps.ToArray())
        {
            RmDep(dep);
        }
    }

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

    public event Signal<T, T?> OnChange;

    public T Get()
    {
        var caller = ObsStack.LastOrDefault();
        if (caller != null)
        {
            /// 将调用者存储到自己的依赖中
            caller.AddDep(this);
        }

        if (!hasCache)
        {
            /// 自己也将作为调用者
            ObsStack.Add(this);
            /// 调用之前，清空自己的依赖，重新收集依赖
            ClearDeps();
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
        return cache;
    }

    T UpdateCache()
    {
        /// 自己也将作为调用者
        ObsStack.Add(this);
        /// 调用之前，清空自己的依赖，重新收集依赖
        _Deps.Clear();
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
            hasCache = false;
            /// 向自己的调用者发去通知
            foreach (var @ref in _Refs.ToArray())
            {
                @ref.hasCache = false;
                @ref.Get();
            }
        }
    }
}