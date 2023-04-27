using UIKit;
using DwebBrowser.Helper;

#nullable enable

namespace DwebBrowser.Base;

public abstract class BaseViewController : UIViewController
{
    public override void ViewDidLoad()
    {
        base.ViewDidLoad();
        InitData();
    }

    /// <summary>
    /// 舒适化数据，或者注册监听
    /// </summary>
    public virtual void InitData() { }


    public virtual void InitView() { }

    public event Signal? OnDestroyController;

    public override void ViewWillDisappear(bool animated)
    {
        base.ViewWillDisappear(animated);
        Task.Run(async () =>
        {
            await (OnDestroyController?.Emit()).ForAwait();
        });
    }
}

