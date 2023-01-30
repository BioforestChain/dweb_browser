//
//  ViewController.swift
//  Challenge
//
//  Created by Nathaniel Whittington on 6/5/22.
//

import UIKit
import EmptyDataSet_Swift

public enum PageType: Int {
    case bookmark, history, share
}

struct SuitableData{
    
}

let safeAreaInsets = UIApplication.shared.keyWindow?.safeAreaInsets

class LinkListPageVC: UIViewController {
    var tableView: UITableView!
    let deleteButton = UIButton()
    
    private var type = PageType.history
    var cachedDatas:[LinkRecord]{
        get{
            sharedCachesMgr.readList(of: type)
        }
    }

    fileprivate var suitableDatasStorage : [[String:Any]]?
    
    var suitableDatas: [[String:Any]]{
        get {
            if suitableDatasStorage == nil {
                suitableDatasStorage = self.convert(sourceData: self.cachedDatas)
            }
            return suitableDatasStorage!
        }
    }

    var curDataSource: [[String:Any]]{
        get{
            let isSearchMode = searchController.isActive && searchController.searchBar.text != ""
            return  isSearchMode ? self.filteredItems : self.suitableDatas
        }
    }
    


    
    var selectedIndexPaths = [IndexPath]()
    
    var filteredItems = [[String:Any]]()
    let searchController = UISearchController()
    
    var config: Configuration!
    
    var isLoading = false {
        didSet {
            tableView.reloadEmptyDataSet()
//            config.isLoading = isLoading
        }
    }
    
    init(type:PageType) {
        self.type = type
        super.init(nibName: nil, bundle: nil)
        self.title = ["书签", "历史记录"][type.rawValue]
        searchController.searchBar.placeholder = ["搜索书签", "搜索历史记录"][type.rawValue]
    }
    
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.config = Configuration(type)
        
        tableView = UITableView.init(frame: CGRect(x: 0, y: 0, width: screen_w, height: 400), style: .grouped)
        
        searchController.searchResultsUpdater = self
        searchController.dimsBackgroundDuringPresentation = false
        definesPresentationContext = true
        tableView.tableHeaderView = searchController.searchBar
        searchController.searchBar.barTintColor = UIColor.white //(red: 245.0/255, green: 246.0/255, blue: 247.0/255)
        searchController.searchBar.searchBarStyle = .minimal;
        searchController.searchBar.searchTextField.backgroundColor = .white
        tableView.register(CustomTableViewCell.self, forCellReuseIdentifier: "cell")
        
        tableView.separatorStyle = .none
        tableView.backgroundColor =  UIColor(red: 245.0/255, green: 246.0/255, blue: 247.0/255, alpha: 1)
        view.backgroundColor =  UIColor(red: 245.0/255, green: 246.0/255, blue: 247.0/255, alpha: 1)
        
        tableView.delegate = self
        tableView.dataSource = self
        tableView.emptyDataSetSource = self
        tableView.emptyDataSetDelegate = self
        view.addSubview(tableView)
        
        deleteButton.setTitle("删除", for: .normal)
        deleteButton.setTitleColor(.red, for: .normal)
        deleteButton.setTitleColor(UIColor(hexColor: "ACB5BF"), for: .disabled)
        deleteButton.titleLabel?.font = .systemFont(ofSize: 22)
        deleteButton.frame = CGRect(x: 0, y: 0, width: screen_w, height: 50)
        deleteButton.backgroundColor = .white
        deleteButton.isEnabled = false
        deleteButton.addTarget(self, action: #selector(deleteRecords), for: .touchUpInside)
        view.addSubview(deleteButton)
        
        tableView.snp.makeConstraints { make in
            make.left.right.equalToSuperview()
            make.top.equalTo(safeAreaInsets!.top)
            make.bottom.equalToSuperview().offset(-safeAreaInsets!.bottom-50)
        }
        print(safeAreaInsets)
        
        deleteButton.snp.makeConstraints { make in
            make.left.right.equalToSuperview()
            make.height.equalTo(40)
            make.top.equalTo(tableView.snp_bottomMargin)
        }
        
    }
    
}

extension LinkListPageVC: HistoryRecordCellDelegate{
    
    func recordFor(indexPath:IndexPath) -> LinkRecord{
        let recordsofDate = curDataSource[indexPath.section]
        return (recordsofDate["list"] as! [LinkRecord])[indexPath.row]
    }
    
    func recordCell(_ cell: CustomTableViewCell, isSelected: Bool) {
        guard let indexPath = tableView.indexPath(for: cell) else { return }
        if isSelected{
            selectedIndexPaths.append(indexPath)
        }else{
            selectedIndexPaths = selectedIndexPaths.filter({$0 != indexPath})
        }
        self.deleteButton.isEnabled = selectedIndexPaths.count > 0
    }
    
    @objc func deleteRecords(){
        let records = selectedIndexPaths.map {
            recordFor(indexPath: $0)
        }
        let ids = records.map {
            $0.dataID
        }
        sharedCachesMgr.removeItems(ids: ids, of: type)
        
        self.selectedIndexPaths.removeAll()
        self.filteredItems.removeAll()
        suitableDatasStorage = nil
        tableView.reloadData()
        
        if type == .bookmark{
            NotificationCenter.default.post(name: bookmarksWasEditedNotification, object: nil)
        }
        deleteButton.isEnabled = false
    }
    
    //desData = [
    //              ["date":"2022-09-20 星期二", "records":[LinkRecord]],
    //              ["date":"2022-09-19 星期一", "records":[LinkRecord]]
    //          ]
    
    func convert(sourceData:[LinkRecord]) -> [[String:Any]]{
        var result = [[String:Any]]()
        var restRecords = sourceData
        
        while restRecords.count > 0{
            let date = restRecords.first?.createdDate
            let temList = restRecords.filter {
                $0.createdDate == date
            }
            let item = ["date":date, "list":temList] as [String : Any]
            result.append(item)
            restRecords = restRecords.filter {
                $0.createdDate != date
            }
        }
        result = result.sorted {
            ($0["date"] as! String) > ($1["date"] as! String)
        }
        return result
    }
}

extension LinkListPageVC: UITableViewDelegate, UITableViewDataSource, UISearchResultsUpdating {
    

    func numberOfSections(in tableView: UITableView) -> Int {
        curDataSource.count
        
    }
    
    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return 30
    }
    
    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        
        var headerView = tableView.dequeueReusableHeaderFooterView(withIdentifier: "headerId")
        if headerView == nil{
            headerView = UITableViewHeaderFooterView(reuseIdentifier: "headerId")
            headerView?.frame = CGRect(x: 0, y: 0, width: screen_w, height: 30)
            let label = UILabel(frame: CGRect(x: 15, y: 0, width: screen_w, height: 30))
            label.textColor = .lightGray
            label.font = .systemFont(ofSize: 13)
            label.tag = 101
            headerView!.addSubview(label)
        }
        
        let label = headerView?.viewWithTag(101) as? UILabel
        label?.text = curDataSource[section]["date"] as? String
        
        return headerView
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {

        return (curDataSource[section]["list"] as! [LinkRecord]).count
        
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as? CustomTableViewCell else {return UITableViewCell()}
   
        let recordsofDate = curDataSource[indexPath.section]
        let record = (recordsofDate["list"] as! [LinkRecord])[indexPath.row]
        cell.delegate = self
        cell.update(with: record, type: type)
        return cell
    }
    
    func updateSearchResults(for searchController: UISearchController) {
        filterReord(for: searchController.searchBar.text ?? "")
    }
    
    private func filterReord(for searchText:String){
        var matchItems = self.cachedDatas.filter { record in
            record.title.contains(searchText) ||  record.link.lowercased().contains(searchText.lowercased())
        }
        
        filteredItems = self.convert(sourceData: matchItems)

        tableView.reloadData()
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        
        tableView.deselectRow(at: indexPath, animated: true)
        let record = self.recordFor(indexPath: indexPath)
        NotificationCenter.default.post(name: newsLinkClickedNotification, object: record.link)
        
        self.dismiss(animated: true)
        //        let cell = tableView.cellForRow(at: indexPath)
//        updateCount()

    }
    
    
}


extension LinkListPageVC: EmptyDataSetSource, EmptyDataSetDelegate{
    
    //MARK: - DZNEmptyDataSetSource
    func title(forEmptyDataSet scrollView: UIScrollView) -> NSAttributedString? {
        return config.titleString
    }
    
    func description(forEmptyDataSet scrollView: UIScrollView) -> NSAttributedString? {
        return config.detailString
    }
    
    func image(forEmptyDataSet scrollView: UIScrollView) -> UIImage? {
        return config.image
    }
    
    func imageAnimation(forEmptyDataSet scrollView: UIScrollView) -> CAAnimation? {
        return config.imageAnimation
    }
    
    func buttonTitle(forEmptyDataSet scrollView: UIScrollView, for state: UIControl.State) -> NSAttributedString? {
        return config.buttonTitle(state)
    }
    
    func buttonBackgroundImage(forEmptyDataSet scrollView: UIScrollView, for state: UIControl.State) -> UIImage? {
        return config.buttonBackgroundImage(state)
    }
    
    func backgroundColor(forEmptyDataSet scrollView: UIScrollView) -> UIColor? {
        return config.backgroundColor
    }
    
    func verticalOffset(forEmptyDataSet scrollView: UIScrollView) -> CGFloat {
        return config.verticalOffset
    }
    
    func spaceHeight(forEmptyDataSet scrollView: UIScrollView) -> CGFloat {
        return config.spaceHeight
    }
    
    //MARK: - DZNEmptyDataSetDelegate Methods
    func emptyDataSetShouldDisplay(_ scrollView: UIScrollView) -> Bool {
        return true
    }
    
    func emptyDataSetShouldAllowTouch(_ scrollView: UIScrollView) -> Bool {
        return true
    }
    
    func emptyDataSetShouldAllowScroll(_ scrollView: UIScrollView) -> Bool {
        return true
    }

}
