//
//  LoadableView.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation

protocol LoadableView {
    
    var indicatorView: UIActivityIndicatorView { get }
    
    func startLoading()
    
    func stopLoading()
}

extension LoadableView {
    
    func startLoading() {
        if !indicatorView.isAnimating {
            indicatorView.startAnimating()
            indicatorView.superview?.isUserInteractionEnabled = false
        }
    }
    
    func stopLoading() {
        indicatorView.superview?.isUserInteractionEnabled = true
        indicatorView.stopAnimating()
    }
    
}
