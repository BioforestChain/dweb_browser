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
        if (self.OutputStream != null)
        {
            responseMessage.Content = new StreamContent(self.OutputStream);
        }

        // Optionally, you can set other properties of the HttpResponseMessage
        // such as content headers, request message, etc.

        // Now you can use the responseMessage object to return the response
        // using ASP.NET Web API or any other HTTP client library

        return responseMessage;
    }

    public static HttpListenerResponse ToHttpListenerResponse(this HttpResponseMessage self, HttpListenerResponse res)
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

        using (var stream = self.Content.ReadAsStreamAsync().Result)
        {
            stream.CopyTo(res.OutputStream);
        }

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

    public static async Task<int?> IntAsync(this HttpResponseMessage self) =>
        (await self.TextAsync()).ToIntOrNull();

    public static async Task<long?> LongAsync(this HttpResponseMessage self) =>
        (await self.TextAsync()).ToLongOrNull();

    public static async Task<float?> FloatAsync(this HttpResponseMessage self) =>
        (await self.TextAsync()).ToFloatOrNull();

    public static async Task<double?> DoubleAsync(this HttpResponseMessage self) =>
        (await self.TextAsync()).ToDoubleOrNull();

    public static async Task<decimal?> DecimalAsync(this HttpResponseMessage self) =>
        (await self.TextAsync()).ToDecimalOrNull();

    public static async Task<bool?> BoolAsync(this HttpResponseMessage self) =>
        (await self.TextAsync()).ToBooleanStrictOrNull();

    public static async Task<T?> Json<T>(this HttpResponseMessage self) =>
        JsonSerializer.Deserialize<T>(await self.StreamAsync());
}

