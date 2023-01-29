//
//  CustomTableViewCell.swift
//  Challenge
//
//  Created by Nathaniel Whittington on 6/5/22.
//

import UIKit

@objc public protocol HistoryRecordCellDelegate{
    @objc func recordCell(_ cell: CustomTableViewCell, isSelected: Bool)
    
}

public class CustomTableViewCell: UITableViewCell {
    
    var checkBtn = UIButton()
    var iconImageView = UIImageView()
    var titleLabel = UILabel()
    var urlLabel = UILabel()
    weak var delegate: HistoryRecordCellDelegate?

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: "cell")
        backgroundColor = UIColor(red: 245.0/255, green: 246.0/255, blue: 247.0/255, alpha: 1)
        setupView()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    
    func setupView(){
        contentView.addSubview(checkBtn)
        contentView.addSubview(iconImageView)
        contentView.addSubview(titleLabel)
        contentView.addSubview(urlLabel)
        
        checkBtn.setImage(UIImage(named: "ico_checkbox_uncheck"), for: .normal)
        checkBtn.setImage(UIImage(named: "ico_checkbox_checked"), for: .selected)
        checkBtn.addTarget(self, action: #selector(selectItem), for: .touchUpInside)
        
        iconImageView.layer.masksToBounds = true
        iconImageView.layer.cornerRadius = 4
        iconImageView.image = UIImage(named: "ico_bottomtab_book")
        
        titleLabel.font = .systemFont(ofSize: 16)
        urlLabel.textColor = .black

        urlLabel.font = .systemFont(ofSize: 13)
        urlLabel.textColor = .lightGray
        
        
        checkBtn.snp.makeConstraints { make in
            make.left.equalToSuperview().offset(15)
            make.width.height.equalTo(20)
            make.centerY.equalToSuperview()
        }
        iconImageView.snp.makeConstraints { make in
            make.left.equalTo(checkBtn.snp.rightMargin).offset(20)
            make.width.height.equalTo(30)
            make.centerY.equalToSuperview()
        }
        
        titleLabel.snp.makeConstraints { make in
            make.left.equalTo(iconImageView.snp.rightMargin).offset(20)
            make.right.equalToSuperview().offset(-30)
            make.height.equalTo(25)
            make.top.equalToSuperview().offset(15)
        }
        urlLabel.snp.makeConstraints { make in
            make.left.right.equalTo(titleLabel)
            make.height.equalTo(20)
            make.top.equalTo(titleLabel.snp.bottom).offset(4)
            make.bottom.equalToSuperview().offset(-10)
        }
    }
    
    @objc func selectItem(sender:UIButton){
        sender.isSelected = !sender.isSelected
        self.delegate?.recordCell(self, isSelected: sender.isSelected)
    }
    
    func update(with record: LinkRecord, type: PageType){
        titleLabel.text = record.title
        urlLabel.text = record.link
        iconImageView.image = ImageHelper.getSavedImage(named: record.imageName)
        checkBtn.isSelected = false
    }
    
}
