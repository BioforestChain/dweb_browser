using System.Web;
namespace DwebBrowser.Helper;

public static class UriExtension
{
    public static Uri AppendQuery(this Uri self, string name, string value)
    {
        var url = new UriBuilder(self.AbsoluteUri);
        var query = HttpUtility.ParseQueryString(url.Query);
        query[name] = value;
        url.Query = query.ToString();
        return url.Uri;
    }

    public static string? GetQuery(this Uri self, string name)
    {
        var url = new UriBuilder(self.AbsoluteUri);
        var query = HttpUtility.ParseQueryString(url.Query);
        return query[name];
    }

    public static string GetFullAuthority(this Uri self, string hostOrAuthority)
    {
        var authority1 = hostOrAuthority;

        if (!authority1.Contains(':'))
        {
            if (self.Scheme == "http")
            {
                authority1 += ":80";
            }
            else if (self.Scheme == "https")
            {
                authority1 += ":443";
            }
        }

        return authority1;
    }

    public static Uri SetSchema(this Uri self, string schema) =>
        new(self.AbsoluteUri.Replace(self.Scheme, schema));


    public static Uri SetAuthority(this Uri self, string authority) =>
        new(self.AbsoluteUri.Replace(self.Authority, authority));

    public static Uri Path(this Uri self, string path) =>
        new($"{self.AbsoluteUri.Substring(0, self.AbsoluteUri.Length - self.AbsolutePath.Length)}{path.Replace("%2f", "/")}");

}

