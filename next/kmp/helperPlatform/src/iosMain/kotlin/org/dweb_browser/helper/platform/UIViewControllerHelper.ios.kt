package org.dweb_browser.helper.platform

import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.UIViewController

fun UIViewController.addMmid(mmid: String) {
  view.addMmid(mmid)
}

fun UIView.addMmid(mmid: String) {
  val domainLabel = UILabel()
  this.addSubview(domainLabel)
  domainLabel.font = UIFont.systemFontOfSize(fontSize = 8.0)
  domainLabel.textColor = UIColor.blackColor.colorWithAlphaComponent(alpha = 0.2)
  domainLabel.text = mmid
  domainLabel.sizeToFit()
  domainLabel.translatesAutoresizingMaskIntoConstraints = false

  NSLayoutConstraint.activateConstraints(
    constraints = listOf(
      domainLabel.topAnchor.constraintEqualToAnchor(this.topAnchor, 3.0),
      domainLabel.centerXAnchor.constraintEqualToAnchor(this.centerXAnchor)
    )
  )
}
