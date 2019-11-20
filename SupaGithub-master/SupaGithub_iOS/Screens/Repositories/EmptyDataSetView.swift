//
//  EmptyDataSetView.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 12/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import Yalta
import SupaGithubCoreKit


final class EmptyDataSetView: NiblessView {

    let titleLabel = UILabel(style: Style.Label.title, {
        $0.textAlignment = .center
    })
    let subtitleLabel = UILabel(style: Style.Label.body, {
        $0.textAlignment = .center
    })
    let imageView = UIImageView {
        $0.contentMode = .scaleAspectFill
        $0.tintColor = .lightGray
    }

    init(viewModel: EmptyDataSetViewModel) {
        super.init(frame: .zero)
        constructView()
        configure(with: viewModel)
    }
    
    func constructView() {
        let container = UIView()
        let margin: CGFloat = 20.0
        container.addSubview(imageView) {
            $0.centerX.alignWithSuperview()
            $0.top.pinToSuperviewMargin()
            $0.height.set(80)
            $0.width.set(80)
        }
        container.addSubview(titleLabel) {
            $0.edges(.left, .right).pinToSuperview()
            $0.top.align(with: imageView.al.bottom + margin)
        }
        container.addSubview(subtitleLabel) {
            $0.edges(.left, .right, .bottom)
                .pinToSuperview()
            $0.top.align(with: titleLabel.al.bottom + margin)
        }
        addSubview(container) {
            $0.centerX.alignWithSuperview()
            $0.centerY.alignWithSuperview(offset: -40)
            $0.width.match(al.width * 0.8)
        }
    }
}

// MARK: - ViewConfigurable
extension EmptyDataSetView: ViewConfigurable {

    func configure(with viewModel: EmptyDataSetViewModel) {
        self.titleLabel.text = viewModel.title
        self.subtitleLabel.text = viewModel.subtitle
        self.imageView.image = viewModel.image
    }
    
}
