//
//  Presentable.swift
//  ClashTV
//
//  Created by Mohamed Ali BEN YAAGOUB on 17/12/2018.
//  Copyright © 2018 Mohamed Ali. All rights reserved.
//

import Foundation
import UIKit

public protocol Presentable {
    func toPresent() -> UIViewController?
}

extension UIViewController: Presentable {

    public func toPresent() -> UIViewController? {
        return self
    }
}
