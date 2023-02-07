import http from "http";
import type { AddressInfo } from "net";
export const findPort = async (favorite_ports: number[] = []) => {
  const server = http.createServer();
  for (const favorite_port of favorite_ports) {
    try {
      server.listen(favorite_port);
      return favorite_port;
    } catch {
    } finally {
      server.close();
    }
  }

  return (server.listen().address() as AddressInfo).port;
};
