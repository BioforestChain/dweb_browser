using System.Diagnostics;
using System.Text.RegularExpressions;

namespace DwebBrowser.Helper;

public static class DebuggerExtensions
{
    public static string TabEnd(this string str, int minPadWidth = 4, int spaceMin = 1)
    {
        var padWidth = (int)Math.Max(Math.Ceiling((float)str.Length / 4) * 4, minPadWidth);
        if (padWidth == str.Length)
        {
            padWidth += 1;
        }
        return str.PadRight(padWidth, ' ');
    }
}
public partial class Debugger
{
    public static List<string> DebugScopes = new();
    public static List<string> DebugTags = new();

    readonly string scopePrefix;
    public Debugger(string scope)
    {
        //Math.Ceiling((float)scope.Length / 4) * 4;
        scopePrefix = scope.TabEnd(8) + "⇉ ";
    }

    public void Write(Func<string> style, string tag, string msg)
    {
        Debug.WriteLine(style() + scopePrefix + tag.TabEnd() + "┊ " + msg);
    }
    public void WriteIf(Func<string> style, string tag, string msg)
    {
        var _bool = false;

        // 过滤scope是否打印
        var scopeBool = DebugScopes.Count > 0 && DebugScopes.FindIndex(scopePrefix.StartsWith) > -1 || DebugScopes.Contains("*");

        // 过滤标签是否打印
        if (scopeBool && (DebugTags.Count > 0 && DebugTags.Contains(tag) || DebugTags.Contains("*")))
        {
            _bool = true;
        }

        Debug.WriteLineIf(_bool, style() + scopePrefix + tag.TabEnd() + "┊ " + msg);
    }
    public void Write(Func<string> style, string tag, string format, params object?[] args)
    {
        var myFormat = MyRegex().Replace(format, (match) =>
        {
            var index = match.Groups[1].Value;
            var hashCode = "";
            if (index.ToIntOrNull() is not null and int index_int)
            {
                if (args.GetValue(index_int) is not null and var arg)
                {
                    hashCode = "@" + arg.GetHashCode().ToString();
                }
            }
            return "{" + index + "}" + hashCode;
        });
        Write(style, tag, string.Format(myFormat, args));
    }
    public void WriteIf(Func<string> style, string tag, string format, params object?[] args)
    {
        var myFormat = MyRegex().Replace(format, (match) =>
        {
            var index = match.Groups[1].Value;
            var hashCode = "";
            if (index.ToIntOrNull() is not null and int index_int)
            {
                if (args.GetValue(index_int) is not null and var arg)
                {
                    hashCode = "@" + arg.GetHashCode().ToString();
                }
            }
            return "{" + index + "}" + hashCode;
        });
        WriteIf(style, tag, string.Format(myFormat, args));
    }

    static string LogStyle() => DateTime.Now.ToLongTimeString() + " 💙 ";
    public void Log(string tag, string msg)
    {
        WriteIf(LogStyle, tag, msg);
    }
    public void Log(string tag, string format, params object?[] args)
    {
        WriteIf(LogStyle, tag, format, args);
    }
    static string WarnStyle() => DateTime.Now.ToLongTimeString() + " 🧡 ";
    public void Warn(string tag, string msg)
    {
        WriteIf(WarnStyle, tag, msg);
    }
    public void Warn(string tag, string format, params object?[] args)
    {
        WriteIf(WarnStyle, tag, format, args);
    }

    static string ErrorStyle() => DateTime.Now.ToLongTimeString() + " 💔 ";
    public void Error(string tag, string msg)
    {
        Write(ErrorStyle, tag, msg);
    }
    public void Error(string tag, string format, params object?[] args)
    {
        Write(ErrorStyle, tag, format, args);
    }

    [GeneratedRegex("{(\\d):H}")]
    private static partial Regex MyRegex();
}

