namespace DwebBrowser.MicroService.Browser.Desk;

public class DeskStore<V, T> : FileStore<V, T> where V : IEnumerable<T>
{
    internal DeskStore(string name = "desk.browser.dweb", StoreOptions options = default) : base(name, options)
    { }
}

sealed public class DeskAppsStore : DeskStore<Dictionary<Mmid, DeskAppsStore.AppOrder>, KeyValuePair<Mmid, DeskAppsStore.AppOrder>>
{
    internal DeskAppsStore() : base()
    {
        AppOrders = Get("desktop/orders", () => new Dictionary<string, AppOrder>());
    }

    public static readonly DeskAppsStore Instance = new();

    public Dictionary<Mmid, AppOrder> AppOrders { get; init; }

    public record AppOrder(int Order);
}

sealed public class TaskAppsStore : DeskStore<HashSet<TaskAppsStore.TaskApps>, TaskAppsStore.TaskApps>
{
    internal string Key = "taskbar/apps";

    public static readonly TaskAppsStore Instance = new();

    internal TaskAppsStore() : base()
    {
        TaskAppsSet = Get(Key, () => new HashSet<TaskApps>());
    }

    public HashSet<TaskApps> TaskAppsSet { get; init; }

    public record TaskApps(Mmid Mmid, long Timestamp) : IEquatable<TaskApps>
    {
        public virtual bool Equals(TaskApps? other)
        {
            return GetHashCode() == other?.GetHashCode();
        }

        public override int GetHashCode()
        {
            return Mmid.GetHashCode();
        }
    }

    private void Save()
    {
        Set(Key, TaskAppsSet);
    }

    internal void Save(List<TaskApps> taskApps)
    {
        Set(Key, taskApps.ToHashSet());
    }

    internal bool Upsert(Mmid mmid)
    {
        if (Contains(mmid))
        {
            return true;
        }

        TaskAppsSet.Add(new TaskApps(mmid, DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()));

        Save();
        return true;
    }

    internal bool Contains(Mmid mmid)
    {
        return TaskAppsSet.Contains(new TaskApps(mmid, 0));
    }

    internal bool Remove(Mmid mmid)
    {
        if (Contains(mmid))
        {
            TaskAppsSet.Remove(new TaskApps(mmid, 0));
            Save();
            return true;
        }

        return false;
    }

    internal List<TaskApps> All() => TaskAppsSet.OrderByDescending(it => it.Timestamp).ToList();
}



