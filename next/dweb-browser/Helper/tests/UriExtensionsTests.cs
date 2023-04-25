using DwebBrowser.Helper;
using System.Diagnostics;
using System;

namespace DwebBrowser.HelperTests
{
    public class UriExtensionsTests
    {
        [Theory]
        [InlineData("https://js.sys.dweb:80/%3cinternal%3e/bootstrap.js")]
        public void Uri_SpecialChar_ReturnSuccess(string url)
        {
            var uri = new Uri(url);

            Debug.WriteLine(uri.AbsoluteUri);
            Assert.IsType<Uri>(uri);
            Assert.Equal(url, uri.AbsoluteUri);
        }

        [Theory]
        [InlineData("https://js.sys.dweb:80/", "%2f%3cinternal%3e/bootstrap.js")]
        public void Path_Replace_ReturnSuccess(string url, string path)
        {
            var uri = new Uri(url);
            Debug.WriteLine(path.Replace("%2f", "/"));
            Debug.WriteLine(string.Concat(uri.AbsoluteUri.AsSpan(0, uri.AbsoluteUri.Length - 1), path.Replace("%2f", "/")));
            Assert.Equal(
                new Uri("https://js.sys.dweb:80/%3cinternal%3e/bootstrap.js"),
                new Uri(string.Concat(uri.AbsoluteUri.AsSpan(0, uri.AbsoluteUri.Length - 1), path.Replace("%2f", "/")))
                );
        }
    }
}