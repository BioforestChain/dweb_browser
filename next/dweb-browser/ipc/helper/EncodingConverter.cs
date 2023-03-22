
namespace ipc.helper;

public class EncodingConverter
{
	public static byte[] DataToBinary(object data, IPC_DATA_ENCODING encoding)
	{
		switch (encoding)
		{
			case IPC_DATA_ENCODING.BINARY:
				return (byte[])data;
			case IPC_DATA_ENCODING.BASE64:
				return Convert.FromBase64String((string)data);
			case IPC_DATA_ENCODING.UTF8:
				return System.Text.UTF8Encoding.UTF8.GetBytes((string)data);
			default:
				throw new Exception("unknown encoding");
		}
	}

	public static string DataToText(object data, IPC_DATA_ENCODING encoding)
	{
		switch (encoding)
		{
			case IPC_DATA_ENCODING.BINARY:
				return System.Text.UTF8Encoding.UTF8.GetString((byte[])data);
			case IPC_DATA_ENCODING.BASE64:
				return System.Text.UTF8Encoding.UTF8.GetString(Convert.FromBase64String((string)data));
			case IPC_DATA_ENCODING.UTF8:
				return (string)data;
			default:
				throw new Exception("unknown encoding");
		}
	}
}

