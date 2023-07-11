namespace DwebBrowser.Helper;

public static class DoubleExtensions
{
	public static nfloat ToNFloat(this double self) => new(self / 255f);
}

