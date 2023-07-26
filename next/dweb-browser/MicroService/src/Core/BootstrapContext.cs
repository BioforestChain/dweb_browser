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
    public Task<IMicroModuleManifest?> Query(Mmid mmid);

    /**
     * <summary>
     * 重启应用
     * </summary>
     */
    public Task Restart(Mmid mmid);

    /**
     * <summary>
     * 打开应用并与之建立链接
     * </summary>
     */
    public Task<ConnectResult> ConnectAsync(Mmid mmid, PureRequest? reason = null);

    /**
     * <summary>
     * 根据类目搜索模块
     * > 这里暂时不需要支持复合搜索，未来如果有需要另外开接口
     * @param category
     * </summary>
     */
    public Task<MicroModule[]> Search(MicroModuleCategory category);

    /**
     * <summary>
     * 打开应用，如果应用不存在，或者因某种原因（程序错误、或者被限制）启动失败，会返回 false
     * 返回true，说明应用已经在运行
     * </summary>
     */
    public Task<bool> Open(Mmid mmid);

    /**
     * <summary>
     * 关闭应用，如果应用不存在，或者用户拒绝关闭、或者因为某种原因（程序错误、或者被限制），会返回false
     * 返回true，说明应用已经停止运行
     * </summary>
     */
    public Task<bool> Close(Mmid mmid);
}

