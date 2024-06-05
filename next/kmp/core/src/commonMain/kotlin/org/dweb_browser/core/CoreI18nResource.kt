package org.dweb_browser.core

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

object CoreI18nResource {
  enum class Category(val value: MICRO_MODULE_CATEGORY, val res: SimpleI18nResource) {
    Service(
      MICRO_MODULE_CATEGORY.Service,
      SimpleI18nResource(Language.ZH to "服务", Language.EN to "Service")
    ),
    RoutingService(
      MICRO_MODULE_CATEGORY.Routing_Service,
      SimpleI18nResource(Language.ZH to "路由服务", Language.EN to "Routing Service")
    ),
    ProcessService(
      MICRO_MODULE_CATEGORY.Process_Service,
      SimpleI18nResource(Language.ZH to "进程服务", Language.EN to "Process Service")
    ),
    RenderService(
      MICRO_MODULE_CATEGORY.Render_Service,
      SimpleI18nResource(Language.ZH to "渲染服务", Language.EN to "Render Service")
    ),
    ProtocolService(
      MICRO_MODULE_CATEGORY.Protocol_Service,
      SimpleI18nResource(Language.ZH to "协议服务", Language.EN to "Protocol Service")
    ),
    DeviceManagementService(
      MICRO_MODULE_CATEGORY.Device_Management_Service,
      SimpleI18nResource(Language.ZH to "设备管理服务", Language.EN to "Device Management Service")
    ),
    ComputingService(
      MICRO_MODULE_CATEGORY.Computing_Service,
      SimpleI18nResource(Language.ZH to "计算服务", Language.EN to "Computing Service")
    ),
    StorageService(
      MICRO_MODULE_CATEGORY.Storage_Service,
      SimpleI18nResource(Language.ZH to "存储服务", Language.EN to "Storage Service")
    ),
    DatabaseService(
      MICRO_MODULE_CATEGORY.Database_Service,
      SimpleI18nResource(Language.ZH to "数据库服务", Language.EN to "Database Service")
    ),
    NetworkService(
      MICRO_MODULE_CATEGORY.Network_Service,
      SimpleI18nResource(Language.ZH to "网络服务", Language.EN to "Network Service")
    ),
    HubService(
      MICRO_MODULE_CATEGORY.Hub_Service,
      SimpleI18nResource(Language.ZH to "聚合服务", Language.EN to "Hub Service")
    ),
    DistributionService(
      MICRO_MODULE_CATEGORY.Distribution_Service,
      SimpleI18nResource(Language.ZH to "分发服务", Language.EN to "Distribution Service")
    ),
    SecurityService(
      MICRO_MODULE_CATEGORY.Security_Service,
      SimpleI18nResource(Language.ZH to "安全服务", Language.EN to "Security Service")
    ),
    LogService(
      MICRO_MODULE_CATEGORY.Log_Service,
      SimpleI18nResource(Language.ZH to "日志服务", Language.EN to "Log Service")
    ),
    IndicatorService(
      MICRO_MODULE_CATEGORY.Indicator_Service,
      SimpleI18nResource(Language.ZH to "指标服务", Language.EN to "Indicator Service")
    ),
    TrackingService(
      MICRO_MODULE_CATEGORY.Tracking_Service,
      SimpleI18nResource(Language.ZH to "追踪服务", Language.EN to "Tracking Service")
    ),
    VisualService(
      MICRO_MODULE_CATEGORY.Visual_Service,
      SimpleI18nResource(Language.ZH to "视觉服务", Language.EN to "Visual Service")
    ),
    AudioService(
      MICRO_MODULE_CATEGORY.Audio_Service,
      SimpleI18nResource(Language.ZH to "语音服务", Language.EN to "Audio Service")
    ),
    TextService(
      MICRO_MODULE_CATEGORY.Text_Service,
      SimpleI18nResource(Language.ZH to "文字服务", Language.EN to "Text Service")
    ),
    MachineLearningService(
      MICRO_MODULE_CATEGORY.Machine_Learning_Service,
      SimpleI18nResource(Language.ZH to "机器学习服务", Language.EN to "Machine Learning Service")
    ),
    Application(
      MICRO_MODULE_CATEGORY.Application,
      SimpleI18nResource(Language.ZH to "应用", Language.EN to "Application")
    ),
    Settings(
      MICRO_MODULE_CATEGORY.Settings,
      SimpleI18nResource(Language.ZH to "设置", Language.EN to "Settings")
    ),
    Desktop(
      MICRO_MODULE_CATEGORY.Desktop,
      SimpleI18nResource(Language.ZH to "桌面", Language.EN to "Desktop")
    ),
    WebBrowser(
      MICRO_MODULE_CATEGORY.Web_Browser,
      SimpleI18nResource(Language.ZH to "网页浏览器", Language.EN to "Web Browser")
    ),
    Files(
      MICRO_MODULE_CATEGORY.Files,
      SimpleI18nResource(Language.ZH to "文件管理", Language.EN to "Files")
    ),
    Wallet(
      MICRO_MODULE_CATEGORY.Wallet,
      SimpleI18nResource(Language.ZH to "钱包", Language.EN to "Wallet")
    ),
    Assistant(
      MICRO_MODULE_CATEGORY.Assistant,
      SimpleI18nResource(Language.ZH to "助理", Language.EN to "Assistant")
    ),
    Business(
      MICRO_MODULE_CATEGORY.Business,
      SimpleI18nResource(Language.ZH to "商业", Language.EN to "Business")
    ),
    Developer(
      MICRO_MODULE_CATEGORY.Developer,
      SimpleI18nResource(Language.ZH to "开发者工具", Language.EN to "Developer")
    ),
    Education(
      MICRO_MODULE_CATEGORY.Education,
      SimpleI18nResource(Language.ZH to "教育", Language.EN to "Education")
    ),
    Finance(
      MICRO_MODULE_CATEGORY.Finance,
      SimpleI18nResource(Language.ZH to "财务", Language.EN to "Finance")
    ),
    Productivity(
      MICRO_MODULE_CATEGORY.Productivity,
      SimpleI18nResource(Language.ZH to "办公效率", Language.EN to "Productivity")
    ),
    Messages(
      MICRO_MODULE_CATEGORY.Messages,
      SimpleI18nResource(Language.ZH to "消息软件", Language.EN to "Messages")
    ),
    Live(
      MICRO_MODULE_CATEGORY.Live,
      SimpleI18nResource(Language.ZH to "实时互动", Language.EN to "Live")
    ),
    Entertainment(
      MICRO_MODULE_CATEGORY.Entertainment,
      SimpleI18nResource(Language.ZH to "娱乐", Language.EN to "Entertainment")
    ),
    Games(
      MICRO_MODULE_CATEGORY.Games,
      SimpleI18nResource(Language.ZH to "游戏", Language.EN to "Games")
    ),
    Lifestyle(
      MICRO_MODULE_CATEGORY.Lifestyle,
      SimpleI18nResource(Language.ZH to "生活休闲", Language.EN to "Lifestyle")
    ),
    Music(
      MICRO_MODULE_CATEGORY.Music,
      SimpleI18nResource(Language.ZH to "音乐", Language.EN to "Music")
    ),
    News(
      MICRO_MODULE_CATEGORY.News,
      SimpleI18nResource(Language.ZH to "新闻", Language.EN to "News")
    ),
    Sports(
      MICRO_MODULE_CATEGORY.Sports,
      SimpleI18nResource(Language.ZH to "体育", Language.EN to "Sports")
    ),
    Video(
      MICRO_MODULE_CATEGORY.Video,
      SimpleI18nResource(Language.ZH to "视频", Language.EN to "Video")
    ),
    Photo(
      MICRO_MODULE_CATEGORY.Photo,
      SimpleI18nResource(Language.ZH to "照片", Language.EN to "Photo")
    ),
    GraphicsDesign(
      MICRO_MODULE_CATEGORY.Graphics_a_Design,
      SimpleI18nResource(Language.ZH to "图形和设计", Language.EN to "Graphics and Design")
    ),
    Photography(
      MICRO_MODULE_CATEGORY.Photography,
      SimpleI18nResource(Language.ZH to "摄影与录像", Language.EN to "Photography")
    ),
    Personalization(
      MICRO_MODULE_CATEGORY.Personalization,
      SimpleI18nResource(Language.ZH to "个性化", Language.EN to "Personalization")
    ),
    Books(
      MICRO_MODULE_CATEGORY.Books,
      SimpleI18nResource(Language.ZH to "书籍", Language.EN to "Books")
    ),
    Magazines(
      MICRO_MODULE_CATEGORY.Magazines,
      SimpleI18nResource(Language.ZH to "杂志", Language.EN to "Magazines")
    ),
    Food(
      MICRO_MODULE_CATEGORY.Food,
      SimpleI18nResource(Language.ZH to "食物", Language.EN to "Food")
    ),
    Health(
      MICRO_MODULE_CATEGORY.Health,
      SimpleI18nResource(Language.ZH to "健康", Language.EN to "Health")
    ),
    Fitness(
      MICRO_MODULE_CATEGORY.Fitness,
      SimpleI18nResource(Language.ZH to "健身", Language.EN to "Fitness")
    ),
    Medical(
      MICRO_MODULE_CATEGORY.Medical,
      SimpleI18nResource(Language.ZH to "医疗", Language.EN to "Medical")
    ),
    Navigation(
      MICRO_MODULE_CATEGORY.Navigation,
      SimpleI18nResource(Language.ZH to "导航", Language.EN to "Navigation")
    ),
    Reference(
      MICRO_MODULE_CATEGORY.Reference,
      SimpleI18nResource(Language.ZH to "参考工具", Language.EN to "Reference")
    ),
    Utilities(
      MICRO_MODULE_CATEGORY.Utilities,
      SimpleI18nResource(Language.ZH to "实用工具", Language.EN to "Utilities")
    ),
    Travel(
      MICRO_MODULE_CATEGORY.Travel,
      SimpleI18nResource(Language.ZH to "旅行", Language.EN to "Travel")
    ),
    Weather(
      MICRO_MODULE_CATEGORY.Weather,
      SimpleI18nResource(Language.ZH to "天气", Language.EN to "Weather")
    ),
    Kids(
      MICRO_MODULE_CATEGORY.Kids,
      SimpleI18nResource(Language.ZH to "儿童", Language.EN to "Kids")
    ),
    Shopping(
      MICRO_MODULE_CATEGORY.Shopping,
      SimpleI18nResource(Language.ZH to "购物", Language.EN to "Shopping")
    ),
    Security(
      MICRO_MODULE_CATEGORY.Security,
      SimpleI18nResource(Language.ZH to "安全", Language.EN to "Security")
    ),
    Social(
      MICRO_MODULE_CATEGORY.Social,
      SimpleI18nResource(Language.ZH to "社交", Language.EN to "Social")
    ),
    Career(
      MICRO_MODULE_CATEGORY.Career,
      SimpleI18nResource(Language.ZH to "职业生涯", Language.EN to "Career")
    ),
    Government(
      MICRO_MODULE_CATEGORY.Government,
      SimpleI18nResource(Language.ZH to "政府", Language.EN to "Government")
    ),
    Politics(
      MICRO_MODULE_CATEGORY.Politics,
      SimpleI18nResource(Language.ZH to "政治", Language.EN to "Politics")
    ),
    ActionGames(
      MICRO_MODULE_CATEGORY.Action_Games,
      SimpleI18nResource(Language.ZH to "动作游戏", Language.EN to "Action Games")
    ),
    AdventureGames(
      MICRO_MODULE_CATEGORY.Adventure_Games,
      SimpleI18nResource(Language.ZH to "冒险游戏", Language.EN to "Adventure Games")
    ),
    ArcadeGames(
      MICRO_MODULE_CATEGORY.Arcade_Games,
      SimpleI18nResource(Language.ZH to "街机游戏", Language.EN to "Arcade Games")
    ),
    BoardGames(
      MICRO_MODULE_CATEGORY.Board_Games,
      SimpleI18nResource(Language.ZH to "棋盘游戏", Language.EN to "Board Games")
    ),
    CardGames(
      MICRO_MODULE_CATEGORY.Card_Games,
      SimpleI18nResource(Language.ZH to "卡牌游戏", Language.EN to "Card Games")
    ),
    CasinoGames(
      MICRO_MODULE_CATEGORY.Casino_Games,
      SimpleI18nResource(Language.ZH to "赌场游戏", Language.EN to "Casino Games")
    ),
    DiceGames(
      MICRO_MODULE_CATEGORY.Dice_Games,
      SimpleI18nResource(Language.ZH to "骰子游戏", Language.EN to "Dice Games")
    ),
    EducationalGames(
      MICRO_MODULE_CATEGORY.Educational_Games,
      SimpleI18nResource(Language.ZH to "教育游戏", Language.EN to "Educational Games")
    ),
    FamilyGames(
      MICRO_MODULE_CATEGORY.Family_Games,
      SimpleI18nResource(Language.ZH to "家庭游戏", Language.EN to "Family Games")
    ),
    KidsGames(
      MICRO_MODULE_CATEGORY.Kids_Games,
      SimpleI18nResource(Language.ZH to "儿童游戏", Language.EN to "Kids Games")
    ),
    MusicGames(
      MICRO_MODULE_CATEGORY.Music_Games,
      SimpleI18nResource(Language.ZH to "音乐游戏", Language.EN to "Music Games")
    ),
    PuzzleGames(
      MICRO_MODULE_CATEGORY.Puzzle_Games,
      SimpleI18nResource(Language.ZH to "益智游戏", Language.EN to "Puzzle Games")
    ),
    RacingGames(
      MICRO_MODULE_CATEGORY.Racing_Games,
      SimpleI18nResource(Language.ZH to "赛车游戏", Language.EN to "Racing Games")
    ),
    RolePlayingGames(
      MICRO_MODULE_CATEGORY.Role_Playing_Games,
      SimpleI18nResource(Language.ZH to "角色扮演游戏", Language.EN to "Role Playing Games")
    ),
    SimulationGames(
      MICRO_MODULE_CATEGORY.Simulation_Games,
      SimpleI18nResource(Language.ZH to "模拟经营游戏", Language.EN to "Simulation Games")
    ),
    SportsGames(
      MICRO_MODULE_CATEGORY.Sports_Games,
      SimpleI18nResource(Language.ZH to "运动游戏", Language.EN to "Sports Games")
    ),
    StrategyGames(
      MICRO_MODULE_CATEGORY.Strategy_Games,
      SimpleI18nResource(Language.ZH to "策略游戏", Language.EN to "Strategy Games")
    ),
    TriviaGames(
      MICRO_MODULE_CATEGORY.Trivia_Games,
      SimpleI18nResource(Language.ZH to "问答游戏", Language.EN to "Trivia Games")
    ),
    WordGames(
      MICRO_MODULE_CATEGORY.Word_Games,
      SimpleI18nResource(Language.ZH to "文字游戏", Language.EN to "Word Games")
    )
    ;

    companion object {
      val ALL = entries.associateBy { it.value }
    }
  }
}