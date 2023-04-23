using System.Text.Json;
using System.Text.Json.Serialization;

namespace DwebBrowser.MicroServiceTests;

public class IpcReqMessageSerializeTest
{

    static IpcReqMessage _ipcReqMessageNull = new IpcReqMessage(
            1,
            IpcMethod.Get,
            "https://www.baidu.com/",
            new Dictionary<string, string>() { { "Content-Type", "text/plain" } },
            new SMetaBody(
                SMetaBody.IPC_META_BODY_TYPE.STREAM_ID,
                0,
                "data"
                ));

    static IpcReqMessage _ipcReqMessage = new IpcReqMessage(
            1,
            IpcMethod.Get,
            "https://www.baidu.com/",
            new Dictionary<string, string>() { { "Content-Type", "text/plain" } },
            new SMetaBody(
                SMetaBody.IPC_META_BODY_TYPE.STREAM_ID,
                0,
                "data",
                "streamId",
                1)
        );

    [Fact]
    public void ToJson_IpcReqMessage_ReturnSuccess()
    {
        Debug.WriteLine(_ipcReqMessage.ToJson());
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
        Assert.IsType<string>(_ipcReqMessage.ToJson());
    }

    [Fact]
    public void ToJson_StreamIdAndReceiverUidIsNull_ReturnSuccess()
    {
        Assert.IsType<string>(_ipcReqMessageNull.ToJson());
    }

    [Fact]
    public void FromJson_IpcReqMessage_ReturnSuccess()
    {
        //var ipcReqMessage = IpcReqMessageTest.FromJson(@"{ ""type"":0,""req_id"":1,""method"":""GET"",""url"":""https://www.baidu.com/""}");
        //Debug.WriteLine(_ipcReqMessageTest.ToJson());
        //var ipcReqMessage = IpcReqMessageTest.FromJson(_ipcReqMessageTest.ToJson());
        //var ipcReqMessage = JsonSerializer.Deserialize<IpcReqMessageTest>(_ipcReqMessageTest.ToJson());

        //var ipcReqMessage = IpcReqMessage.FromJson(_ipcReqMessage.ToJson());
        var ipcReqMessage = IpcReqMessage.FromJson(@"{
                 ""type"":0,
                 ""req_id"":1,
                 ""method"":""GET"",
                 ""url"":""https://www.baidu.com/"",
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

        Debug.WriteLine(ipcReqMessage);
        Debug.WriteLine($"type: {ipcReqMessage.Type.ToString()}");
        Debug.WriteLine($"req_id: {ipcReqMessage.ReqId}");
        Debug.WriteLine($"method: {ipcReqMessage.Method}");
        Debug.WriteLine($"url: {ipcReqMessage.Url}");
        Debug.WriteLine($"headers: {ipcReqMessage.Headers}");
        foreach (var entry in ipcReqMessage.Headers)
        {
            Debug.WriteLine($"{entry.Key} : {entry.Value}");
        }
        Debug.WriteLine($"metaBody: {ipcReqMessage.MetaBody}");

        Assert.IsType<IpcReqMessage>(ipcReqMessage);
    }
}

