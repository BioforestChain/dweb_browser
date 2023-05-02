using System;
namespace DwebBrowser.Helper;


static class StateShared
{
    internal static List<dynamic> ObsStack = new();
}
public class State<T>
{

    /// <summary>
    /// 我的依赖：当依赖更新，我需要重新执行更新
    /// </summary>
    HashSet<dynamic> _Deps = new();
    public IReadOnlySet<dynamic> Deps { get => _Deps; }
    /// <summary>
    /// 我的引用：当我更新，我需要去通知它们执行更新
    /// </summary>
    HashSet<dynamic> _Refs = new();
    public IReadOnlySet<dynamic> Refs { get => _Refs; }

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
        lock (StateShared.ObsStack)
        {
            var caller = StateShared.ObsStack.LastOrDefault();
            if (caller != null)
            {
                /// 将调用者存储到自己的依赖中
                caller.AddDep(this);
            }
        }


        if (!hasCache)
        {
            /// 自己也将作为调用者
            /// 
            lock (StateShared.ObsStack)
            {
                StateShared.ObsStack.Add(this);
            }
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
                lock (StateShared.ObsStack)
                {
                    StateShared.ObsStack.Remove(this);
                }
            }
        }
        return cache;
    }

    public bool Set(T value, bool force = false)
    {
        if (setter(value) || force)
        {
            hasCache = false;
            /// 向自己的调用者发去通知
            foreach (var @ref in _Refs.ToArray())
            {
                @ref.hasCache = false;
                @ref.Get();
            }
            return true;
        }
        return false;
    }
    public bool Update(Func<T?, T> updater, bool force = true)
    {
        return Set(updater(cache), force);
    }
    public bool Update(Func<T?, bool> updater)
    {
        return Set(cache, updater(cache));
    }
    public bool Update(Action<T?> updater, bool force = true)
    {
        updater(cache);
        return Set(cache, force);
    }
    public async IAsyncEnumerable<T> ToStream()
    {
        PromiseOut<T>? waitter = null;
        Mutex locker = new();

        Queue<T> cacheList = new();
        Signal<T, T?> onChange = async (value, old, _) =>
        {

            locker.WaitOne();

            try
            {
                if (waitter != null)
                {

                    waitter.Resolve(value);
                    waitter = null;
                    return;
                }

                cacheList.Append(value);
            }
            finally
            {
                locker.ReleaseMutex();
            }
        };
        try
        {
            OnChange += onChange;

            yield return Get();
            while (true)
            {

                while (true)
                {
                    bool success;
                    T? item;
                    locker.WaitOne();
                    try
                    {
                        success = cacheList.TryDequeue(out item);
                    }
                    finally
                    {
                        locker.ReleaseMutex();
                    }
                    if (!success)
                    {
                        break;
                    }
                    yield return item!;
                }

                locker.WaitOne();
                PromiseOut<T> w;
                waitter = w = new();
                locker.ReleaseMutex();
                yield return await w.WaitPromiseAsync();
            }
        }
        finally
        {
            OnChange -= onChange;
            locker.Dispose();
        }
    }
}