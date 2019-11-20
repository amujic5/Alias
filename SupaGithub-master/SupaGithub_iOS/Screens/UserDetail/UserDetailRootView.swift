// UserDetailRootView.swift
// SupaGithub
//
// Created by Mohamed Ali on 14/06/2019.
//Copyright © 2019 GeekDev. All rights reserved.
//

import Yalta
import SupaGithubCoreKit

protocol UserDetailRootViewDelegate: class {
    
}

final class UserDetailRootView: NiblessView {
    
    let userImageView = UIImageView(style: Style.ImageView.bigThumbnail)
    let usernameLabel = UILabel(style: Style.Label.title)
    let companyLabel = UILabel(style: Style.Label.body)
    let blogLabel = UILabel(style: Style.Label.body)
    let locationLabel = UILabel(style: Style.Label.body)
    let emailLabel = UILabel(style: Style.Label.body)
    let bioLabel = UILabel(style: Style.Label.body)
    
    lazy var userInfoView: UIView = self.constructUserInfo() as! UIStackView
    lazy var userImageAndNameView: UIView = self.constructUserImageAndName()
    
    unowned var delegate: UserDetailRootViewDelegate
    
    init(delegate: UserDetailRootViewDelegate) {
        self.delegate = delegate
        super.init(frame: .zero)
        constructView()
    }
    
    func constructView() {
        let marginInset: CGFloat = 10
        
        addSubview(userImageAndNameView) {
            $0.leading.pinToSuperview(inset: marginInset)
            $0.top.pinToSuperview(inset: marginInset)
            $0.width.match(self.al.width * 0.35)
        }
        addSubview(userInfoView) {
            $0.trailing.pinToSuperview(inset: marginInset)
            $0.width.match(self.al.width * 0.5)
            $0.centerY.align(with: userImageAndNameView.al.centerY)
        }
        
        addSubview(bioLabel) {
            $0.leading.align(with: userImageAndNameView.al.leading)
            $0.trailing.align(with: userInfoView.al.trailing)
            $0.top.align(with: userInfoView.al.bottom + marginInset)
            $0.bottom.pinToSuperview(inset: marginInset, relation: .lessThanOrEqual)
        }
        
    }
    
    func constructUserImageAndName() -> UIView {
        let marginInset: CGFloat = 10
        let container = UIView()
        container.addSubview(userImageView) {
            $0.top.pinToSuperview(inset: marginInset)
            $0.centerX.alignWithSuperview()
        }
        usernameLabel.textAlignment = .center
        container.addSubview(usernameLabel) {
            $0.centerX.align(with: userImageView.al.centerX)
            $0.top.align(with: userImageView.al.bottom + marginInset)
            $0.leading.pinToSuperviewMargin()
            $0.width.match(container.al.width - 10)
            $0.bottom.pinToSuperviewMargin()
        }
        return container
    }
    
    func constructUserInfo() -> UIView {
        let company = makeIconAndLabel(imageName: "company",
                                       label: companyLabel)
        let location = makeIconAndLabel(imageName: "location",
                                        label: locationLabel)
        let link = makeIconAndLabel(imageName: "link",
                                    label: blogLabel)
        let email = makeIconAndLabel(imageName: "email",
                                     label: emailLabel)
        let views = [company, location, email, link]
        return UIStackView(arrangedSubviews: views,
                           axis: .vertical,
                           spacing: 20,
                           alignment: .fill,
                           distribution: .fillProportionally)
    }
    
    func makeIconAndLabel(imageName: String,
                          label: UILabel) -> UIStackView {
        let iconImageView = UIImageView {
            $0.image = UIImage(named: imageName)
            $0.contentMode = .scaleAspectFill
            $0.al.size.set(.init(width: 15, height: 15))
        }
        return UIStackView(arrangedSubviews: [iconImageView, label],
                           axis: .horizontal,
                           spacing: 5,
                           alignment: .fill,
                           distribution: .fillProportionally)
    }
    
}

// MARK: - ViewConfigurable
extension UserDetailRootView: ViewConfigurable {
    
    func configure(with viewModel: UserDetailViewModel) {
        if let url = viewModel.authorThumbnail {
            self.userImageView.loadImage(with: url)
        }
        self.usernameLabel.text = viewModel.authorName
        self.bioLabel.text = viewModel.bio
        self.companyLabel.text = viewModel.company
        self.blogLabel.text = viewModel.blog
        self.locationLabel.text = viewModel.location
        self.emailLabel.text = viewModel.email
    }
}
