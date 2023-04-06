using System.Net;
namespace DwebBrowser.Helper;

public static class RequestExtensions
{
    public static HttpRequestMessage ToHttpRequestMessage(this HttpListenerRequest self)
    {
        // Create a new HttpRequestMessage object
        HttpRequestMessage requestMessage = new HttpRequestMessage();

        // Set the request method
        requestMessage.Method = new HttpMethod(self.HttpMethod);

        // Set the request URI
        requestMessage.RequestUri = new Uri(self.Url.ToString());

        // Copy the headers from the HttpListenerRequest to the HttpRequestMessage
        foreach (string headerName in self.Headers.AllKeys)
        {
            requestMessage.Headers.TryAddWithoutValidation(headerName, self.Headers[headerName]);
        }

        // Set the request content (if there is any)
        if (self.HasEntityBody)
        {
            requestMessage.Content = new StreamContent(self.InputStream);
        }

        // Optionally, you can set other properties of the HttpRequestMessage
        // such as the request version, content headers, etc.

        // Now you can use the requestMessage object to send the request
        // using HttpClient or any other HTTP client library
        return requestMessage;
    }
}

