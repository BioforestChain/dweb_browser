using System;
using BrowserFramework;
using Foundation;
using ObjCRuntime;
using UIKit;
using WebKit;

namespace BrowserFramework
{
	// @interface BrowserManager : NSObject
	[BaseType (typeof(NSObject))]
	interface BrowserManager
	{
		// @property (nonatomic, strong) UIView * _Nullable swiftView;
		[NullAllowed, Export ("swiftView", ArgumentSemantic.Strong)]
		UIView SwiftView { get; set; }

		// @property (copy, nonatomic) NSArray<WKWebView *> * _Nullable webViewList;
		[NullAllowed, Export ("webViewList", ArgumentSemantic.Copy)]
		WKWebView[] WebViewList { get; set; }

		// @property (copy, nonatomic) void (^ _Nullable)(NSArray<WKWebView *> * _Nonnull) onValueChanged;
		[NullAllowed, Export ("onValueChanged", ArgumentSemantic.Copy)]
		Action<NSArray<WKWebView>> OnValueChanged { get; set; }
	}
}
