using System;
using System.Collections;
using System.Collections.Specialized;
using System.Text;
using System.Web;
using static System.Runtime.InteropServices.JavaScript.JSType;

namespace DwebBrowser.Helper;

/// <summary>
/// 模拟JS的URL标准，能更加轻松地对Query进行修改
/// </summary>

public class URL
{

    UriBuilder _uriBuilder;
    UriBuilder uriBuilder
    {
        get
        {
            /// 强制读取一下 Query, 确保更新反应到 builder 上
            var _ = this.Query;
            return _uriBuilder;
        }
    }

    public URL(string uri)
    {
        _uriBuilder = new(uri);
    }
    public URL(Uri uri)
    {
        _uriBuilder = new(uri);
    }
    public Uri Uri
    {
        get => uriBuilder.Uri;
        set => _uriBuilder = new(value);
    }
    public string Scheme
    {
        get => uriBuilder.Scheme;
        set => uriBuilder.Scheme = value;
    }
    public string UserName
    {
        get => uriBuilder.UserName;
        set => uriBuilder.UserName = value;
    }
    public string Password
    {
        get => uriBuilder.Password;
        set => uriBuilder.Password = value;
    }
    public string Hostname
    {
        get => uriBuilder.Host;
        set => uriBuilder.Host = value;
    }
    public int Port
    {
        get => uriBuilder.Port;
        set => uriBuilder.Port = value;
    }
    private LazyBox<string> _fullHost => new();
    public string FullHost => _fullHost.GetOrPut(() => uriBuilder.Uri.GetFullAuthority());
    public string Path
    {
        get => Uri.AbsolutePath;
        set => uriBuilder.Path = value;
    }
    private LazyBox<URLSearchParams> _searchParams = new();
    public URLSearchParams SearchParams
    {
        get => _searchParams.GetOrPut(() => new URLSearchParams(_uriBuilder.Query));
    }
    public string Query
    {
        get
        {
            if (_searchParams.TryGetValue(out var searchParams))
            {
                if (searchParams.HasChanged) lock (searchParams)
                    {
                        _uriBuilder.Query = searchParams.ToString();
                        searchParams.HasChanged = false;
                    }
            }
            return _uriBuilder.Query;
        }
        set
        {
            if (_uriBuilder.Query == value)
            {
                return;
            }
            _uriBuilder.Query = value;

            /// 清除缓存
            _searchParams.Reset();
        }
    }

    public string PathAndQuery => Uri.PathAndQuery;
    public string Href => Uri.OriginalString;

    public URL SearchParamsSet(string? name, string? value)
    {
        SearchParams.Set(name, value);
        return this;
    }

    public override string ToString() => uriBuilder.ToString();
}


public class URLSearchParams : IEnumerable<KeyValuePair<string, string[]>>
{
    internal bool HasChanged = false;
    private NameValueCollection _qs;
    public URLSearchParams(string query) : this(HttpUtility.ParseQueryString(query)) { }
    public URLSearchParams(NameValueCollection qs)
    {
        this._qs = qs;
    }

    public URLSearchParams Add(string? name, string? value)
    {
        _qs.Add(name, value);
        HasChanged = true;
        return this;
    }
    public URLSearchParams Add(NameValueCollection qs)
    {
        _qs.Add(qs);
        HasChanged = true;
        return this;
    }
    public URLSearchParams Clear()
    {
        _qs.Clear();
        HasChanged = true;
        return this;
    }
    public URLSearchParams Set(string? name, string? value)
    {
        _qs.Set(name, value);
        HasChanged = true;
        return this;
    }
    public URLSearchParams Remove(string? name)
    {
        _qs.Remove(name);
        HasChanged = true;
        return this;
    }
    public string? this[string name] { get => _qs[name]; set => _qs[name] = value; }
    public string?[] AllKeys { get => _qs.AllKeys; }
    public string? Get(string name) => _qs.Get(name);

    static Func<string, string> DefaultOnNoFound = (name) => throw new FormatException("no found query params: " + name);
    public string ForceGet(string name, Func<string, string>? onNotFound = null) => _qs.Get(name) ?? (onNotFound ?? DefaultOnNoFound)(name);
    public string[]? GetValues(string name) => _qs.GetValues(name);

    public bool Has(string name) => _qs.AllKeys.Contains(name);
    public bool HasKeys() => _qs.HasKeys();
    public int Count => _qs.Count;

    public override string ToString() => _qs.ToString() ?? "";

    public IEnumerator<KeyValuePair<string, string[]>> GetEnumerator()
    {
        for (int i = 0, len = _qs.Count; i < len; i++)
        {
            if (_qs.GetKey(i) is not null and var key && _qs.GetValues(i) is not null and var value)
            {
                yield return new(key, value);
            }
        }
    }

    public Dictionary<string, string[]> ToDictionary()
    {
        var res = new Dictionary<string, string[]>();
        foreach (var (key, value) in this)
        {
            res.Add(key, value);
        }
        return res;
    }

    IEnumerator<KeyValuePair<string, string[]>> IEnumerable<KeyValuePair<string, string[]>>.GetEnumerator() => GetEnumerator();
    IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
}
