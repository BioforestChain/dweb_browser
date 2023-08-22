namespace DwebBrowser.Helper;

public static class FloatExtensions
{
	public static int ToInt(this float self) => Convert.ToInt32(self);
	public static string To2Hex(this float self) => (self * 255).ToInt().ToString("X").PadLeft(2, '0');
}

