
namespace ipc.helper;

public class EncodingConverter
{
	public static byte[] DataToBinary(object data, IPC_DATA_ENCODING encoding) =>
		encoding switch
		{
			IPC_DATA_ENCODING.BINARY => (byte[])data,
			IPC_DATA_ENCODING.BASE64 => Convert.FromBase64String((string)data),
			IPC_DATA_ENCODING.UTF8 => System.Text.UTF8Encoding.UTF8.GetBytes((string)data),
			_ => throw new Exception("unknown encoding"),
		};

	public static string DataToText(object data, IPC_DATA_ENCODING encoding) =>
		encoding switch
		{
			IPC_DATA_ENCODING.BINARY => System.Text.UTF8Encoding.UTF8.GetString((byte[])data),
			IPC_DATA_ENCODING.BASE64 => System.Text.UTF8Encoding.UTF8.GetString(Convert.FromBase64String((string)data)),
			IPC_DATA_ENCODING.UTF8 => (string)data,
			_ => throw new Exception("unknown encoding"),
		};
}

