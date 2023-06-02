using UniformTypeIdentifiers;
using DwebBrowser.Helper;

namespace DwebBrowserIOSUnitTest.Tests;

public static class UTTypesTest
{
	static readonly Debugger Console = new("UTTypesTest");

	public static void UTTypes_ToString()
	{
		// public.text
		Console.Log("UTTypes", "text: {0}", UTTypes.Text.ToString());
		// public.image
		Console.Log("UTTypes", "image: {0}", UTTypes.Image.ToString());
		// public.url
		Console.Log("UTTypes", "url: {0}", UTTypes.Url.ToString());
	}
}

