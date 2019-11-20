//
//  UIActivityIndicator+Extension.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 12/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import Yalta

extension UIActivityIndicatorView {
    
    /// Instanciate a activityIndicatorView and add it at center in the given parameter view
    ///
    /// - Parameters:
    ///   - view: view in which will be added the activityIndicatorView
    ///   - style: The style of the indicatorView by default = .gray
    convenience init(for view: UIView, style: UIActivityIndicatorView.Style = .gray) {
        self.init(style: style)
        hidesWhenStopped = true
        view.addSubview(self) {
            $0.center.alignWithSuperview()
        }
    }
    
    
    /// Instanciate a activityIndicatorView and add it at center in the given parameter view
    ///
    /// - Parameter view: view in which will be added the activityIndicatorView
    /// - Returns: the instanciate activityIndicatorView
    static func make(for view: UIView, style: UIActivityIndicatorView.Style = .gray) -> UIActivityIndicatorView {
        let indicatorView = UIActivityIndicatorView(style: style)
        
        indicatorView.hidesWhenStopped = true
        
        view.addSubview(indicatorView) {
            $0.center.alignWithSuperview()
        }
        
        return indicatorView
    }
}
