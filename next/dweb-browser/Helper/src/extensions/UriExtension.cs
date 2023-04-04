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
}

