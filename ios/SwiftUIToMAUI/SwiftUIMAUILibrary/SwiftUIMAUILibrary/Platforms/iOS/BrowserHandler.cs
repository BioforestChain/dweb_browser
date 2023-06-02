using System;
using Microsoft.Maui.Graphics.Platform;
using UIKit;
using BrowserFramework;
using Microsoft.Maui.Handlers;
using Foundation;
using WebKit;

namespace SwiftUIMAUILibrary;

public partial class BrowserHandler : ViewHandler<BrowserControl, UIView>
{
	private BrowserManager wrapper { get; set; }

	public BrowserHandler() : base(PropertyMapper)
	{

	}

	private static IPropertyMapper<BrowserControl, BrowserHandler> PropertyMapper = new PropertyMapper<BrowserControl, BrowserHandler>(ViewMapper)
	{
		[nameof(BrowserControl.Value)] = MapValue,
	};

	private static void MapValue(BrowserHandler handler, BrowserControl control)
	{
		if (handler == null || handler.wrapper.WebViewList == control.Value)
			return;

		handler.wrapper.WebViewList = control.Value;
	}

    protected override UIView CreatePlatformView()
    {
		wrapper = new BrowserManager();
		//wrapper.OnValueChanged = OnValueChanged;
		return wrapper.SwiftView;
    }

	//private void OnValueChanged(NSArray<WKWebView> value) => VirtualView.Value = value;
}

