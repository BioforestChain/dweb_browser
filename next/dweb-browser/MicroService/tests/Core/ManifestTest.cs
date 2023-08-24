using DwebBrowser.MicroService.Core;

namespace DwebBrowser.MicroServiceTests.Core;

public class ManifestTest
{
	[Fact]
	public void JsonSerializerManifestTest()
	{
		var manifest = new MicroModuleManifest(
			"test.sys.dweb",
			new IpcSupportProtocols { Cbor = true, Raw = false, Protobuf = true },
			new List<string> { "dweb:open" },
			new List<MicroModuleCategory> { MicroModuleCategory.Application, MicroModuleCategory.Books },
			"test app",
			"1.0.1",
			TextDirectionType.Auto,
			"zh-CN",
			"test",
			"test app to manifest",
			new List<ImageResource> { new ImageResource("https://dweb.waterbang.top/icons.png") },
			new List<ImageResource> { new ImageResource("https://dweb.waterbang.top/screenshots.png") },
			DisplayModeType.Browser,
			OrientationType.Landscape,
			"#ff0035",
			"#00ff00",
			new List<ShortcutItem>() { new ShortcutItem("item", "https://dweb.waterbang.top", icons: new List<ImageResource>
			{
				new ImageResource("https://dweb.waterbang.top/shortcutItem.png")
			}) }
		);

		var json = manifest.ToJson();
		Debug.WriteLine(json);
		var fromJson = MicroModuleManifest.FromJson(json);

		if (fromJson is not null)
		{
            Debug.WriteLine("mmid: " + fromJson.Mmid);
            Debug.WriteLine("IpcSupportProtocols cbor: " +
				fromJson.IpcSupportProtocols.Cbor + " raw: " +
				fromJson.IpcSupportProtocols.Raw + " protobuf: " +
				fromJson.IpcSupportProtocols.Protobuf);
			foreach (var link in fromJson.Dweb_deeplinks)
			{
                Debug.WriteLine("dweb_deepLinks: " + link);
            }
			foreach (var category in fromJson.Categories)
			{
                Debug.WriteLine("Categories: " + category);
            }
            Debug.WriteLine("name: " + fromJson.Name);
            Debug.WriteLine("version: " + fromJson.Version);
            Debug.WriteLine("dir: " + fromJson.Dir);
            Debug.WriteLine("lang: " + fromJson.Lang);
            Debug.WriteLine("short_name: " + fromJson.ShortName);
            Debug.WriteLine("description: " + fromJson.Description);
			if (fromJson.Icons is not null)
			{
                foreach (var icon in fromJson.Icons)
                {
                    Debug.WriteLine("icons: " + icon.Src);
                }
            }
			if (fromJson.Screenshots is not null)
			{
                foreach (var screenshot in fromJson.Screenshots)
                {
                    Debug.WriteLine("Screenshots: " + screenshot.Src);
                }
            }
            Debug.WriteLine("display: " + fromJson.Display);
            Debug.WriteLine("orientation: " + fromJson.Orientation);
            Debug.WriteLine("theme_color: " + fromJson.ThemeColor);
            Debug.WriteLine("background_color: " + fromJson.BackgroundColor);

			if (fromJson.Shortcuts is not null)
			{
				foreach (var shortcut in fromJson.Shortcuts)
				{
                    Debug.WriteLine("Shortcuts name: " + shortcut.name);
                    Debug.WriteLine("Shortcuts url: " + shortcut.url);
					if (shortcut.icons is not null)
					{
                        foreach (var icon in shortcut.icons)
                        {
                            Debug.WriteLine("Shortcuts icons: " + icon.Src);
                        }
                    }
                }
            }
            
        }

        //Debug.WriteLine("manifest: " + manifest.GetHashCode());
        //Debug.WriteLine("fromJson: " + fromJson?.GetHashCode());
        //Assert.Equal(manifest, fromJson);

        Debug.WriteLine("manifest Mmid " + manifest.Mmid.GetHashCode());
        Debug.WriteLine("manifest IpcSupportProtocols " + manifest.IpcSupportProtocols.GetHashCode());
        Debug.WriteLine("manifest Dweb_deeplinks " + manifest.Dweb_deeplinks.GetHashCode());
        Debug.WriteLine("manifest Categories " + manifest.Categories.GetHashCode());
        Debug.WriteLine("manifest Name " + manifest.Name.GetHashCode());
        Debug.WriteLine("manifest Version " + (manifest.Version?.GetHashCode() ?? 0));
        Debug.WriteLine("manifest Dir " + (manifest.Dir?.GetHashCode() ?? 0));
        Debug.WriteLine("manifest Lang " + (manifest.Lang?.GetHashCode() ?? 0));
        Debug.WriteLine("manifest ShortName " + (manifest.ShortName?.GetHashCode() ?? 0));
        Debug.WriteLine("manifest Description " + (manifest.Description?.GetHashCode() ?? 0));
        Debug.WriteLine("manifest Icons " + (manifest.Icons?.GetHashCode() ?? 0));
        Debug.WriteLine("manifest Screenshots " + (manifest.Screenshots?.GetHashCode() ?? 0));
        Debug.WriteLine("manifest Display " + (manifest.Display?.GetHashCode() ?? 0));
        Debug.WriteLine("manifest Orientation " + (manifest.Orientation?.GetHashCode() ?? 0));
        Debug.WriteLine("manifest ThemeColor " + (manifest.ThemeColor?.GetHashCode() ?? 0));
        Debug.WriteLine("manifest BackgroundColor " + (manifest.BackgroundColor?.GetHashCode() ?? 0));
        Debug.WriteLine("manifest Shortcuts " + (manifest.Shortcuts?.GetHashCode() ?? 0));


        Debug.WriteLine("fromJson Mmid " + fromJson.Mmid.GetHashCode());
        Debug.WriteLine("fromJson IpcSupportProtocols " + fromJson.IpcSupportProtocols.GetHashCode());
        Debug.WriteLine("fromJson Dweb_deeplinks " + fromJson.Dweb_deeplinks.GetHashCode());
        Debug.WriteLine("fromJson Categories " + fromJson.Categories.GetHashCode());
        Debug.WriteLine("fromJson Name " + fromJson.Name.GetHashCode());
        Debug.WriteLine("fromJson Version " + (fromJson.Version?.GetHashCode() ?? 0));
        Debug.WriteLine("fromJson Dir " + (fromJson.Dir?.GetHashCode() ?? 0));
        Debug.WriteLine("fromJson Lang " + (fromJson.Lang?.GetHashCode() ?? 0));
        Debug.WriteLine("fromJson ShortName " + (fromJson.ShortName?.GetHashCode() ?? 0));
        Debug.WriteLine("fromJson Description " + (fromJson.Description?.GetHashCode() ?? 0));
        Debug.WriteLine("fromJson Icons " + (fromJson.Icons?.GetHashCode() ?? 0));
        Debug.WriteLine("fromJson Screenshots " + (fromJson.Screenshots?.GetHashCode() ?? 0));
        Debug.WriteLine("fromJson Display " + (fromJson.Display?.GetHashCode() ?? 0));
        Debug.WriteLine("fromJson Orientation " + (fromJson.Orientation?.GetHashCode() ?? 0));
        Debug.WriteLine("fromJson ThemeColor " + (fromJson.ThemeColor?.GetHashCode() ?? 0));
        Debug.WriteLine("fromJson BackgroundColor " + (fromJson.BackgroundColor?.GetHashCode() ?? 0));
        Debug.WriteLine("fromJson Shortcuts " + (fromJson.Shortcuts?.GetHashCode() ?? 0));

        /// 得出结论：List<T> 无法通过GetHashCode来判断值是否相等
    }
}

