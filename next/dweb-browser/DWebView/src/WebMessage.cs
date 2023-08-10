using DwebBrowser.Helper;

namespace DwebBrowser.DWebView;


public class WebMessage
{
    public readonly NSObject Data;
    public readonly WebMessagePort[] Ports;
    public WebMessage(NSObject data, WebMessagePort[]? ports)
    {
        Data = data;
        Ports = ports ?? Array.Empty<WebMessagePort>();
    }
    public WebMessage(NSObject data)
    {
        Data = data;
        Ports = Array.Empty<WebMessagePort>();
    }
    public static WebMessage From(string message, WebMessagePort[]? ports = default) => new(new NSString(message), ports);
    public static WebMessage From(int message, WebMessagePort[]? ports = default) => new(new NSNumber(message), ports);
    public static WebMessage From(float message, WebMessagePort[]? ports = default) => new(new NSNumber(message), ports);
    public static WebMessage From(double message, WebMessagePort[]? ports = default) => new(new NSNumber(message), ports);
    public static WebMessage From(bool message, WebMessagePort[]? ports = default) => new(NSNumber.FromBoolean(message), ports);
    /// <summary>
    /// Int32Array
    /// </summary>
    /// <param name="message"></param>
    /// <param name="ports"></param>
    /// <returns></returns>
    public static WebMessage FromBytes(byte[] bytes, WebMessagePort[]? ports = default)
    {
        var len = ((uint)bytes.Length);
        var arr = new NSMutableArray(new UIntPtr(len));
        //var pos = 0;
        //for (var i = 0; i < len; i++, pos+=4)
        //{
        //    arr.Add(new NSNumber(BitConverter.ToInt32(bytes, pos)));
        //}
        for (var i = 0; i < len; i++)
        {
            arr.Add(new NSNumber(bytes[i]));
        }

        //IntPtr ptr = Marshal.AllocHGlobal(bytes.Length);
        //Marshal.Copy(bytes, 0, ptr, bytes.Length);

        //NSData data = NSData.FromBytesNoCopy(ptr, (nuint)bytes.Length);


        //Marshal.FreeHGlobal(ptr);

        return new(arr, ports);
    }
    /// <summary>
    /// cbor
    /// </summary>
    /// <param name="message"></param>
    /// <param name="ports"></param>
    /// <returns></returns>
    public static WebMessage From(byte[] message, WebMessagePort[]? ports = default)
    {
        byte[] byteArr = CborHelper.Encode(message);

        var arr = new NSMutableArray();
        for (var i = 0; i < byteArr.Length; i++)
        {
            arr.Add(new NSNumber(byteArr[i]));
        }

        return new(arr, ports);
    }
}
