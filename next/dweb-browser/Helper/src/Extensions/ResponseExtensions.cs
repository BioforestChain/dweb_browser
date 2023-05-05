using System.Net;
using System.Text.Json;

namespace DwebBrowser.Helper;

public static class ResponseExtensions
{
    public static HttpResponseMessage ToHttpResponseMessage(this HttpListenerResponse self)
    {
        // Create a new HttpResponseMessage object
        HttpResponseMessage responseMessage = new HttpResponseMessage();

        // Set the response status code
        responseMessage.StatusCode = (HttpStatusCode)self.StatusCode;

        // Copy the headers from the HttpListenerResponse to the HttpResponseMessage
        foreach (string headerName in self.Headers.AllKeys)
        {
            responseMessage.Headers.TryAddWithoutValidation(headerName, self.Headers[headerName]);
        }

        // Set the response content (if there is any)
        if (self.OutputStream is not null and var outputStream)
        {
            responseMessage.Content = new StreamContent(outputStream);
            if (self.ContentType is not null)
            {
                responseMessage.Content.Headers.ContentType = new(self.ContentType);
            }
            if (self.ContentEncoding is not null)
            {
                responseMessage.Content.Headers.ContentEncoding.Clear();
                responseMessage.Content.Headers.ContentEncoding.Add(self.ContentEncoding.WebName);
            }
        }

        // Optionally, you can set other properties of the HttpResponseMessage
        // such as content headers, request message, etc.

        // Now you can use the responseMessage object to return the response
        // using ASP.NET Web API or any other HTTP client library

        return responseMessage;
    }

    public static async Task<HttpListenerResponse> ToHttpListenerResponse(this HttpResponseMessage self, HttpListenerResponse res)
    {
        res.StatusCode = (int)self.StatusCode;

        foreach (var header in self.Headers)
        {
            res.Headers[header.Key] = string.Join(",", header.Value);
        }

        foreach (var header in self.Content.Headers)
        {
            res.Headers[header.Key] = string.Join(",", header.Value);
        }

        res.ContentType = self.Content.Headers.ContentType?.MediaType;
        res.ContentLength64 = self.Content.Headers.ContentLength > 0 ? (long)self.Content.Headers.ContentLength : 0;

        var stream = await self.Content.ReadAsStreamAsync();
        stream.CopyTo(res.OutputStream);

        return res;
    }

    public static HttpResponseMessage Ok(this HttpResponseMessage self) =>
        (int)self.StatusCode >= 400
            ? throw new Exception(Enum.GetName(self.StatusCode))
            : self;

    public static Task<string> TextAsync(this HttpResponseMessage self) =>
        self.Content.ReadAsStringAsync();

    public static Task<Stream> StreamAsync(this HttpResponseMessage self) =>
        self.Content.ReadAsStreamAsync();

    public static Stream Stream(this HttpResponseMessage self) =>
        self.Content.ReadAsStream();

    public static async Task<int> IntAsync(this HttpResponseMessage self) =>
        (await self.TextAsync()).ToIntOrNull()
        ?? throw new Exception("response content can't converter to int.");

    public static async Task<long> LongAsync(this HttpResponseMessage self) =>
        (await self.TextAsync()).ToLongOrNull()
        ?? throw new Exception("response content can't converter to long.");

    public static async Task<float> FloatAsync(this HttpResponseMessage self) =>
        (await self.TextAsync()).ToFloatOrNull()
        ?? throw new Exception("response content can't converter to float.");

    public static async Task<double> DoubleAsync(this HttpResponseMessage self) =>
        (await self.TextAsync()).ToDoubleOrNull()
        ?? throw new Exception("response content can't converter to double.");

    public static async Task<decimal> DecimalAsync(this HttpResponseMessage self) =>
        (await self.TextAsync()).ToDecimalOrNull()
        ?? throw new Exception("response content can't converter to decimal.");

    public static async Task<bool> BoolAsync(this HttpResponseMessage self) =>
        (await self.TextAsync()).ToBooleanStrictOrNull()
        ?? throw new Exception("response content can't converter to bool.");

    public static async Task<T> Json<T>(this HttpResponseMessage self) => self.Content switch
    {
        StringContent => JsonSerializer.Deserialize<T>(await self.TextAsync())!,
        StreamContent => JsonSerializer.Deserialize<T>(await self.StreamAsync())!,
        ByteArrayContent byteArrayContent => JsonSerializer.Deserialize<T>(await byteArrayContent.ReadAsByteArrayAsync())!,
        _ => throw new Exception("response content can't converter to generic type")
    };
}

