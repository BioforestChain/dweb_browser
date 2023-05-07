namespace DwebBrowser.MicroService.Http;

public record PureRequest(
    string Url,
    IpcMethod Method,
    IpcHeaders Headers,
    PureBody Body
)
{
    public HttpRequestMessage ToHttpRequestMessage()
    {
        var repuest = new HttpRequestMessage(Method.ToHttpMethod(), Url);

        switch (Body.Raw)
        {
            case string body:
                repuest.Content = new StringContent(body);
                break;
            case byte[] body:
                repuest.Content = new ByteArrayContent(body);
                break;
            case Stream body:
                repuest.Content = new StreamContent(body);
                break;
            case null:
                break;
            default:
                throw new Exception(String.Format("invalid body to request: {0}", Body.Raw));
        }

        Headers.ToHttpMessage(repuest.Headers, repuest.Content?.Headers);
        return repuest;
    }
}
