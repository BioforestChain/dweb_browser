using System;
using System.Diagnostics;
using System.IO;
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
public class Debugger
{
    string scopePrefix;
    public Debugger(string scope)
    {
        //Math.Ceiling((float)scope.Length / 4) * 4;
        this.scopePrefix = scope.TabEnd(8) + "⇉ ";
    }
    public void Write(Func<string> style, string tag, string msg)
    {
        Debug.WriteLine(style() + scopePrefix + tag.TabEnd() + "┊ " + msg);
    }
    public void Write(Func<string> style, string tag, string format, params object?[] args)
    {
        var myFormat = Regex.Replace(format, @"{(\d):H}", (match) =>
        {
            var index = match.Groups[1].Value;
            var index_int = index.ToIntOrNull();
            var hashCode = "";
            if (index_int != null)
            {
                var arg = args.GetValue((int)index_int);
                if (arg != null)
                {
                    hashCode = "@" + arg.GetHashCode().ToString();
                }
            }
            return "{" + index + "}" + hashCode;
        });
        Write(style, tag, String.Format(myFormat, args));
    }

    static string LogStyle() => DateTime.Now.ToLongTimeString() + " 💙 ";
    public void Log(string tag, string msg)
    {
        Write(LogStyle, tag, msg);
    }
    public void Log(string tag, string format, params object?[] args)
    {
        Write(LogStyle, tag, format, args);
    }
    static string WarnStyle() => DateTime.Now.ToLongTimeString() + " 🧡 ";
    public void Warn(string tag, string msg)
    {
        Write(WarnStyle, tag, msg);
    }
    public void Warn(string tag, string format, params object?[] args)
    {
        Write(WarnStyle, tag, format, args);
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
}

