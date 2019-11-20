//
//  LaunchRootView.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import Yalta

final class LaunchRootView: NiblessView {
    
    override init(frame: CGRect = .zero) {
        super.init(frame: frame)
        styleView()
        configure()
    }
    
    func styleView() {
        // TODO : implement style
    }
    
    func configure() {
        let label = UILabel(style: Style.Label.body, ({
            $0.text = "Launching..."
        }))
        
        addSubview(label) {
            $0.center.alignWithSuperview()
        }
        
    }
    
}
