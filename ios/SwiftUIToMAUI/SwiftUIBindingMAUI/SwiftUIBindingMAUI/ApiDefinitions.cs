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

		// -(void)addNewWkWebViewWithWebView:(WKWebView * _Nonnull)webView;
		[Export ("addNewWkWebViewWithWebView:")]
		void AddNewWkWebViewWithWebView (WKWebView webView);

		// -(void)openWebViewUrlWithUrlString:(NSString * _Nonnull)urlString;
		[Export ("openWebViewUrlWithUrlString:")]
		void OpenWebViewUrlWithUrlString (string urlString);

		// +(void)webviewGeneratorCallbackWithCallback:(WKWebView * _Nonnull (^ _Nonnull)(WKWebViewConfiguration * _Nullable))callback;
		[Static]
		[Export ("webviewGeneratorCallbackWithCallback:")]
		void WebviewGeneratorCallbackWithCallback (Func<WKWebViewConfiguration, WKWebView> callback);
	}

	// @interface DownloadAppManager : NSObject
	[BaseType (typeof(NSObject))]
	[DisableDefaultCtor]
	interface DownloadAppManager
	{
		// @property (nonatomic, strong) UIView * _Nullable downloadView;
		[NullAllowed, Export ("downloadView", ArgumentSemantic.Strong)]
		UIView DownloadView { get; set; }

		// -(instancetype _Nonnull)initWithData:(NSData * _Nonnull)data downloadStatus:(NSInteger)downloadStatus __attribute__((objc_designated_initializer));
		[Export ("initWithData:downloadStatus:")]
		[DesignatedInitializer]
		NativeHandle Constructor (NSData data, nint downloadStatus);

		// -(void)onListenProgressWithProgress:(float)progress;
		[Export ("onListenProgressWithProgress:")]
		void OnListenProgressWithProgress (float progress);

		// -(void)clickDownloadActionWithCallback:(void (^ _Nonnull)(NSString * _Nonnull))callback;
		[Export ("clickDownloadActionWithCallback:")]
		void ClickDownloadActionWithCallback (Action<NSString> callback);

		// -(void)onDownloadChangeWithDownloadStatus:(NSInteger)downloadStatus;
		[Export ("onDownloadChangeWithDownloadStatus:")]
		void OnDownloadChangeWithDownloadStatus (nint downloadStatus);

		// -(void)onBackActionWithCallback:(void (^ _Nonnull)(void))callback;
		[Export ("onBackActionWithCallback:")]
		void OnBackActionWithCallback (Action callback);
	}

	// @interface HapticsHelper : NSObject
	[BaseType (typeof(NSObject))]
	interface HapticsHelper
	{
		// +(void)vibrateWithDurationArr:(NSArray<NSNumber *> * _Nonnull)durationArr;
		[Static]
		[Export ("vibrateWithDurationArr:")]
		void VibrateWithDurationArr (NSNumber[] durationArr);
	}
}
