
namespace DwebBrowser.Helper;

public class ComparableWrapperBuilder<T>
{
    public Func<T, Dictionary<string, int>> GetScore { get; init; }
    public ComparableWrapperBuilder(Func<T, Dictionary<string, int>> getScore)
    {
        GetScore = getScore;
    }

    public ComparableWrapper<T> Build(T value) => new(value, GetScore);
}

public class ComparableWrapper<T> : IComparable<ComparableWrapper<T>>
{
    public T Value { get; init; }
    public Func<T, Dictionary<string, int>> GetScore { get; init; }

    public ComparableWrapper(T value, Func<T, Dictionary<string, int>> getScore)
    {
        Value = value;
        GetScore = getScore;
    }

    public static ComparableWrapperBuilder<T> Builder(Func<T, Dictionary<string, int>> getScore) => new(getScore);

    private Dictionary<string, int>? score = null;

    public Dictionary<string, int> Score => score ??= GetScore(Value);

    private HashSet<string> ScopeKeys => Score.Keys.ToHashSet();

    public int CompareTo(ComparableWrapper<T> other)
    {
        var aScore = score;
        var bScore = other.score;

        foreach (var key in ScopeKeys)
        {
            var aValue = aScore.GetValueOrDefault(key);
            var bValue = bScore.GetValueOrDefault(key);

            if (aValue != bValue)
            {
                return aValue - bValue;
            }
        }

        return 0;
    }
}

public static class EnumComparable
{
    public static int EnumToComparable<T>(T enumValue, List<T> enumList) => enumList.IndexOf(enumValue);

    public static List<int> EnumToComparable<T>(IEnumerable<T> enumValues, List<T> enumList)
    {
        var sorts = new List<int>();
        foreach (var value in enumValues)
        {
            sorts.Add(enumList.IndexOf(value));
        }

        return sorts.OrderBy(it => it).ToList();
    }
}

