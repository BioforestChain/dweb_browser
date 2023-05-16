using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace DwebBrowser.MicroService.Sys.Http.Net;

public static class PortHelper
{
    public static (bool, int?) IsPortInUse(int? try_port)
    {
        int port = try_port ?? 0;
        int tryTimes = 100;
        while (tryTimes-- > 0)
        {

            try { 
                TcpListener tcpListen = new TcpListener(IPAddress.Parse("127.0.0.1"), port);
                tcpListen.Start();
                port = ((IPEndPoint)tcpListen.LocalEndpoint).Port;
                tcpListen.Stop();

                return (false, port);
            }
            catch
            {
                if(port is 0) {
                    throw;
                }
                port++;
            }
        }
        throw new Exception("No found usebale ip port");
    }

    public static int FindPort(int[] favorite_ports)
    {
        foreach (var favorite_port in favorite_ports)
        {
            if (!IsPortInUse(favorite_port).Item1)
            {
                return favorite_port;
            }
        }

        var (isUse, port) = IsPortInUse(null);

        if (!isUse)
        {
            return port.GetValueOrDefault();
        }

        throw new Exception("fail to get useable port");
    }
}

