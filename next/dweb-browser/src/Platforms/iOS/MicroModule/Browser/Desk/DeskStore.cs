using System.Text.Json;

namespace DwebBrowser.MicroService.Browser.Desk;

sealed public class DeskStore : FileStore
{
    static readonly Debugger Console = new("DeskStore");
    public static DeskStore Instance = new("desk.browser.dweb");
    internal DeskStore(string name, StoreOptions options = default) : base(name, options)
    {
    }

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

    private LazyBox<HashSet<TaskApps>> STaskAppsSet = new();
    private HashSet<TaskApps> TaskAppsSet
    {
        get
        {
            var hashset = JsonSerializer.Deserialize<HashSet<TaskApps>>(Get("taskbar/apps",
                () => JsonSerializer.Serialize(new HashSet<TaskApps>())));

            if (hashset.Count == 0)
            {
                return STaskAppsSet.GetOrPut(() => new HashSet<TaskApps>());
            }

            return hashset;
        }
    }

    private void Save()
    {
        Set("taskbar/apps", JsonSerializer.Serialize(TaskAppsSet));
    }

    internal void Save(List<TaskApps> taskApps)
    {
        Set("taskbar/apps", JsonSerializer.Serialize(taskApps.ToHashSet()));
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

