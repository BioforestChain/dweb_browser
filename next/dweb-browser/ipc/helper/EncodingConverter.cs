
namespace ipc.helper;

public static class EncodingConverter
{
	/**
	 * <summary>
	 * Convert base64 string、utf8 string、byte[] to utf8 byte[].
	 * </summary> 
	 * 
	 * <param name="data">source data</param>
	 * <param name="encoding">source data encoding type</param>
	 * 
	 * <returns>byte[]</returns>
	 */
	public static byte[] DataToBinary(object data, IPC_DATA_ENCODING encoding) =>
		encoding switch
		{
			IPC_DATA_ENCODING.BINARY => (byte[])data,
			IPC_DATA_ENCODING.BASE64 => Convert.FromBase64String((string)data),
			IPC_DATA_ENCODING.UTF8 => System.Text.UTF8Encoding.UTF8.GetBytes((string)data),
			_ => throw new Exception("unknown encoding"),
		};

    /**
	 * <summary>
	 * Convert byte[]、base64 string、string to utf8 string.
	 * </summary>
	 * 
	 * <param name="data">source data</param>
	 * <param name="encoding">source data encoding type</param>
	 * 
	 * <returns>string</returns>
	 */
    public static string DataToText(object data, IPC_DATA_ENCODING encoding) =>
		encoding switch
		{
			IPC_DATA_ENCODING.BINARY => System.Text.UTF8Encoding.UTF8.GetString((byte[])data),
			IPC_DATA_ENCODING.BASE64 => System.Text.UTF8Encoding.UTF8.GetString(Convert.FromBase64String((string)data)),
			IPC_DATA_ENCODING.UTF8 => (string)data,
			_ => throw new Exception("unknown encoding"),
		};
}

