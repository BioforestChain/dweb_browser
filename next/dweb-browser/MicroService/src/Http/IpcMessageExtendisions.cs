namespace DwebBrowser.MicroService.Http;


static public class IpcMessageExtendisions
{
    static public IpcBody ToIpcBody(this IPureBody body, Ipc ipc)
    {
        return body switch
        {
            PureUtf8StringBody stringBody => IpcBodySender.FromText(stringBody.Data, ipc),
            PureBase64StringBody byteArrayBody => IpcBodySender.FromBinary(byteArrayBody.Data, ipc),
            PureByteArrayBody byteArrayBody => IpcBodySender.FromBinary(byteArrayBody.Data, ipc),
            PureStreamBody streamBody => IpcBodySender.FromStream(streamBody.Data, ipc),
            PureEmptyBody => IpcBodySender.FromText("", ipc),
            _ => IpcBodySender.FromText("", ipc),
        };
    }
    static public IPureBody ToPureBody(this IpcBody body)
    {
        return body.Raw switch
        {
            string stringData => new PureUtf8StringBody(stringData),
            byte[] byteArrayData => new PureByteArrayBody(byteArrayData),
            Stream streamData => new PureStreamBody(streamData),
            var unknownData => throw new Exception(string.Format("invalid body to request: {0}", unknownData)),
        };
    }


    static public IpcRequest ToIpcRequest(this PureRequest request, int req_id, Ipc ipc)
    {
        var ipcBody = (request.Method.Method is "GET" or "HEAD")
                    ? IpcBodySender.FromText("", ipc)
                    : request.Body.ToIpcBody(ipc);

        var ipcRequest = new IpcRequest(req_id,
                request.Url,
                request.Method,
                request.Headers,
                ipcBody,
                ipc
            );
        return ipcRequest;
    }
    static public PureRequest ToPureRequest(this IpcRequest self) =>
        new(self.Url, self.Method, self.Headers, self.Body.ToPureBody());

    static public IpcResponse ToIpcResponse(this PureResponse response, int req_id, Ipc ipc) =>
        new(req_id, (int)response.StatusCode, response.Headers, response.Body.ToIpcBody(ipc), ipc);
    static public PureResponse ToPureResponse(this IpcResponse self) =>
        new((HttpStatusCode)self.StatusCode, self.Headers, self.Body.ToPureBody());

    static public Task PostPureResponseAsync(this Ipc ipc, int req_id, PureResponse response) =>
        ipc.PostMessageAsync(response.ToIpcResponse(req_id, ipc)).NoThrow();
}