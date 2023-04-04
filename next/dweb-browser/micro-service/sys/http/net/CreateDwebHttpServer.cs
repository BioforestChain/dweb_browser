
namespace micro_service.sys.http.net;

public class CreateDwebHttpServer
{
    public CreateDwebHttpServer()
    {
    }
}

public record DwebHttpServerOptions(int port = 80, string subdomain = "");

