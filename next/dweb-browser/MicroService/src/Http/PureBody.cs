namespace DwebBrowser.MicroService.Http;

public interface PureBody
{
    public abstract dynamic? Raw { get; }

    public abstract Stream ToStream();

    public abstract byte[] ToByteArray();

    public abstract string ToUtf8String();

    static public PureEmptyBody Empty = new();
}

public record PureStreamBody(Stream Data) : PureBody
{
    public dynamic? Raw => Data;

    public byte[] ToByteArray()
    {
        return Data.ToByteArray();
    }

    public Stream ToStream()
    {
        return Data;
    }

    public string ToUtf8String()
    {
        return Data.ToByteArray().ToUtf8();
    }
}
public record PureByteArrayBody(byte[] Data) : PureBody
{
    public dynamic? Raw => Data;
    public byte[] ToByteArray()
    {
        return Data;
    }

    public Stream ToStream()
    {
        return new MemoryStream(Data);
    }

    public string ToUtf8String()
    {
        return Data.ToUtf8();
    }
}
public record PureBase64StringBody(string XData) : PureByteArrayBody(XData.ToBase64ByteArray());

public record PureUtf8StringBody(string Data) : PureBody
{
    public dynamic? Raw => Data;
    public byte[] ToByteArray()
    {
        return Data.ToUtf8ByteArray();
    }

    public Stream ToStream()
    {
        return new MemoryStream(ToByteArray());
    }

    public string ToUtf8String()
    {
        return Data;
    }
}

public record PureEmptyBody() : PureBody
{
    public dynamic? Raw => null;

    static byte[] EmptyByteArray = new byte[0];
    static Stream EmptyStream = new MemoryStream(EmptyByteArray);
    public byte[] ToByteArray()
    {
        return EmptyByteArray;
    }

    public Stream ToStream()
    {
        return EmptyStream;
    }

    public string ToUtf8String()
    {
        return "";
    }
}