
namespace DwebBrowser.Helper;

public static class IntExtensions
{
	public static byte[] ToByteArray(this int self) => BitConverter.GetBytes(self);

	/// <summary>
	/// convert long to int32
	/// </summary>
	/// <returns>return int32</returns>
	public static int ToInt(this long self)
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

