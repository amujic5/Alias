//
//  RepositoryRootView.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import CoordinatorKit
import SupaGithubCoreKit

protocol RepositoryRootViewDelegate: class {
    func repositoryRootView(_ view: RepositoryRootView,
                            didSelect cellViewModel: RepositoryCellViewModel)

    func repositoryRootView(_ view: RepositoryRootView,
                            didSelectUserImageView imageView: UIImageView,
                            userViewModel: UserDetailViewModel)

    func repositoryRootView(_ view: RepositoryRootView,
                            needToLoaMoreAt indexPath: IndexPath)
}

final class RepositoryRootView: NiblessView, LoadableView {
    lazy var indicatorView = UIActivityIndicatorView(for: self)

    unowned var delegate: RepositoryRootViewDelegate
    
    let tableView = UITableView()
    
    let emptyDataSetView: EmptyDataSetView = {
        let viewModel = EmptyDataSetViewModel.searchGithub
        let view = EmptyDataSetView(viewModel:
            viewModel)
        return view
    }()
    
    var viewModel = RepositoryViewModel(sections: [])
    
    lazy var tableViewConfiguration = RepositoryTableViewConfiguration(rootView: self)
    
    
    init(delegate: RepositoryRootViewDelegate) {
        self.delegate = delegate
        super.init(frame: .zero)
        constructView()
        tableViewConfiguration.configure(tableView)
        configure(with: viewModel)
    }
    
    func constructView() {
        addSubview(tableView) {
            $0.edges.pinToSuperview()
        }
        addSubview(emptyDataSetView) {
            $0.edges.pinToSuperview()
        }

    }
    
    func loadView() {
        self.viewModel.sections = []
        self.tableView.reloadData()
        self.tableView.isHidden = true
        self.emptyDataSetView.isHidden = true
        startLoading()
    }

    func stopLoadView() {
        self.tableView.isHidden = false
        stopLoading()
    }

}

// MARK: - ViewConfigurable
extension RepositoryRootView {
        
    func configure(with viewModel: RepositoryViewModel) {
        self.viewModel = viewModel
        emptyDataSetView.configure(with: viewModel.emptyDataSetViewModel)
        emptyDataSetView.isHidden = !viewModel.sections.isEmpty
        tableView.reloadData()
    }
    
}


// MARK: - UITableViewDelegate
extension RepositoryRootView: UITableViewDelegate {
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard let row = viewModel.row(at: indexPath) else {
                print("Warning cells and viewmodel.cells doesn't have the same content ")
            return
        }
        self.delegate
            .repositoryRootView(self,
                                didSelect: row)
        
    }
}


// MARK: - RepositoryTableViewCellDelegate
extension RepositoryRootView: RepositoryTableViewCellDelegate {
    func cell(_ cell: RepositoryTableViewCell,
              didTapUserImageWith viewModel: UserDetailViewModel) {
        self.delegate
            .repositoryRootView(self,
                                didSelectUserImageView: cell.userImageView,
                                userViewModel: viewModel)
    }
}
