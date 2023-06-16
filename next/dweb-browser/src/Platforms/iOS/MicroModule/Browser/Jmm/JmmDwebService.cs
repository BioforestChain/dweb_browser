using System.Collections.Concurrent;
using System.IO.Compression;

#nullable enable

namespace DwebBrowser.MicroService.Browser.Jmm;

public static class JmmDwebService
{
    static Debugger Console = new("JmmDwebService");
    private static ConcurrentDictionary<Mmid, JmmDownload> s_downloadMap = new();
    private static ConcurrentQueue<Mmid> s_downloadQueue = new();

    private const string DOWNLOAD = "dwebDownloads";
    private const string DWEB_APP = "dwebApps";

    /// <summary>
    /// 下载目录
    /// </summary>
    public static readonly string DOWNLOAD_DIR = Path.Join(
        PathHelper.GetIOSDocumentDirectory(), DOWNLOAD);

    /// <summary>
    /// dweb应用目录
    /// </summary>
    public static readonly string DWEB_APP_DIR = Path.Join(
        PathHelper.GetIOSDocumentDirectory(), DWEB_APP);

    static event Signal? _onStart;

    static JmmDwebService()
    {
        if (!Directory.Exists(DOWNLOAD_DIR))
        {
            Directory.CreateDirectory(DOWNLOAD_DIR);
        }
        if (!Directory.Exists(DWEB_APP_DIR))
        {
            Directory.CreateDirectory(DWEB_APP_DIR);
        }

        _onStart += async (_) =>
        {
            await foreach (var result in DownloadEnumerableAsync())
            {
                Console.Log("DownloadEnumerableAsync", "installed {0}", result);
            }
        };
    }

    public static async void Start()
    {
        await _onStart.Emit();
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

    public static void Add(JmmDownload jmmDownload)
    {
        if (!s_downloadMap.ContainsKey(jmmDownload.JmmMetadata.Id))
        {
            s_downloadMap.TryAdd(jmmDownload.JmmMetadata.Id, jmmDownload);
            s_downloadQueue.Enqueue(jmmDownload.JmmMetadata.Id);
        }
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
    public static void UnInstall(Mmid mmid)
    {
        Directory.Delete(Path.Join(DWEB_APP_DIR, mmid), true);
    }
}

public class JmmDownload
{
    static Debugger Console = new("JmmDownload");

    private const long CHUNK_SIZE = 8 * 1024;

    private readonly State<bool> _isPause = new(true);
    private readonly State<long> _readBytes = new(0L);
    private readonly State<DownloadStatus> _downloadStatus = new(DownloadStatus.IDLE);
    private string _downloadFile { get; init; }
    private long _totalSize = 0L;

    public PromiseOut<bool> DownloadPo = new();
    public JmmMetadata JmmMetadata { get; init; }

    public JmmDownload(
        JmmMetadata jmmMetadata,
        Action<nint> onDownloadStatusChange,
        Action<float> onDownloadProgressChange)
    {
        JmmMetadata = jmmMetadata;
        var url = new URL(jmmMetadata.BundleUrl);
        _downloadFile = Path.Join(JmmDwebService.DOWNLOAD_DIR, url.Path);

        _readBytes.OnChange += async (value, _, _) =>
        {
            // progress 进度汇报
            onDownloadProgressChange((float)(value * 0.8 / _totalSize));
        };
        _downloadStatus.OnChange += async (value, _, _) =>
        {
            switch (value)
            {
                case DownloadStatus.DownloadComplete:
                    onDownloadProgressChange((float)0.8);
                    CompressZip(onDownloadProgressChange);
                    break;
                case DownloadStatus.Installed:
                    onDownloadStatusChange((nint)DownloadStatus.Installed);
                    DownloadPo.Resolve(true);
                    break;
                case DownloadStatus.Fail:
                    onDownloadStatusChange((nint)DownloadStatus.Fail);
                    DownloadPo.Resolve(false);
                    break;
                case DownloadStatus.Cancel:
                    onDownloadStatusChange((nint)DownloadStatus.Cancel);
                    DownloadPo.Resolve(false);
                    break;
            }
        };
    }

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

        using var client = new HttpClient();
        using var response = await client.GetAsync(JmmMetadata.BundleUrl, HttpCompletionOption.ResponseHeadersRead);
        using var content = response.Content;

        _totalSize = content.Headers.ContentLength ?? 0L;
    }

    /// <summary>
    /// 下载文件
    /// </summary>
    /// <returns></returns>
    public async Task DownloadFile()
    {
        try
        {
            await _getTotalSizeAsync();

            using var client = new HttpClient();
            //using var request = new HttpRequestMessage { RequestUri = _url.Uri };
            using var response = await client.GetAsync(JmmMetadata.BundleUrl, HttpCompletionOption.ResponseHeadersRead);
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

            var isRunning = false;
            var ms = new MemoryStream();
            stream.CopyTo(ms);
            ms.Position = 0;

            _isPause.OnChange += async (value, _, _) =>
            {
                if (value)
                {
                    Console.Log("DownloadFile", "暂停下载");
                    return;
                }
                else if (_readBytes.Get() >= _totalSize)
                {
                    return;
                }
                else if (isRunning)
                {
                    return;
                }
                isRunning = true;

                using var fileStream = new FileStream(_downloadFile, FileMode.Append, FileAccess.Write);

                do
                {
                    var buffer = new byte[CHUNK_SIZE];
                    var bytesRead = await ms.ReadAsync(buffer);
                    if (bytesRead == 0)
                    {
                        _downloadStatus.Set(DownloadStatus.DownloadComplete);
                        ms.Dispose();
                        ms = null;
                        break;
                    }

                    await fileStream.WriteAsync(buffer.AsMemory(0, bytesRead));
                    var readBytes = _readBytes.Get();
                    _readBytes.Set(readBytes + bytesRead);
                    Console.Log("DownloadFile", "readBytes: {0}, totalBytes: {1}", _readBytes.Get(), _totalSize);
                } while (!_isPause.Get());
                isRunning = false;
            };
            _isPause.Set(false);
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
    public void CompressZip(Action<float> onDownloadProgressChange)
    {
        _ = Task.Run(() =>
        {
            ZipFile.ExtractToDirectory(_downloadFile, JmmDwebService.DWEB_APP_DIR);
            Console.Log("CompressZip", JmmDwebService.DWEB_APP_DIR);
            File.Delete(_downloadFile);
            onDownloadProgressChange((float)1.0);
            _downloadStatus.Set(DownloadStatus.Installed);
            JmmMetadataDB.AddJmmMetadata(JmmMetadata.Id, JmmMetadata);
            Console.Log("CompressZip", "success!!!");
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
