using AVFoundation;

namespace DwebBrowser.MicroService.Browser.NativeUI.Torch;

public class TorchNMM : NativeMicroModule
{
    public TorchNMM() : base("torch.nativeui.browser.dweb", "torch")
    {
    }

    static readonly Debugger Console = new("TorchNMM");

    private readonly AVCaptureDevice _device = AVCaptureDevice.GetDefaultDevice(AVMediaTypes.Video);

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/toggleTorch", async (request, _) =>
        {
            _toggleTorch();
            return true;
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/torchState", async (request, _) =>
        {
            return _torchState();
        });
    }

    private void _toggleTorch()
    {
        if (_device.HasTorch)
        {
            if (_device.LockForConfiguration(out var error))
            {
                _device.TorchMode = _device.TorchActive ? AVCaptureTorchMode.Off : AVCaptureTorchMode.On;
            }

            if (error is not null)
            {
                Console.Error("toggleTorch", error.LocalizedDescription);
            }

            _device.UnlockForConfiguration();
        }
    }

    private bool _torchState() => _device.TorchActive;
}

