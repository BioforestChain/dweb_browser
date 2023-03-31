
namespace micro_service.core;

public interface IBootstrapContext
{
    public IDnsMicroModule Dns { get; init; }
}

public interface IDnsMicroModule
{
    /**
     * <summary>
     * 动态安装应用
     * </summary>
     */
    public void Install(MicroModule mm);

    /**
     * <summary>
     * 动态卸载应用
     * </summary>
     */
    public void UnInstall(MicroModule mm);

    /**
     * <summary>
     * 与其它应用建立连接
     * </summary>
     */
    public Task<ConnectResult> Connect(Mmid mmid, HttpRequestMessage? reason = null);

    /**
     * <summary>
     * 启动其它应用
     * </summary>
     */
    public Task Bootstrap(Mmid mmid);
}

