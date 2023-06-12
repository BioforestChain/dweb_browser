namespace DwebBrowser.Helper;

public class Semver: IComparable<Semver>
{
    public int Major { get; init; }
    public int Minor { get; init; }
    public int Patch { get; init; }

    public Semver(int major, int minor, int patch)
    {
        Major = major;
        Minor = minor;
        Patch = patch;
    }

    public Semver(string version)
    {
        var versionArr = version.Split(".");

        if (versionArr.Length == 3)
        {
            Major = versionArr[0].ToIntOrNull() ?? 0;
            Minor = versionArr[1].ToIntOrNull() ?? 0;
            Patch = versionArr[2].ToIntOrNull() ?? 0;
        }
    }

    public int CompareTo(Semver other)
    {
        if (Major != other.Major)
            return Major - other.Major;
        if (Minor != other.Minor)
            return Minor - other.Minor;
        return Patch - other.Patch;
    }

    public override string ToString() => $"{Major}.{Minor}.{Patch}";
}
