using Vision;
using Foundation;

#nullable enable

namespace DwebBrowser.Platforms.iOS.MicroModule.Plugin.Barcode;

public class ScanningManager
{
    static Debugger Console = new("ScanningManager");

    private NSData _data { get; init; }
    private PromiseOut<string[]> _scanningPo { get; init; }

    public ScanningManager(byte[] bytes, PromiseOut<string[]> po)
    {
        _data = NSData.FromArray(bytes);
        _scanningPo = po;
    }

    private LazyBox<VNDetectBarcodesRequest> _lazyBarcodeDetectionRequest = new();
    private VNDetectBarcodesRequest _barcodeDetectionRequest
    {
        get => _lazyBarcodeDetectionRequest.GetOrPut(() =>
        {
            var request = new VNDetectBarcodesRequest(_handleDetectedBarcodes);
            request.Symbologies = new VNBarcodeSymbology[]
            {
                VNBarcodeSymbology.QR,
                VNBarcodeSymbology.Aztec,
                VNBarcodeSymbology.Upce
            };

            return request;
        });
    }

    private static ScanningManager? _scanningManager;

    public static void Start(byte[] bytes, PromiseOut<string[]> po)
    {
        _scanningManager = new ScanningManager(bytes, po);
        _scanningManager._start();
    }

    public static void Stop()
    {
        if (_scanningManager is not null)
        {
            _scanningManager._stop();
            _scanningManager = null;
        }
    }

    private void _start()
    {
        var handler = new VNImageRequestHandler(imageData: _data, imageOptions: new());
        var _bool = handler.Perform(new VNRequest[] { _barcodeDetectionRequest }, out var err);

        if (!_bool && err is not null)
        {
            Console.Error("perform", err.LocalizedDescription);
            _scanningPo.Reject(err.LocalizedDescription);
            return;
        }
    }

    private void _stop()
    {
        _barcodeDetectionRequest.Cancel();
    }

    private async void _handleDetectedBarcodes(VNRequest request, NSError error)
    {
        if (error is not null)
        {
            Console.Error("handleDetectedBarcodes", error.LocalizedDescription);
            _scanningPo.Reject(error.LocalizedDescription);
            return;
        }

        var results = await MainThread.InvokeOnMainThreadAsync(() =>
        {
            string[] parseResults = Array.Empty<string>();
            var results = request.GetResults<VNBarcodeObservation>();
            foreach (var result in results)
            {
                var parseResult = result.PayloadStringValue;
                if (parseResult is not null)
                {
                    parseResults.Append(parseResult);
                }
            }

            return parseResults;
        });

        _scanningPo.Resolve(results);
    }
}

