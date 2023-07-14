namespace DwebBrowser.MicroService.Core;

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
    public bool UnInstall(MicroModule mm);

    /**
     * <summary>
     * 动态js应用查询
     * 无法使用JsMicroModule作为返回值，因为会造成循环引用，
     * 所以使用时要判断是否为JsMicroModule
     * </summary>
     */
    public MicroModule? Query(Mmid mmid);

    /**
     * <summary>
     * 重启应用
     * </summary>
     */
    public void Restart(Mmid mmid);

    /**
     * <summary>
     * 与其它应用建立连接
     * </summary>
     */
    public Task<ConnectResult> ConnectAsync(Mmid mmid, PureRequest? reason = null);

    /**
     * <summary>
     * 启动其它应用
     * </summary>
     */
    public Task<bool> Open(Mmid mmid);

    /**
     * <summary>
     * 关闭其它应用
     * </summary>
     */
    public Task<bool> Close(Mmid mmid);
}

