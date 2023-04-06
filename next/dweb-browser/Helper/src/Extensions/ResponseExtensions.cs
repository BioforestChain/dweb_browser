using System.Net;

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
}

