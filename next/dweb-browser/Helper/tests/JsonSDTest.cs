using System;
using System.Text.Json;
namespace DwebBrowser.HelperTests;

public class JsonSDTest
{

    class SimpleData
    {
        public string Token { get; set; }
    }

    [Fact]
    public void ClassFieldTest()
    {
        var data = new SimpleData();
        data.Token = "3";

        var jsonData = JsonSerializer.Serialize(data);
        Debug.WriteLine("jsonData: {0}", jsonData);

        var data2 = JsonSerializer.Deserialize<SimpleData>(jsonData);
        Debug.WriteLine("data2.Token: {0}", data2.Token);

        Assert.Equal(data2.Token, data.Token);
    }
}

