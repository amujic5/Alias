//
//  UIFont+Additions.swift
//  BeautyRoom
//
//  Generated on Zeplin. (5/19/2018).
//  Copyright (c) 2018 __MyCompanyName__. All rights reserved.
//

import UIKit

extension UIFont {

    class var subtitleStyle: UIFont {
        let size: CGFloat = 13.0
        return UIFont.systemFont(ofSize: size, weight: .regular)
    }

    class var textFieldStyle: UIFont {
        let size: CGFloat = 14.0
        return UIFont.systemFont(ofSize: size, weight: .regular)
    }

    class var buttonStyle: UIFont {
        let size: CGFloat = 14.0
        return UIFont.systemFont(ofSize: size, weight: .medium)
    }

    class var titleStyle: UIFont {
        let size: CGFloat = 14.0
        return UIFont.systemFont(ofSize: size, weight: .semibold)
    }

}

extension UIEdgeInsets {
    static var defaultInsets: UIEdgeInsets {
        return UIEdgeInsets(top: 8, left: 8, bottom: 8, right: 8)
    }
}
