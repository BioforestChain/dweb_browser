using System.Collections.Concurrent;
using System.IO.Compression;
using DwebBrowser.Helper;

#nullable enable

namespace DwebBrowser.MicroService.Browser.Jmm;

public static class JmmDwebService
{
    static Debugger Console = new("JmmDwebService");
    private static ConcurrentDictionary<Mmid, JmmDownload> s_downloadMap = new();
    private static ConcurrentQueue<Mmid> s_downloadQueue = new();

    static event Signal? _onStart;

    static JmmDwebService()
    {
        _onStart += async (_) =>
        {
            await foreach (var result in DownloadEnumerableAsync())
            {
                Console.Log("DownloadEnumerableAsync", "installed {0}", result);
            }
        };
    }

    private static Mutex _mutex = new();
    public static async void Start()
    {
        _mutex.WaitOne();
        await _onStart.Emit();
        _mutex.ReleaseMutex();
    }

    static async IAsyncEnumerable<bool> DownloadEnumerableAsync()
    {
        if (s_downloadQueue.TryDequeue(out var mmid))
        {
            if (s_downloadMap.TryGetValue(mmid, out var jmmDownload))
            {
                await jmmDownload.DownloadFile();
            }

            yield return await Remove(jmmDownload);
        }
    }

    public static JmmDownload Add(JmmMetadata jmmMetadata, Action<nint> onDownloadStatusChange, Action<float> onDownloadProgressChange)
    {
        if (!s_downloadMap.TryGetValue(jmmMetadata.Id, out var jmmDownload))
        {
            jmmDownload = new(jmmMetadata);
        }
        s_downloadMap.TryAdd(jmmDownload.JmmMetadata.Id, jmmDownload);
        s_downloadQueue.Enqueue(jmmDownload.JmmMetadata.Id);
        jmmDownload.OnDownloadProgressChange += onDownloadProgressChange;
        jmmDownload.OnDownloadStatusChange += onDownloadStatusChange;
        return jmmDownload;
    }

    public static async Task<bool> Remove(JmmDownload? jmmDownload)
    {
        var _bool = false;

        await (jmmDownload?.DownloadPo.WaitPromiseAsync()).ForAwait();

        if (jmmDownload is not null)
        {
            _bool = s_downloadMap.TryRemove(new(jmmDownload.JmmMetadata.Id, jmmDownload));
        }

        return _bool;
    }

    public static Unit UpdateDownloadControlStatus(Mmid mmid, DownloadControlStatus downloadControlStatus)
    {
        var jmmDownload = s_downloadMap.GetValueOrDefault(mmid);

        if (jmmDownload is not null)
        {
            jmmDownload.UpdateDownloadStatus(downloadControlStatus);
        }

        return unit;
    }

    /// <summary>
    /// 卸载应用
    /// </summary>
    /// <param name="mmid">JmmMetadata id</param>
    public static void UnInstall(JmmMetadata jmmMetadata)
    {
        Directory.Delete(JsMicroModule.GetInstallPath(jmmMetadata), true);
    }
}

public class JmmDownload
{
    static Debugger Console = new("JmmDownload");

    private const string DOWNLOAD = "dwebDownloads";

    /// <summary>
    /// 下载目录
    /// </summary>
    static readonly string DOWNLOAD_DIR = Path.Join(
      PathHelper.GetIOSDocumentDirectory(), DOWNLOAD);

    static JmmDownload()
    {
        if (!Directory.Exists(DOWNLOAD_DIR))
        {
            Directory.CreateDirectory(DOWNLOAD_DIR);
        }
    }

    private readonly State<bool> _isPause = new(true);
    private readonly State<long> _readBytes = new(0L);
    private readonly State<DownloadStatus> _downloadStatus = new(DownloadStatus.IDLE);
    private string _downloadFile { get; init; }
    private long _totalSize = 0L;

    public PromiseOut<bool> DownloadPo = new();
    public JmmMetadata JmmMetadata { get; init; }

    public JmmDownload(JmmMetadata jmmMetadata)
    {
        JmmMetadata = jmmMetadata;
        var url = new URL(jmmMetadata.BundleUrl);
        _downloadFile = Path.Join(DOWNLOAD_DIR, url.Path);

        _readBytes.OnChange += async (value, _, _) =>
        {
            // progress 进度汇报
            OnDownloadProgressChange?.Invoke((float)(value * 0.8 / _totalSize));
        };
        _downloadStatus.OnChange += async (value, _, _) =>
        {
            switch (value)
            {
                case DownloadStatus.DownloadComplete:
                    OnDownloadProgressChange?.Invoke((float)0.8);
                    UnCompressZip();
                    break;
                case DownloadStatus.Installed:
                    OnDownloadStatusChange?.Invoke((nint)DownloadStatus.Installed);
                    DownloadPo.Resolve(true);
                    break;
                case DownloadStatus.Fail:
                    OnDownloadStatusChange?.Invoke((nint)DownloadStatus.Fail);
                    DownloadPo.Resolve(false);
                    break;
                case DownloadStatus.Cancel:
                    OnDownloadStatusChange?.Invoke((nint)DownloadStatus.Cancel);
                    DownloadPo.Resolve(false);
                    break;
            }
        };
    }

    public event Action<nint>? OnDownloadStatusChange = null;
    public event Action<float>? OnDownloadProgressChange = null;


    private async Task _getTotalSizeAsync()
    {
        if (_totalSize is not 0L)
        {
            return;
        }

        var size = JmmMetadata.BundleSize;

        if (size is not 0L)
        {
            _totalSize = size;
            return;
        }

        using var response = await httpClient.GetAsync(JmmMetadata.BundleUrl, HttpCompletionOption.ResponseHeadersRead);
        using var content = response.Content;

        _totalSize = content.Headers.ContentLength ?? 0L;
    }
    static HttpClient httpClient = new HttpClient().Also(it =>
    {
        /// 默认禁用缓存
        it.DefaultRequestHeaders.CacheControl = new() { NoCache = true };
    });

    /// <summary>
    /// 下载文件
    /// </summary>
    /// <returns></returns>
    public async Task DownloadFile()
    {
        try
        {
            await _getTotalSizeAsync();

            //using var request = new HttpRequestMessage { RequestUri = _url.Uri };
            using var response = await httpClient.GetAsync(JmmMetadata.BundleUrl, HttpCompletionOption.ResponseHeadersRead);
            //using var response = await client.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
            using var content = response.Content;
            using var stream = await content.ReadAsStreamAsync();

            if (_totalSize is 0L)
            {
                _totalSize = stream.Length;

                if (_totalSize is 0L)
                {
                    var message = "Can't get download app size";
                    Console.Warn("_getTotalSizeAsync", message);
                    throw new HttpRequestException(message);
                }
            }
            _isPause.Set(false);


            using var fileStream = new FileStream(_downloadFile, FileMode.Append, FileAccess.Write);

            /// 等待开始的信号
            await _isPause.Until((v) => !v);

            _readBytes.Set(1L);

            await foreach (var buffer in stream.ReadBytesStream())
            {
                await fileStream.WriteAsync(buffer);
                _readBytes.Update(byteLen => byteLen + buffer.LongLength);
                Console.Log("DownloadFile", "readBytes: {0}, totalBytes: {1}", _readBytes.Get(), _totalSize);

                /// 如果暂停，等待信号恢复
                await _isPause.Until((v) => !v);
                /// TODO 页面销毁时，要销毁这些异步等待
            }
            _downloadStatus.Set(DownloadStatus.DownloadComplete);

        }
        catch (Exception e)
        {
            Console.Error("DownloadFile", "Exception: {0}", e.Message);
            Console.Error("DownloadFile", "Exception: {0}", e.StackTrace);
            _downloadStatus.Set(DownloadStatus.Fail);
        }
    }

    /// <summary>
    /// 解压
    /// </summary>
    public void UnCompressZip()
    {
        _ = Task.Run(() =>
        {
            var outputDir = JsMicroModule.GetInstallPath(JmmMetadata);
            Console.Log("UnCompressZip", outputDir);

            if (!Directory.Exists(outputDir))
            {
                Directory.CreateDirectory(outputDir);
            }

            ZipFile.ExtractToDirectory(_downloadFile, outputDir);
            File.Delete(_downloadFile);
            foreach (var info in JsMicroModule.GetAllVersions(JmmMetadata.Id))
            {
                if (info.Version != JmmMetadata.Version)
                {
                    Directory.Delete(info.InstallPath, true);
                }
            }
            OnDownloadProgressChange?.Invoke((float)1.0);
            _downloadStatus.Set(DownloadStatus.Installed);
            JmmMetadataDB.AddJmmMetadata(JmmMetadata.Id, JmmMetadata);
            Console.Log("UnCompressZip", "success!!!");
        }).NoThrow();
    }

    public void UpdateDownloadStatus(DownloadControlStatus downloadControlStatus)
    {
        switch (downloadControlStatus)
        {
            case DownloadControlStatus.Pause:
                _isPause.Set(true);
                break;
            case DownloadControlStatus.Resume:
                _isPause.Set(false);
                break;
            case DownloadControlStatus.Cancel:
                _isPause.Set(true);
                _downloadStatus.Set(DownloadStatus.Cancel);

                if (File.Exists(_downloadFile))
                {
                    File.Delete(_downloadFile);
                }

                break;
        }
    }
}

public enum DownloadStatus : int
{
    IDLE,
    Downloading,
    DownloadComplete,
    Pause,
    Installed,
    Fail,
    Cancel,
    NewVersion
}

public enum DownloadControlStatus
{
    Pause,
    Resume,
    Cancel
}
