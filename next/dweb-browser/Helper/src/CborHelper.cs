using System.Formats.Cbor;
using System.Text.Json;

namespace DwebBrowser.Helper;

public enum CborEncodeType
{
    UTF8,
    BASE64
}

public static class CborHelper
{
    public static byte[] Encode(string json, CborEncodeType type = CborEncodeType.UTF8)
    {
        var writer = new CborWriter();

        switch (type)
        {
            case CborEncodeType.UTF8:
                writer.WriteByteString(json.ToUtf8ByteArray());
                break;
            case CborEncodeType.BASE64:
                writer.WriteByteString(json.ToBase64ByteArray());
                break;
        }

        return writer.Encode();
    }

    public static byte[] Encode(byte[] bytes)
    {
        var writer = new CborWriter();

        writer.WriteByteString(bytes);

        return writer.Encode();
    }

    public static byte[] Decode(byte[] encoded)
    {
        var reader = new CborReader(encoded);

        return reader.ReadByteString();
    }

    public static T? DecodeDeserialize<T>(byte[] encoded)
    {
        var reader = new CborReader(encoded);
        return JsonSerializer.Deserialize<T>(reader.ReadByteString());
    }

    public static string DecodeJson(byte[] encoded) => Decode(encoded).ToUtf8();
}

