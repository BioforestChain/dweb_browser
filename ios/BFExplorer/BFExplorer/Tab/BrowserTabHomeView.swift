//
//  BrowserTabEmptyStateView.swift
//  Browser
//
//    23.    
//

import UIKit
import SnapKit
import RxSwift
//热门网站
//个人收藏
//当前头条


let screen_w = UIScreen.main.bounds.width
let screen_h = UIScreen.main.bounds.height

let padding = CGFloat(10)

let GapOfTitleAndTouches = CGFloat(10)

let CategorySideMargin = CGFloat(20)

let itemVerGap = CGFloat(10)

let TitleHeight = CGFloat(40)

let CategoryViewWidth = screen_w - 2*CategorySideMargin

let itemWidth = (CategoryViewWidth - 3 * padding )/4

let itemHeight = itemWidth * 6.0 / 6

let CategoryViewHeight = TitleHeight + GapOfTitleAndTouches + itemVerGap + itemHeight * 2

let CategoryGap = CGFloat(30)

let mainTintColor = UIColor(red: 56/255.0, green: 126/255.0, blue: 246/255.0, alpha: 1)

let ItemHorzGap = CGFloat( 10)

let lineGap = CGFloat( 20)

var appNames: [String]?{
    Array (InnerAppFileManager.shared.appIdList)
}

public enum Category: Int {
    case hotSite, bookmark, apps
}

var onDeckApps = sharedCachesMgr.readAvailableApps()
var downloadingAppIds = [String]()


class CategoryView: UIView{
    public let titleLabel = UILabel(frame: CGRect(x: 0, y: 0, width: 150, height: TitleHeight))
    public var clickables : [TouchView]? = nil
    var type: Category = .hotSite

    let bg = DisposeBag()
    
    init(frame:CGRect, type: Category){
        super.init(frame: frame)
        self.type = type
        self.setupView()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupView(){
        titleLabel.text = ["热门网站", "我的书签", "我的app"][type.rawValue]
        titleLabel.font = .boldSystemFont(ofSize: 20)
        addSubview(titleLabel)
        
        clickables = Array()
        for index in 0..<8 {
            let rect = CGRect(x: CGFloat(index % 4) * (itemWidth+ItemHorzGap), y:TitleHeight + CGFloat(index / 4) * (itemHeight+lineGap), width: itemWidth, height: itemHeight)
            let btn = TouchView(frame: rect)
            clickables?.append(btn)
            addSubview(btn)
            btn.tag = index
            let selector = (type == .apps) ? #selector (iconClicked) : #selector (clickedNews)
            btn.addTarget(self, action: selector, for: .touchUpInside)
        }
        if type == .apps{
            operateMonitor.startAnimationMonitor.subscribe(onNext: { [weak self] appId in
          
                DispatchQueue.main.async {
                    if let index = onDeckApps.firstIndex(where: { info in
                        info.appId == appId
                    }){
                        let button = self!.clickables![index]
                        button.realImageView.setupForAppleReveal()
                        
                    }
                }
               
            }).disposed(by: bg)
            updateRedSpot()
        }

    }
    
    
    func updateRedSpot(){
        for (index, info) in onDeckApps.enumerated(){
            let shouldHide = !InnerAppFileManager.shared.redHot(appId: info.appId)
            let touchView = self.clickables![index]
            touchView.hideRedSpot(shouldHide)
        }

    }
    
    @objc private  func clickedNews(sender: TouchView){
        let index = clickables?.firstIndex(of: sender)
        
        var link = ""
        if type == .hotSite{
            link = hotWebsites[index!].link!
        }else if(type == .apps){
            
        }else{
            let bookmarks = sharedCachesMgr.readList(of: .bookmark)
            
            if index == 7 && bookmarks.count > 8{
                NotificationCenter.default.post(name: ShowAllBookmarksWhenCountMoreThan8, object: nil)
                return
            }else{
                link = bookmarks[index!].link
            }
        }
        NotificationCenter.default.post(name: newsLinkClickedNotification, object: link)
    }
    
    func updateHotWebsites(_ models:[WebModel]){
        for (index, btn) in clickables!.enumerated() {
            let model = models[index]
            
            btn.setImage(image: model.icon!)
            btn.setTitle(text: model.title!)
        }
    }
    
    func updateTouches(_ records:[LinkRecord]){
        
        for index in 0...7{
            if index < 8 {
                if index < records.count{
                    
                    if let image = ImageHelper.getSavedImage(named: records[index].imageName) {
                        clickables?[index].setImage(image: image)
                    }
                    if records[index].title.count > 0 {
                        clickables?[index].setTitle(text: records[index].title)
                    }
                }
            }
            
            if index == 7 && records.count > 8{
                clickables?[index].setImage(image: UIImage(named: "ico_home_bookmark")!)
                clickables?[index].setTitle(text: "更多")
            }
            clickables?[index].isHidden = index >= records.count
        }
    }

    func updateAppContainerView(_ records:[AppInfo]){
        for (index,record) in records.enumerated(){
            if let image = record.appIcon {
                clickables?[index].setImage(image: image)
            }
            clickables?[index].setTitle(text: record.appName)
        }
    }
    
    @objc func iconClicked(sender: UIButton) {
        guard sender.tag < onDeckApps.count else { return }
        let info = onDeckApps[sender.tag]
        let touchView = clickables![sender.tag]
        if InnerAppFileManager.shared.redHot(appId: info.appId){
            touchView.hideRedSpot(true)
            InnerAppFileManager.shared.updateRedHot(appId: info.appId, statue: false)
        }
        
        print(sender.tag)
        let type = InnerAppFileManager.shared.currentAppType(appId: info.appId)
        if type == .system {
            
            let second = WebViewViewController()
            second.appId = info.appId
            second.urlString = InnerAppFileManager.shared.systemWebAPPURLString(appId: info.appId) ?? "" //":/index.html"
            let type = InnerAppFileManager.shared.systemAPPType(appId: info.appId)
            let url = InnerAppFileManager.shared.systemWebAPPURLString(appId: info.appId) ?? ""
   
            second.urlString = url
            
            NotificationCenter.default.post(name: openAnAppNotification, object: second)
            
        } else if type == .recommend {
            InnerAppFileManager.shared.clickRecommendAppAction(appId: info.appId)
        } else if type == .user {
            InnerAppFileManager.shared.clickRecommendAppAction(appId: info.appId)
        }
    }
}

let newsBtnWidth = screen_w - padding * 2
let newsBtnHeight = CGFloat(30)
let newsVertGap = CGFloat(5)
let HoverViewTag = 10013

@objc class HotNewsView: UIView{
    public let titleLabel = UILabel(frame: CGRect(x: 0, y: 0, width: 150, height: TitleHeight))
    
    public var clickables : [UIButton]? = nil
    
    @objc  dynamic var clickedLink: String = "www.baidu.com"
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        addSubview(titleLabel)
        titleLabel.text = "全网热搜"
        titleLabel.font = UIFont.boldSystemFont(ofSize: 21)
        setupView()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    func setupView(){
        clickables = Array()
        for i in 1...10 {
            let btn = UIButton(frame: CGRect(x: 5, y: newsVertGap + (newsBtnHeight + newsVertGap) * CGFloat(i), width: CategoryViewWidth, height: newsBtnHeight))
            
            btn.contentHorizontalAlignment = .left
            btn.setTitleColor(UIColor(hexColor: "#0A1626"), for: .normal)
            addSubview(btn)
            clickables?.append(btn)
            
            let scales = [0.3, 0.8, 0.3, 0.6, 0.4, 0.7, 0.8, 0.3, 0.6, 0.4, 0.7, 0.5] // CGFloat.random(in: 0.3 ... 0.8)
            let hoverView = HoverView(frame: CGRect(x: 0, y: 0, width: scales[i] * btn.width, height: btn.height * 0.8))
            hoverView.tag = HoverViewTag
            btn.addSubview(hoverView)
            
        }
        layer.masksToBounds = true
        
    }
    
    func updateHotNewsList(newsList:CachedNesws){
        let colors = [UIColor.init(hexColor: "FA3E3E"), UIColor.init(hexColor: "FA9C3E"), UIColor.init(hexColor: "FA9C3E"), UIColor.init(hexColor: "ACB5BF") ]

        for (index, news) in newsList.enumerated() {
            let btn = clickables![index]
            btn.addTarget(self, action: #selector(newsClicked), for: .touchUpInside)
            //            btn.setTitle(news["title"] as? String, for: .normal)
            // create attributed string
            var boldFont:UIFont { return UIFont(name: "SourceHanSansCN-Bold", size: 18) ?? UIFont.boldSystemFont(ofSize: 18) }
            let attr1 = [ NSAttributedString.Key.font: boldFont, NSAttributedString.Key.foregroundColor:colors[index < 3 ? index : 3]]
            var str1 = " \(index+1)  "
            if index > 8{
                str1.removeFirst()
            }
            let attrStr1 = NSMutableAttributedString(string: str1, attributes: attr1 )
            
            
            var titleFont:UIFont { return UIFont(name: "SourceHanSansCN-Regular", size: 16) ?? UIFont.systemFont(ofSize: 16) }
            
            let myString = news["title"] as? String
            let myAttribute = [ NSAttributedString.Key.font: titleFont,NSAttributedString.Key.foregroundColor: UIColor.init(hexColor: "0a1626") ]
            let myAttrString = NSAttributedString(string: myString ?? String(index+1), attributes: myAttribute)
            
            attrStr1.append(myAttrString)
            // set attributed text on a UILabel
            btn.setAttributedTitle(attrStr1, for: .normal)
            
            print("current thread" + "\(Thread.current)")
            if let hoverView = btn.viewWithTag(HoverViewTag) as? HoverView {
                hoverView.visible = true
            }
        }
    }
    
    
    @objc func newsClicked(sender:UIButton){
        
        guard let index = clickables?.firstIndex(of: sender) else { return }
        guard let item = sharedCachesMgr.fetchNews()?[index] else { return }
//        guard index < news.count, let item = news[index] else { return }
        guard let link = item["link"] as? String, link.count > 10 else { return }
        NotificationCenter.default.post(name: newsLinkClickedNotification, object: link)
    }
    
}


class BrowserTabHomeView: UIView, UIScrollViewDelegate {
    private let aboveBlankOfHotSite = CGFloat(40)
    
    private let aboveBlankOfBookmark = CGFloat(28)
    private let aboveBlankOfHotNews = CGFloat(40)
    private let GapOfTitleAndNews = CGFloat(10)
    
    private let newsCount = 10
    //    private let blankH = CGFloat(40)
    
    let topCutline = UIView()
    public var scrollview = UIScrollView()
    public var bookmarkCategoryView : CategoryView!
    public var hotSiteCategoryView : CategoryView!
    public var appsContainerView : CategoryView!
    @objc dynamic var hotArticleView : HotNewsView!
    
    private var fetchNewsTimer : Timer?
    
    var aboveBlankAreaOfScrollviewHeightConstraint: Constraint?
    var cutlineHeightConstraint: Constraint?
    var bookmarkViewHeightConstraint: Constraint?
    var appContainerViewHeightConstraint: Constraint?
    
    
    var hasHandledScrolling = false
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
        self.backgroundColor = UIColor(hexColor: "F5F6F7")
        addObserver(self, forKeyPath: #keyPath(UIView.alpha), options: .new, context: nil)
        
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == #keyPath(UIView.alpha){
            print(change)
        }
    }
    
    func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        if !hasHandledScrolling{
            hasHandledScrolling = true
            shouldHideHomePage = false
            NotificationCenter.default.post(name: hideKeyboardAfterScrollviewDidScrolled, object: nil)
        }
    }
    
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        let verticalOffset = scrollview.contentOffset.y
        if verticalOffset > 2 {
            cutlineHeightConstraint?.update(offset: 0.5)
            
        }else if verticalOffset < 0.3 && verticalOffset > -0.3{
            cutlineHeightConstraint?.update(offset: 0)
        }
        topCutline.alpha = abs( verticalOffset )/10.0 * 0.3
        
    }
    
    func scrollViewDidEndDragging(_ scrollView: UIScrollView, willDecelerate decelerate: Bool) {
        hasHandledScrolling = false
    }
    
    func updateTopAreaStatus(){
        aboveBlankAreaOfScrollviewHeightConstraint?.update(offset: 40)
    }
    
    func reloadBookMarkView(){
        let list = sharedCachesMgr.readList(of: .bookmark)
        bookmarkCategoryView.titleLabel.text = list.count < 1 ? "" : "我的书签"
        
        if list.count < 1{
            bookmarkViewHeightConstraint?.update(offset: 0)
        }else if list.count < 5{
            bookmarkViewHeightConstraint?.update(offset: itemHeight + TitleHeight + GapOfTitleAndTouches)
        }else{ // 多于8个
            bookmarkViewHeightConstraint?.update(offset: CategoryViewHeight)
        }
        bookmarkCategoryView.updateTouches(list)
    }
    
    func reloadAppContainerView(){
        let list = sharedCachesMgr.readAvailableApps()
        appsContainerView.titleLabel.text = list.count < 1 ? "" : "我的app"
        
        if list.count < 1{
            appContainerViewHeightConstraint?.update(offset: 0)
        }else if list.count < 5{
            appContainerViewHeightConstraint?.update(offset: itemHeight + TitleHeight + GapOfTitleAndTouches)
        }else{ // 多于8个
            appContainerViewHeightConstraint?.update(offset: CategoryViewHeight)
        }
        appsContainerView.updateAppContainerView(list)
    }

    func appendTemporaryApp(appId: String){
        if !onDeckApps.contains(where: { info in
            info.appId == appId
        }){
            guard let image = PKImageExtensions.image(with: .cyan,size: CGSize(width: 120, height: 120)) else { return }
            var appInfo = AppInfo(appName: appId, appId: appId)
            appInfo.appIcon = image
            onDeckApps.append(appInfo)
            downloadingAppIds.append(appId)
            appsContainerView.updateAppContainerView(onDeckApps)
        }

    }
    
    func installingProgressUpdate(_ notify: Notification){
        guard let infoDict = notify.userInfo,
                let progress = infoDict["progress"] as? String else { return }
        guard let appId = infoDict["appId"] as? String else { return }
        
        if !downloadingAppIds.contains(where: { installingAppId in
            installingAppId == appId
        }){
            appendTemporaryApp(appId: appId)
            operateMonitor.startAnimationMonitor.onNext(appId)
        }
                
        if progress == "complete" {
            guard let appId = infoDict["appId"] as? String else { return }
            InnerAppFileManager.shared.updateFileType(appId: appId)

            guard let index = onDeckApps.firstIndex(where: { info in
                info.appId == appId
            })  else {return}

            let button = self.appsContainerView.clickables![index]
            button.realImageView.startExpandAnimation()
            button.realImageView.image = InnerAppFileManager.shared.currentAppImage(appId: appId)
            button.realTitleLabel.text = InnerAppFileManager.shared.currentAppName(appId: appId)
            if let index = onDeckApps.firstIndex(where: { info in
                info.appName == appId
            }){
                onDeckApps.remove(at: index)
            }
            if let index2 = downloadingAppIds .firstIndex(where: { installingId in
                installingId == appId
            }){
                downloadingAppIds.remove(at: index2)
            }
            appsContainerView.updateRedSpot()

        }else if progress == "fail"{
            if let index = onDeckApps.firstIndex(where: { info in
                info.appName == appId
            }){
                onDeckApps.remove(at: index)
            }
            self.appsContainerView.updateAppContainerView(onDeckApps)
            
        }else {
            guard var progress = Double(progress) else { return }
            progress = progress < 0.98 ? progress : 0.98
            
            if let index = onDeckApps.firstIndex(where: { info in
                info.appId == appId
            }){
                let button = self.appsContainerView.clickables![index]
                button.realImageView.startProgressAnimation(progress: 1.0 - progress)
            }
        }
    }
}

// MARK: Helper methods
private extension BrowserTabHomeView {
    func setupView() {
        
        hotSiteCategoryView = CategoryView(frame: .zero, type: .hotSite)
        bookmarkCategoryView = CategoryView(frame: .zero, type: .bookmark)
        appsContainerView = CategoryView(frame:.zero, type: .apps)
        hotArticleView = HotNewsView(frame:.zero)
        
        scrollview.frame = self.bounds
        addSubview(scrollview)
        
        scrollview.alwaysBounceVertical = true
        scrollview.addSubview(hotSiteCategoryView)
        scrollview.addSubview(bookmarkCategoryView)
        scrollview.addSubview(appsContainerView)
        scrollview.addSubview(hotArticleView)
        
        hotSiteCategoryView.snp.makeConstraints { make in
            make.left.equalToSuperview().offset(CategorySideMargin)
            make.top.equalToSuperview().offset(aboveBlankOfHotSite)
            make.width.equalTo(CategoryViewWidth)
            make.height.equalTo(CategoryViewHeight)
        }
        
        bookmarkCategoryView.snp.makeConstraints { make in
            make.left.equalToSuperview().offset(CategorySideMargin)
            make.top.equalTo(hotSiteCategoryView.snp_bottomMargin).offset(aboveBlankOfBookmark)
            make.width.equalTo(CategoryViewWidth)
            bookmarkViewHeightConstraint = make.height.equalTo(CategoryViewHeight).constraint
        }
        
        appsContainerView.snp.makeConstraints { make in
            make.left.equalToSuperview().offset(CategorySideMargin)
            make.top.equalTo(bookmarkCategoryView.snp_bottomMargin).offset(aboveBlankOfBookmark)
            make.width.equalTo(CategoryViewWidth)
            appContainerViewHeightConstraint = make.height.equalTo(CategoryViewHeight).constraint
        }
        
        hotArticleView.snp.makeConstraints { make in
            make.left.equalToSuperview().offset(CategorySideMargin)
            make.top.equalTo(appsContainerView.snp_bottomMargin).offset(aboveBlankOfHotNews)
            make.width.equalTo(CategoryViewWidth)
            make.height.equalTo(TitleHeight + CGFloat(newsCount) * (newsBtnHeight + newsVertGap))
            make.bottom.equalToSuperview()
        }
        
        scrollview.delegate = self
        
        addSubview(topCutline)
        topCutline.snp.makeConstraints { make in
            make.left.right.equalToSuperview()
            cutlineHeightConstraint = make.height.equalTo(0).constraint
            aboveBlankAreaOfScrollviewHeightConstraint = make.top.equalTo(safeAreaInsets.top).constraint
            
        }
        topCutline.backgroundColor = .lightGray
        
        scrollview.snp.makeConstraints { make in
            make.top.equalTo(topCutline.snp_bottomMargin).offset(8)
            make.left.right.bottom.equalToSuperview()
        }
        
        hotSiteCategoryView?.updateHotWebsites(hotWebsites)
        reloadBookMarkView()
        reloadAppContainerView()
        guard let newsList = sharedCachesMgr.fetchNews(), newsList.count > 0 else { return }

        hotArticleView.updateHotNewsList(newsList: newsList)
    }
}
