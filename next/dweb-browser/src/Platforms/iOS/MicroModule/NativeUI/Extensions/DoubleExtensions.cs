using System;
namespace DwebBrowser.Helper;

public static class DoubleExtensions
{
	public static nfloat ToNFloat(this double self) => new nfloat(self / 255f);
}

