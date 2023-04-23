using System.Text.Json;
using System.Text.Json.Serialization;

namespace DwebBrowser.MicroServiceTests;

public class IpcResMessageSerializeTest
{
    static IpcResMessage _ipcResMessageNull = new IpcResMessage(
            1,
            200,
            new Dictionary<string, string>() { { "Content-Type", "text/plain" } },
            new SMetaBody(
                SMetaBody.IPC_META_BODY_TYPE.STREAM_ID,
                0,
                "data"
                ));

    static IpcResMessage _ipcResMessage = new IpcResMessage(
            1,
            404,
            new Dictionary<string, string>() { { "Content-Type", "text/plain" } },
            new SMetaBody(
                SMetaBody.IPC_META_BODY_TYPE.STREAM_ID,
                0,
                "data",
                "streamId",
                1)
        );

    [Fact]
    public void ToJson_IpcResMessage_ReturnSuccess()
    {
        Debug.WriteLine(_ipcResMessage.ToJson());
        //{
        //    "type":0,
        //	"req_id":1,
        //	"method":"GET",
        //	"url":"https://www.baidu.com/",
        //	"headers":{ "Content-Type":"text/plain"},
        //	"metaBody":{
        //        "type":0,
        //		"senderUid":0,
        //		"streamId":"streamId",
        //		"receiverUid":1,
        //		"metaId":"94o_CFs4MBo",
        //		"data":"data"

        //    }
        //}
        Assert.IsType<string>(_ipcResMessage.ToJson());
    }

    [Fact]
    public void ToJson_StreamIdAndReceiverUidIsNull_ReturnSuccess()
    {
        Assert.IsType<string>(_ipcResMessageNull.ToJson());
    }

    [Fact]
    public void FromJson_IpcResMessage_ReturnSuccess()
    {
        //var ipcReqMessage = IpcReqMessageTest.FromJson(@"{ ""type"":0,""req_id"":1,""method"":""GET"",""url"":""https://www.baidu.com/""}");
        //Debug.WriteLine(_ipcReqMessageTest.ToJson());
        //var ipcReqMessage = IpcReqMessageTest.FromJson(_ipcReqMessageTest.ToJson());
        //var ipcReqMessage = JsonSerializer.Deserialize<IpcReqMessageTest>(_ipcReqMessageTest.ToJson());

        //var ipcReqMessage = IpcReqMessage.FromJson(_ipcReqMessage.ToJson());
        var ipcResMessage = IpcResMessage.FromJson(@"{
                 ""type"":1,
                 ""req_id"":1,
                 ""statusCode"":200,
                 ""headers"":{ ""Content-Type"":""text/plain""},
                 ""metaBody"":{
                        ""type"":0,
                  ""senderUid"":0,
                  ""streamId"":""streamId"",
                  ""receiverUid"":1,
                  ""metaId"":""94o_CFs4MBo"",
                  ""data"":""data""
                    }
                }
            ");

        Debug.WriteLine(ipcResMessage);
        Debug.WriteLine($"type: {ipcResMessage.Type.ToString()}");
        Debug.WriteLine($"req_id: {ipcResMessage.ReqId}");
        Debug.WriteLine($"method: {ipcResMessage.StatusCode}");
        Debug.WriteLine($"headers: {ipcResMessage.Headers}");
        foreach (var entry in ipcResMessage.Headers)
        {
            Debug.WriteLine($"{entry.Key} : {entry.Value}");
        }
        Debug.WriteLine($"metaBody: {ipcResMessage.MetaBody}");

        Assert.IsType<IpcResMessage>(ipcResMessage);
    }
}

