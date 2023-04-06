using System;
namespace DwebBrowser.HelperTests;

public class RequestExtensionsTests
{
	[Theory]
	[InlineData("https://www.baidu.com/xx.html?a=aa&b=cc")]
	[Trait("Helper", "Request")]
	public void QueryValidate_BaseTypeString_ReturnSuccess(string url)
	{
		var request = new HttpRequestMessage() { RequestUri = new Uri(url) };
		Assert.Equal("aa", request.QueryValidate<string>("a"));
		Assert.Equal("cc", request.QueryValidate<string>("b"));
	}

	[Theory]
    [InlineData("https://www.baidu.com/xx.html?a=10")]
	[Trait("Helper", "Request")]
    public void QueryValidate_BaseTypeInt_ReturnSuccess(string url)
	{
        var request = new HttpRequestMessage() { RequestUri = new Uri(url) };
		Assert.Equal(10, request.QueryValidate<int>("a"));
    }

    [Theory]
    [InlineData("https://www.baidu.com/xx.html?a=aa&c=true")]
    [Trait("Helper", "Request")]
    public void QueryValidate_BaseTypeBool_ReturnSuccess(string url)
	{
        var request = new HttpRequestMessage() { RequestUri = new Uri(url) };
		Assert.True(request.QueryValidate<bool>("c"));
    }

	[Theory]
	[InlineData("https://www.baidu.com/xx.html?a=aa&c=true")]
	public void QueryValidate_BaseTypeBool_ReturnFail(string url)
	{
        var request = new HttpRequestMessage() { RequestUri = new Uri(url) };
        Action actual = () => request.QueryValidate<int>("d");

        var ex = Assert.Throws<Exception>(actual);

        Assert.Contains("query name is no found", ex.Message);
    }
}

