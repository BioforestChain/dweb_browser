
namespace DwebBrowser.MicroService.Sys.Http.Net;

public class CreateDwebHttpServer
{
    public CreateDwebHttpServer()
    {
    }
}

public record DwebHttpServerOptions(int port = 80, string subdomain = "");

