
namespace ipc.extensions;

public static class IntExtensions
{
	public static byte[] toByteArray(this int self) => BitConverter.GetBytes(self);

	/// <summary>
	/// convert int64 to int32
	/// </summary>
	/// <returns>return int32</returns>
	public static int toInt(this Int64 self)
	{
		try
		{
			return (int)self;
		}
		catch(Exception e)
		{
			Console.WriteLine(e.Message);
			throw e;
		}
	}
}

