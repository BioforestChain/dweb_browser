using System.Text.Json;
using System.Text.Json.Serialization;

namespace DwebBrowser.MicroServiceTests;

public class IpcResMessageSerializeTest
{
    static IpcResMessage _ipcResMessageNull = new IpcResMessage(
            1,
            200,
            new Dictionary<string, string>() { { "Content-Type", "text/plain" } },
            new MetaBody(
                MetaBody.IPC_META_BODY_TYPE.STREAM_ID,
                0,
                "data"
                ));

    static IpcResMessage _ipcResMessage = new IpcResMessage(
            1,
            404,
            new Dictionary<string, string>() { { "Content-Type", "text/plain" } },
            new MetaBody(
                MetaBody.IPC_META_BODY_TYPE.STREAM_ID,
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
        Debug.WriteLine(_ipcResMessageNull.ToJson());
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
        Debug.WriteLine(String.Format("type: {0}", ipcResMessage.Type.ToString()));
        Debug.WriteLine(String.Format("req_id: {0}", ipcResMessage.ReqId));
        Debug.WriteLine(String.Format("method: {0}", ipcResMessage.StatusCode));
        Debug.WriteLine(String.Format("headers: {0}", ipcResMessage.Headers));
        foreach (var entry in ipcResMessage.Headers)
        {
            Debug.WriteLine(String.Format("{0} : {1}", entry.Key, entry.Value));
        }
        Debug.WriteLine(String.Format("metaBody: {0}", ipcResMessage.MetaBody));

        Assert.IsType<IpcResMessage>(ipcResMessage);
    }
}

