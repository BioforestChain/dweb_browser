using System;
namespace DwebBrowser.HelperTests;

public class RequestExtensionsTests
{
    [Theory]
    [InlineData("https://www.baidu.com/xx.html?a=aa&b=cc")]
    [Trait("Helper", "Request")]
    public void QueryValidate_RequiredBaseTypeString_ReturnSuccess(string url)
    {
        var request = new HttpRequestMessage() { RequestUri = new Uri(url) };
        Assert.Equal("aa", request.QueryValidate<string>("a"));
        Assert.Equal("cc", request.QueryValidate<string>("b"));
    }

    [Theory]
    [InlineData("https://www.baidu.com/xx.html?a=10")]
    [Trait("Helper", "Request")]
    public void QueryValidate_RequiredBaseTypeInt_ReturnSuccess(string url)
    {
        var request = new HttpRequestMessage() { RequestUri = new Uri(url) };
        Assert.Equal(10, request.QueryValidate<int>("a"));
    }

    [Theory]
    [InlineData("https://www.baidu.com/xx.html?a=aa&c=true")]
    [Trait("Helper", "Request")]
    public void QueryValidate_RequiredBaseTypeBool_ReturnSuccess(string url)
    {
        var request = new HttpRequestMessage() { RequestUri = new Uri(url) };
        Assert.True(request.QueryValidate<bool>("c"));
    }

    /// <summary>
    /// 字符串的 default 值是 null 而不是 ""
    /// </summary>
    [Theory]
    [InlineData("https://www.baidu.com/xx.html?a=aa&b=cc")]
    [Trait("Helper", "Request")]
    public void QueryValidate_OptionalBaseTypeString_ReturnSuccess(string url)
    {
        var request = new HttpRequestMessage() { RequestUri = new Uri(url) };
        Assert.Null(request.QueryValidate<string>("d", false));
    }

    [Theory]
    [InlineData("https://www.baidu.com/xx.html?a=10")]
    [Trait("Helper", "Request")]
    public void QueryValidate_OptionalBaseTypeInt_ReturnSuccess(string url)
    {
        var request = new HttpRequestMessage() { RequestUri = new Uri(url) };
        Assert.Equal(0, request.QueryValidate<int>("c", false));
    }

    [Theory]
    [InlineData("https://www.baidu.com/xx.html?a=aa&c=true")]
    [Trait("Helper", "Request")]
    public void QueryValidate_OptionalBaseTypeBool_ReturnSuccess(string url)
    {
        var request = new HttpRequestMessage() { RequestUri = new Uri(url) };
        Assert.False(request.QueryValidate<bool>("d", false));
    }

    [Theory]
    [InlineData("https://www.baidu.com/xx.html?a=aa&c=true")]
    public void QueryValidate_BaseTypeBool_ReturnFail(string url)
    {
        var request = new HttpRequestMessage() { RequestUri = new Uri(url) };
        Action actual = () => request.QueryValidate<int>("d");

        var ex = Assert.Throws<Exception>(actual);

        Assert.Contains("required query is null", ex.Message);
    }
}

