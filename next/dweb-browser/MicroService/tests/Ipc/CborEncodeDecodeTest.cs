using System.Text.Json;
using PeterO.Cbor;
namespace DwebBrowser.MicroServiceTests;

public class CborEncodeDecodeTest
{
    public struct IpcMessageTestItem
    {
        public string Item { get; set; }
    }

    public struct IpcMessageTestData
    {
        public string Data { get; set; }
        public int Number { get; set; }
        public bool BoolValue { get; set; }
        public IpcMessageTestItem Item { get; set; }
    }

    [Fact]
    public void EncodeToBytes_IpcMessageTestData_ReturnSuccess()
    {
        var ipcMessageTestData = new IpcMessageTestData()
        {
            Data = "testData",
            Number = 10,
            BoolValue = false,
            Item = new IpcMessageTestItem() { Item = "testItem" }
        };

        var json = JsonSerializer.Serialize(ipcMessageTestData);
        Debug.WriteLine(json);
        var cbor = CBORObject.FromJSONBytes(json.FromUtf8());
        Assert.IsType<byte[]>(cbor.EncodeToBytes());
    }

    [Fact]
    public void ToJsonString_IpcMessageTestData_ReturnSuccess()
    {
        var ipcMessageTestData = new IpcMessageTestData()
        {
            Data = "testData",
            Number = 10,
            BoolValue = false,
            Item = new IpcMessageTestItem() { Item = "testItem" }
        };

        var json = JsonSerializer.Serialize(ipcMessageTestData);
        var cbor = CBORObject.FromJSONBytes(json.FromUtf8());
        var encoded = cbor.EncodeToBytes();

        Debug.WriteLine(CBORObject.DecodeFromBytes(encoded).ToJSONString());
        var deserializeJson = JsonSerializer.Deserialize<IpcMessageTestData>(
            CBORObject.DecodeFromBytes(encoded).ToJSONString());

        Assert.Equal(ipcMessageTestData, deserializeJson);
    }
}

