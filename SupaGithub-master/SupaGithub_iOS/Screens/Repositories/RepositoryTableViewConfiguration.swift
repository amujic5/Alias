//
//  RepositoryTableViewConfiguration.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 17/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import SupaGithubCoreKit

final class RepositoryTableViewConfiguration: NSObject {
    
    typealias Section = RepositoryViewModel.Section
    unowned let rootView: RepositoryRootView
    var viewModel: RepositoryViewModel {
        return rootView.viewModel
    }
    
    var sections: [Section] {
        return viewModel.sections
    }

    init(rootView: RepositoryRootView) {
        self.rootView = rootView
        super.init()
    }
    
    public func configure(_ tableView: UITableView) {
        tableView.register(cellType: RepositoryTableViewCell.self)
        tableView.dataSource = self
        tableView.delegate = self
        tableView.remembersLastFocusedIndexPath = true
        tableView.showsVerticalScrollIndicator = false
        tableView.prefetchDataSource = self
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 100
        tableView.keyboardDismissMode = .interactive
        tableView.tableFooterView = UIView()
    }
}


// MARK: - UITableViewDataSource
extension RepositoryTableViewConfiguration: UITableViewDataSource {
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return viewModel.sections.count
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return viewModel.sections[section].rows.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(for: indexPath) as RepositoryTableViewCell
        let section = indexPath.section
        let row = indexPath.row
        if let rowViewModel = viewModel.sections[section].rows[row] {
            cell.configure(with: rowViewModel)
        } else {
            cell.configure(with: .none)
        }
        cell.delegate = rootView
        return cell
    }

}


// MARK: - UITableViewDelegate
extension RepositoryTableViewConfiguration: UITableViewDelegate {
    
    func tableView(_ tableView: UITableView,
                   didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard let row = viewModel.row(at: indexPath) else {
                print("Warning cells and viewmodel.cells doesn't have the same content ")
                return
        }
        self.rootView
            .delegate
            .repositoryRootView(rootView, didSelect: row)
        
    }
}

// MARK: - UITableViewDataSourcePrefetching
extension RepositoryTableViewConfiguration: UITableViewDataSourcePrefetching {
    
    func tableView(_ tableView: UITableView,
                   prefetchRowsAt indexPaths: [IndexPath]) {
        guard let lastIndexRow = sections.first?.rows.indices.last else {
            return
        }
        let lastIndexPath = IndexPath(row: lastIndexRow, section: 0)
        if indexPaths.contains(lastIndexPath) {
            self.rootView
                .delegate
                .repositoryRootView(rootView,
                                    needToLoaMoreAt: lastIndexPath)
        }
    }
}
