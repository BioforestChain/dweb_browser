namespace DwebBrowser.MicroServiceTests.Ipc;

public class MetaBodyTest
{
    [Fact]
    public async Task MetaBodyToJson()
    {
        var str = "";
        foreach(var i in Enumerable.Range(1,60000))
        {
            str += i.ToString();
        }
        var data = new MetaBody(MetaBody.IPC_META_BODY_TYPE.INLINE_BASE64, 1, str, "r2", 3);


        var x = Stopwatch.StartNew();
        x.Start();
        foreach (var i in Enumerable.Range(1, 10))
        {
            var json = data.ToJson();
            Debug.WriteLine("Size: {0}, Time: {1}", json.Length, x.ElapsedMilliseconds);
        }
    }
}

