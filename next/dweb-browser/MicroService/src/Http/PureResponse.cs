namespace DwebBrowser.MicroService.Http;

public record PureResponse(
    HttpStatusCode StatusCode = HttpStatusCode.OK,
    IpcHeaders? Headers = null,
    PureBody? Body = null,
    string? StatusText = null,
    string? Url = null
) : System.IDisposable
{
    public IpcHeaders Headers = Headers ?? new();
    public PureBody Body = Body ?? PureBody.Empty;
    public async Task<string> TextAsync() => await Body.ToUtf8StringAsync();
    public async Task<bool?> BooleanStrictAsync() => (await TextAsync()).ToBooleanStrictOrNull();
    public async Task<bool> BoolAsync() => (await BooleanStrictAsync()) ?? false;
    public async Task<int?> IntAsync() => (await TextAsync()).ToIntOrNull();
    public async Task<long?> LongAsync() => (await TextAsync()).ToLongOrNull();
    public async Task<float?> FloatAsync() => (await TextAsync()).ToFloatOrNull();
    public async Task<double?> DoubleAsync() => (await TextAsync()).ToDoubleOrNull();
    public async Task<T> JsonAsync<T>() => JsonSerializer.Deserialize<T>(await TextAsync()) ?? throw new JsonException("fail parse to json");

    public void Dispose()
    {
        if (Body is PureStreamBody streamBody)
        {
            streamBody.Dispose();
        }
    }
}

