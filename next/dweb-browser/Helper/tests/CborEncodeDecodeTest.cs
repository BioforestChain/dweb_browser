using System.Text.Json;
using PeterO.Cbor;
using System.Formats.Cbor;

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
        var cbor = CBORObject.FromJSONBytes(json.ToUtf8ByteArray());
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
        var cbor = CBORObject.FromJSONBytes(json.ToUtf8ByteArray());
        var encoded = cbor.EncodeToBytes();

        Debug.WriteLine(CBORObject.DecodeFromBytes(encoded).ToJSONString());
        var deserializeJson = JsonSerializer.Deserialize<IpcMessageTestData>(
            CBORObject.DecodeFromBytes(encoded).ToJSONString());

        Assert.Equal(ipcMessageTestData, deserializeJson);
    }

    [Fact]
    public void SystemCbor_EncodeToBytes_IpcMessageTestData_ReturnSuccess()
    {
        var ipcMessageTestData = new IpcMessageTestData()
        {
            Data = "testData",
            Number = 10,
            BoolValue = false,
            Item = new IpcMessageTestItem() { Item = "testItem" }
        };

        var writer = new CborWriter();

        var json = JsonSerializer.Serialize(ipcMessageTestData);
        Debug.WriteLine(json);

        //var cbor = CBORObject.FromJSONBytes(json.ToUtf8ByteArray());
        writer.WriteByteString(json.ToUtf8ByteArray());
        var cbor = writer.Encode();
        Debug.WriteLine(cbor);
        Assert.IsType<byte[]>(cbor);
    }

    [Fact]
    public void SystemCbor_ToJsonString_IpcMessageTestData_ReturnSuccess()
    {
        var ipcMessageTestData = new IpcMessageTestData()
        {
            Data = "testData",
            Number = 10,
            BoolValue = false,
            Item = new IpcMessageTestItem() { Item = "testItem" }
        };

        var writer = new CborWriter();
        var json = JsonSerializer.Serialize(ipcMessageTestData);
        writer.WriteByteString(json.ToUtf8ByteArray());
        var encoded = writer.Encode();

        var reader = new CborReader(encoded);
        var result = reader.ReadByteString().ToUtf8();
        Debug.WriteLine(result);

        //Debug.WriteLine(CBORObject.DecodeFromBytes(encoded).ToJSONString());
        var deserializeJson = JsonSerializer.Deserialize<IpcMessageTestData>(result);

        Assert.Equal(ipcMessageTestData, deserializeJson);
    }
}

