using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace DwebBrowser.MicroService.Sys.Http.Net;

public static class PortHelper
{
    public static (bool, int?) IsPortInUse(int? try_port)
    {
        int? port = try_port;

        if (port is null)
        {
            TcpListener tcpListen = new TcpListener(IPAddress.Parse("127.0.0.1"), 0);
            tcpListen.Start();
            port = ((IPEndPoint)tcpListen.LocalEndpoint).Port;
            tcpListen.Stop();

            return (true, port);
        }
        else
        {
            bool inUse = false;
            IPGlobalProperties ipProperties = IPGlobalProperties.GetIPGlobalProperties();
            IPEndPoint[] ipEndPoints = ipProperties.GetActiveTcpListeners();
            foreach (IPEndPoint endPoint in ipEndPoints)
            {
                if (endPoint.Port == port)
                {
                    inUse = true;
                    break;
                }
            }

            return (inUse, port);
        }
    }

    public static int FindPort(int[] favorite_ports)
    {
        foreach (var favorite_port in favorite_ports)
        {
            if (IsPortInUse(favorite_port).Item1)
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

