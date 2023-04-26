using System.Net;
namespace DwebBrowser.Helper;

public static class RequestExtensions
{
    public static HttpRequestMessage ToHttpRequestMessage(this HttpListenerRequest self)
    {
        // Create a new HttpRequestMessage object
        HttpRequestMessage requestMessage = new HttpRequestMessage(
            // Set the request method
            new HttpMethod(self.HttpMethod),
            // Set the request URI
            self.Url
         );

        // Copy the headers from the HttpListenerRequest to the HttpRequestMessage
        foreach (var headerName in self.Headers.AllKeys)
        {
            if (headerName is not null)
            {
                requestMessage.Headers.TryAddWithoutValidation(headerName, self.Headers[headerName]);
            }
        }

        // Set the request content (if there is any)
        if (self.HasEntityBody)
        {
            requestMessage.Content = new StreamContent(self.InputStream);
            if (self.ContentType is not null)
            {
                requestMessage.Content.Headers.ContentType = new(self.ContentType);
            }
            requestMessage.Content.Headers.ContentEncoding.Clear();
            requestMessage.Content.Headers.ContentEncoding.Add(self.ContentEncoding.WebName);
        }

        // Optionally, you can set other properties of the HttpRequestMessage
        // such as the request version, content headers, etc.

        // Now you can use the requestMessage object to send the request
        // using HttpClient or any other HTTP client library
        return requestMessage;
    }

    private static T? QueryValidateOptional<T>(this HttpRequestMessage self, string name, out bool isDefault)
    {
        isDefault = true;
        if (self.RequestUri is null) return default(T);

        try
        {
            var query = self.RequestUri.GetQuery(name);

            if (query is not null)
            {
                isDefault = false;
                return (T)Convert.ChangeType(query, typeof(T));
            }
            else
            {
                return default(T);
            }
        }
        catch
        {
            return default(T);
        }
    }

    public static T? QueryValidate<T>(this HttpRequestMessage self, string name, bool isRequired = true)
    {
        var result = QueryValidateOptional<T>(self, name, out bool isDefault);

        if (isRequired && isDefault)
        {
            throw new Exception("required query is null");
        }
        else
        {
            return result;
        }
    }
}

