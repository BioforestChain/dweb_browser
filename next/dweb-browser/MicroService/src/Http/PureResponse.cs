using System;
namespace DwebBrowser.MicroService.Http;

public record PureResponse(
    int StatusCode,
    IpcHeaders Headers,
    PureBody Body,
    string? StatusText,
    string? Url
)
{
    public HttpResponseMessage ToHttpResponseMessage()
    {
        var response = new HttpResponseMessage((HttpStatusCode)StatusCode);
        response.ReasonPhrase = StatusText;

        switch (Body.Raw)
        {
            case string body:
                response.Content = new StringContent(body);
                break;
            case byte[] body:
                response.Content = new ByteArrayContent(body);
                break;
            case Stream body:
                response.Content = new StreamContent(body);
                break;
            default:
                throw new Exception(String.Format("invalid body to request: {0}", Body.Raw));
        }

        Headers.ToHttpMessage(response.Headers, response.Content.Headers);
        return response;
    }
}

