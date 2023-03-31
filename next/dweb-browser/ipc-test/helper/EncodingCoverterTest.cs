
namespace ipc_test.helper;

public class EncodingCoverterTest : Log
{
    public EncodingCoverterTest(ITestOutputHelper output) : base(output)
    { }

    public static IEnumerable<object[]> DataEncodingAndExpected()
    {
        yield return new object[] { new byte[] { 0, 4, 255 }, IPC_DATA_ENCODING.BINARY, new byte[] { 0, 4, 255 } };
        yield return new object[] { new byte[] { 10, 122, 235, 23 }, IPC_DATA_ENCODING.BINARY, new byte[] { 10, 122, 235, 23 } };
        yield return new object[] { new byte[] { byte.MinValue, 122, byte.MaxValue }, IPC_DATA_ENCODING.BINARY,
            new byte[] { byte.MinValue, 122, byte.MaxValue } };
    }

    [Theory]
    [MemberData(nameof(DataEncodingAndExpected))]
    [Trait("Helper", "Encoding")]
    public void DataToBinary_ByteArrayDataEncodingBinary_ReturnsByteArray(byte[] data, IPC_DATA_ENCODING encoding, byte[] expected) =>
        Assert.Equal(expected, EncodingConverter.DataToBinary(data, encoding));

    [Theory]
    // data = test
    [InlineData("dGVzdA==", IPC_DATA_ENCODING.BASE64, new byte[] { 116, 101, 115, 116 })]
    // data = ipc
    [InlineData("aXBj", IPC_DATA_ENCODING.BASE64, new byte[] { 105, 112, 99 })]
    [Trait("Helper", "Encoding")]
    public void DataToBinary_StringDataEncodingBase64_ReturnsByteArray(string data, IPC_DATA_ENCODING encoding, byte[] expected) =>
        Assert.Equal(expected, EncodingConverter.DataToBinary(data, encoding));

    [Theory]
    [InlineData("test", IPC_DATA_ENCODING.UTF8, new byte[] { 116, 101, 115, 116 })]
    [InlineData("ipc", IPC_DATA_ENCODING.UTF8, new byte[] { 105, 112, 99 })]
    [Trait("Helper", "Encoding")]
    public void DataToBinary_StringDataEncodingUtf8_ReturnsByteArray(string data, IPC_DATA_ENCODING encoding, byte[] expected) =>
        Assert.Equal(expected, EncodingConverter.DataToBinary(data, encoding));

    [Fact]
    [Trait("Helper", "Encoding")]
    public void DataToBinary_UnknownEncoding_ThrowException()
    {
        Action actual = () => EncodingConverter.DataToBinary("", (IPC_DATA_ENCODING)3);

        var ex = Assert.Throws<Exception>(actual);

        Assert.Equal("unknown encoding", ex.Message);
    }

    [Theory]
    [InlineData(new byte[] { 116, 101, 115, 116 }, IPC_DATA_ENCODING.BINARY, "test")]
    [InlineData(new byte[] { 105, 112, 99 }, IPC_DATA_ENCODING.BINARY, "ipc")]
    [Trait("Helper", "Encoding")]
    public void DataToText_ByteArrayDataEncodingBinary_ReturnsString(byte[] data, IPC_DATA_ENCODING encoding, string expected) =>
        Assert.Equal(expected, EncodingConverter.DataToText(data, encoding));

    [Theory]
    [InlineData("dGVzdA==", IPC_DATA_ENCODING.BASE64, "test")]
    [InlineData("aXBj", IPC_DATA_ENCODING.BASE64, "ipc")]
    [Trait("Helper", "Encoding")]
    public void DataToText_StringDataEncodingBase64_ReturnsString(string data, IPC_DATA_ENCODING encoding, string expected) =>
        Assert.Equal(expected, EncodingConverter.DataToText(data, encoding));

    [Theory]
    [InlineData("test", IPC_DATA_ENCODING.UTF8, "test")]
    [InlineData("ipc", IPC_DATA_ENCODING.UTF8, "ipc")]
    [Trait("Helper", "Encoding")]
    public void DataToText_StringDataEncodingUtf8_ReturnsString(string data, IPC_DATA_ENCODING encoding, string expected) =>
        Assert.Equal(expected, EncodingConverter.DataToText(data, encoding));

    [Fact]
    [Trait("Helper", "Encoding")]
    public void DataToText_UnknownEncoding_ThrowException()
    {
        Action actual = () => EncodingConverter.DataToText("", (IPC_DATA_ENCODING)5);

        var ex = Assert.Throws<Exception>(actual);

        Assert.Equal("unknown encoding", ex.Message);
    }
}

