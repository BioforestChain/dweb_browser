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
            new MetaBody(
                MetaBody.IPC_META_BODY_TYPE.STREAM_ID,
                0,
                "data"
                ));

    static IpcReqMessage _ipcReqMessage = new IpcReqMessage(
            1,
            IpcMethod.Get,
            "https://www.baidu.com/",
            new Dictionary<string, string>() { { "Content-Type", "text/plain" } },
            new MetaBody(
                MetaBody.IPC_META_BODY_TYPE.STREAM_ID,
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
        Debug.WriteLine(_ipcReqMessageNull.ToJson());
        Assert.IsType<string>(_ipcReqMessageNull.ToJson());
    }

    [Fact]
    public void FromJson_IpcReqMessage_ReturnSuccess()
    {
        Debug.WriteLine(_ipcReqMessage.ToJson());
        var ipcReqMessage = IpcReqMessage.FromJson(_ipcReqMessage.ToJson());
        //var ipcReqMessage = IpcReqMessage.FromJson(@"{
        //         ""type"":0,
        //         ""req_id"":1,
        //         ""method"":""GET"",
        //         ""url"":""https://www.baidu.com/"",
        //         ""headers"":{ ""Content-Type"":""text/plain""},
        //         ""metaBody"":{
        //                ""type"":0,
        //          ""senderUid"":0,
        //          ""streamId"":""streamId"",
        //          ""receiverUid"":1,
        //          ""metaId"":""94o_CFs4MBo"",
        //          ""data"":""data""
        //            }
        //        }
        //    ");

        Debug.WriteLine(ipcReqMessage);
        Debug.WriteLine(String.Format("type: {0}", ipcReqMessage.Type.ToString()));
        Debug.WriteLine(String.Format("req_id: {0}", ipcReqMessage.ReqId));
        Debug.WriteLine(String.Format("method: {0}", ipcReqMessage.Method));
        Debug.WriteLine(String.Format("url: {0}", ipcReqMessage.Url));
        Debug.WriteLine(String.Format("headers: {0}", ipcReqMessage.Headers));
        foreach (var entry in ipcReqMessage.Headers)
        {
            Debug.WriteLine(String.Format("{0} : {1}", entry.Key, entry.Value));
        }
        Debug.WriteLine(String.Format("metaBody: {0}", ipcReqMessage.MetaBody));

        Assert.IsType<IpcReqMessage>(ipcReqMessage);
    }

    class QAQ
    {
        [JsonPropertyName("req_id")]
        public int ReqId { get; set; }
        [Obsolete("使用带参数的构造函数", true)]
        public QAQ()
        {
        }
        public QAQ(int req_id)
        {
            this.ReqId = req_id;
        }
    }
    [Fact]
    public void JsonTest()
    {
        var qaq = new QAQ(12313);
        var res = JsonSerializer.Serialize(qaq);
        Debug.WriteLine(res);

        var qaqclone = JsonSerializer.Deserialize<QAQ>(res);
        Debug.WriteLine(qaqclone.ReqId);
    }
}

