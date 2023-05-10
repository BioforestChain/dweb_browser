namespace DwebBrowser.MicroService.Http;

public interface PureBody
{
    public abstract dynamic? Raw { get; }

    public abstract Stream ToStream();
    // public virtual async Task<Stream> ToStreamAsync() => ToStream();

    public abstract byte[] ToByteArray();
    public virtual async Task<byte[]> ToByteArrayAsync() => ToByteArray();

    public abstract string ToUtf8String();
    public virtual async Task<string> ToUtf8StringAsync() => ToUtf8String();

    static public PureEmptyBody Empty = new();
}

public record PureStreamBody(Stream Data) : PureBody
{
    public dynamic? Raw => Data;

    public Stream ToStream() => Data;

    public byte[] ToByteArray() => Data.ToByteArray();
    public Task<byte[]> ToByteArrayAsync() => Data.ToByteArrayAsync();

    public string ToUtf8String() => Data.ToByteArray().ToUtf8();
    public async Task<string> ToUtf8StringAsync() => (await Data.ToByteArrayAsync()).ToUtf8();
}
public record PureByteArrayBody(byte[] Data) : PureBody
{
    public dynamic? Raw => Data;
    public byte[] ToByteArray() => Data;
    public Stream ToStream() => new MemoryStream(Data);

    public string ToUtf8String() => Data.ToUtf8();
}
public record PureBase64StringBody(string XData) : PureByteArrayBody(XData.ToBase64ByteArray());

public record PureUtf8StringBody(string Data) : PureBody
{
    public dynamic? Raw => Data;
    public byte[] ToByteArray() => Data.ToUtf8ByteArray();

    public Stream ToStream() => new MemoryStream(ToByteArray());

    public string ToUtf8String() => Data;
}

public record PureEmptyBody() : PureBody
{
    public dynamic? Raw => null;

    static byte[] EmptyByteArray = new byte[0];
    static Stream EmptyStream = new MemoryStream(EmptyByteArray);
    public byte[] ToByteArray() => EmptyByteArray;


    public Stream ToStream() => EmptyStream;


    public string ToUtf8String() => "";

}