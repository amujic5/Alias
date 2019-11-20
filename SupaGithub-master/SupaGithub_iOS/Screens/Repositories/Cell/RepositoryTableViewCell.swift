//
//  RepositoryTableViewCell.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 12/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import Yalta
import Reusable
import SupaGithubCoreKit

protocol RepositoryTableViewCellDelegate: class {
    func cell(_ cell: RepositoryTableViewCell,
              didTapUserImageWith viewModel: UserDetailViewModel)
}

final class RepositoryTableViewCell: UITableViewCell, Reusable, LoadableView {
    
    // MARK: - Views
    let userImageView = UIImageView(style: Style.ImageView.smallCircled)
    let userLabel = UILabel(style: Style.Label.body, {
        $0.font = .systemFont(ofSize: 11, weight: .regular)
        $0.numberOfLines = 1
        $0.textAlignment = .center
        $0.lineBreakMode = .byTruncatingTail
    })
    let repositoryNameLabel = UILabel(style: Style.Label.title)
    let forkLabel = UILabel(style: Style.Label.body)
    let starLabel = UILabel(style: Style.Label.body)
    let languageLabel = UILabel(style: Style.Label.body)
    let descriptionLabel = UILabel(style: Style.Label.body)
    lazy var indicatorView = UIActivityIndicatorView(for: self)
    
    
    // MARK: - Properties
    weak var delegate: RepositoryTableViewCellDelegate?

    var viewModel: RepositoryCellViewModel?

    // MARK: - LifeCycle
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        constructView()
        setupGestureUserImageView()
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func constructView() {
        let marginInset: CGFloat = 20
        
        contentView.addSubview(userImageView) {
            $0.leading.pinToSuperview(inset: marginInset)
            $0.top.pinToSuperview(inset: marginInset)
        }
        contentView.addSubview(userLabel) {
            $0.centerX.align(with: userImageView.al.centerX)
            $0.leading.pinToSuperview(inset: 5)
            $0.top.align(with: userImageView.al.bottom + marginInset / 2)
        }
        contentView.addSubview(repositoryNameLabel) {
            $0.leading.align(with: userImageView.al.trailing + marginInset)
            $0.trailing.align(with: self.contentView.al.trailing - marginInset)
            $0.top.align(with: userImageView.al.top)
        }
        let footer = makeFooterStackView()
        contentView.addSubview(footer) {
            $0.edges(.left, .right).pinToSuperviewMargins()
            $0.bottom.align(with: self.contentView.al.bottom - marginInset)
        }
        contentView.addSubview(descriptionLabel) {
            $0.top.align(with: repositoryNameLabel.al.bottom + marginInset)
            $0.leading.align(with: repositoryNameLabel.al.leading)
            $0.trailing.align(with: repositoryNameLabel.al.trailing)
            $0.bottom.align(with: footer.al.top - marginInset)
            $0.height.set(40)
        }
        
    }
    
    func makeFooterStackView() -> UIView {
        let forkStackView = makeIconAndLabel(imageName: "git-branch", label: forkLabel)
        let starStackView = makeIconAndLabel(imageName: "star", label: starLabel)
        let languageStackView = makeIconAndLabel(imageName: "code", label: languageLabel)
        let views = [
            forkStackView,
            starStackView,
            languageStackView,
        ]
        return UIStackView(arrangedSubviews: views,
                           axis: .horizontal,
                           alignment: .fill,
                           distribution: .fillEqually
        )
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
                           spacing: 4,
                           alignment: .fill,
                           distribution: .fillProportionally)
    }
    
    func makeHorizontalStackView(for views: [UIView]) -> UIStackView {

        return UIStackView(arrangedSubviews: views,
                           axis: .horizontal,
                           spacing: 4,
                           alignment: .fill,
                           distribution: .fillProportionally)
    }
    
    func setupGestureUserImageView() {
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(imageTapped(gesture:)))
        
        // add it to the image view;
        userImageView.addGestureRecognizer(tapGesture)
        // make sure imageView can be interacted with by user
        userImageView.isUserInteractionEnabled = true
    }
    
    @objc func imageTapped(gesture: UIGestureRecognizer) {
        // if the tapped view is a UIImageView then set it to imageview
        if let viewModel = self.viewModel,
            (gesture.view as? UIImageView) != nil {
            let userViewModel = viewModel.userDetailViewModel
            delegate?
                .cell(self,
                      didTapUserImageWith: userViewModel)
            
        }
    }
}

// MARK: - ViewConfigurable
extension RepositoryTableViewCell: ViewConfigurable {
    
    func configure(with viewModel: RepositoryCellViewModel?) {
        self.viewModel = viewModel
        if let safeViewModel = viewModel {
            stoploadingCell()
            configureViewWithSafeViewModel(safeViewModel)
        } else {
            startLoadingCell()
        }
    }
    
    func startLoadingCell() {
        startLoading()
        contentView.alpha = 0
    }

    func stoploadingCell() {
        stopLoading()
        contentView.alpha = 1
    }

    private func configureViewWithSafeViewModel(_ viewModel: RepositoryCellViewModel) {
        if let url = viewModel.userDetailViewModel.authorThumbnail {
            userImageView.loadImage(with: url)
        } else {
            userImageView.image = UIImage(named: "defaultImage")
        }
        repositoryNameLabel.text = viewModel.repositoryName
        descriptionLabel.text = viewModel.description
        userLabel.text = viewModel.userDetailViewModel.authorName
        forkLabel.text = viewModel.forksCount
        starLabel.text = viewModel.starsCount
        languageLabel.text = viewModel.language
    }
    
}
