using System;
using System.Diagnostics;

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
    public void Write(Func<string> style, string tag, string format, object? arg0)
    {
        Write(style, tag, String.Format(format, arg0));
    }
    public void Write(Func<string> style, string tag, string format, object? arg0, object? arg1)
    {
        Write(style, tag, String.Format(format, arg0, arg1));
    }
    public void Write(Func<string> style, string tag, string format, object? arg0, object? arg1, object? arg2)
    {
        Write(style, tag, String.Format(format, arg0, arg1, arg2));
    }
    public void Write(Func<string> style, string tag, string format, object? arg0, object? arg1, object? arg2, object? arg3)
    {
        Write(style, tag, String.Format(format, arg0, arg1, arg2, arg3));
    }
    public void Write(Func<string> style, string tag, string format, object? arg0, object? arg1, object? arg2, object? arg3, object? arg4)
    {
        Write(style, tag, String.Format(format, arg0, arg1, arg2, arg4));
    }

    static string LogStyle() => DateTime.Now.ToLongTimeString() + " 💙 ";
    public void Log(string tag, string msg)
    {
        Write(LogStyle, tag, msg);
    }
    public void Log(string tag, string format, object? arg0)
    {
        Write(LogStyle, tag, format, arg0);
    }
    public void Log(string tag, string format, object? arg0, object? arg1)
    {
        Write(LogStyle, tag, format, arg0, arg1);
    }
    public void Log(string tag, string format, object? arg0, object? arg1, object? arg2)
    {
        Write(LogStyle, tag, format, arg0, arg1, arg2);
    }
    public void Log(string tag, string format, object? arg0, object? arg1, object? arg2, object? arg3)
    {
        Write(LogStyle, tag, format, arg0, arg1, arg2, arg3);
    }
    public void Log(string tag, string format, object? arg0, object? arg1, object? arg2, object? arg3, object? arg4)
    {
        Write(LogStyle, tag, format, arg0, arg1, arg2, arg4);
    }

    static string WarnStyle() => DateTime.Now.ToLongTimeString() + " 🧡 ";
    public void Warn(string tag, string msg)
    {
        Write(WarnStyle, tag, msg);
    }
    public void Warn(string tag, string format, object? arg0)
    {
        Write(WarnStyle, tag, format, arg0);
    }
    public void Warn(string tag, string format, object? arg0, object? arg1)
    {
        Write(WarnStyle, tag, format, arg0, arg1);
    }
    public void Warn(string tag, string format, object? arg0, object? arg1, object? arg2)
    {
        Write(WarnStyle, tag, format, arg0, arg1, arg2);
    }
    public void Warn(string tag, string format, object? arg0, object? arg1, object? arg2, object? arg3)
    {
        Write(WarnStyle, tag, format, arg0, arg1, arg2, arg3);
    }
    public void Warn(string tag, string format, object? arg0, object? arg1, object? arg2, object? arg3, object? arg4)
    {
        Write(WarnStyle, tag, format, arg0, arg1, arg2, arg4);
    }

    static string ErrorStyle() => DateTime.Now.ToLongTimeString() + " 💔 ";
    public void Error(string tag, string msg)
    {
        Write(ErrorStyle, tag, msg);
    }
    public void Error(string tag, string format, object? arg0)
    {
        Write(ErrorStyle, tag, format, arg0);
    }
    public void Error(string tag, string format, object? arg0, object? arg1)
    {
        Write(ErrorStyle, tag, format, arg0, arg1);
    }
    public void Error(string tag, string format, object? arg0, object? arg1, object? arg2)
    {
        Write(ErrorStyle, tag, format, arg0, arg1, arg2);
    }
    public void Error(string tag, string format, object? arg0, object? arg1, object? arg2, object? arg3)
    {
        Write(ErrorStyle, tag, format, arg0, arg1, arg2, arg3);
    }
    public void Error(string tag, string format, object? arg0, object? arg1, object? arg2, object? arg3, object? arg4)
    {
        Write(ErrorStyle, tag, format, arg0, arg1, arg2, arg4);
    }
}

