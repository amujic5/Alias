//
//  AppViewFactory.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import UIKit

extension UIButton {
    public convenience init(type: UIButton.ButtonType = .system, style: ((UIButton) -> Void)...) {
        self.init(type: type)
        style.forEach { $0(self) }
    }
}

extension UIImageView {
    public convenience init(style: ((UIImageView) -> Void)...) {
        self.init()
        style.forEach { $0(self) }
    }
}

extension UILabel {
    public convenience init(style: ((UILabel) -> Void)...) {
        self.init(frame: .zero)
        style.forEach { $0(self) }
    }
}

extension UITextField {
    public convenience init(style: ((UITextField) -> Void)...) {
        self.init()
        style.forEach { $0(self) }
    }
}
