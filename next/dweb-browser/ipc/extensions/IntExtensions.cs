
namespace ipc.extensions;

public static class IntExtensions
{
	public static byte[] toByteArray(this int self) => BitConverter.GetBytes(self);
}

