using System;
using WebKit;

namespace SwiftUIMAUILibrary;

public class BrowserControl : View
{

    public static readonly BindableProperty ValueProperty = BindableProperty.Create(nameof(Value), typeof(WKWebView[]), typeof(BrowserControl), default);
    public WKWebView[] Value {
        get => (WKWebView[])GetValue(ValueProperty);
        set => SetValue(ValueProperty, value);
    }
}

