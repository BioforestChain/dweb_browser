using System.Text.Json.Serialization;

namespace DwebBrowser.MicroService.Browser.NativeUI.Base;

public abstract class AreaController
{
    /// <summary>
    /// 是否层叠渲染
    /// </summary>
    readonly public State<bool> OverlayState;

    /// <summary>
    /// 插入空间
    /// </summary>
    readonly public State<AreaJson> AreaState;


    public AreaController(State<bool> overlayState, State<AreaJson> areaState)
    {
        OverlayState = overlayState;
        AreaState = areaState;
    }
}

public class AreaState
{
    [JsonPropertyName("overlay")]
    public bool Overlay { get; set; }
    [JsonPropertyName("insets")]
    public AreaJson Area { get; set; }
}
