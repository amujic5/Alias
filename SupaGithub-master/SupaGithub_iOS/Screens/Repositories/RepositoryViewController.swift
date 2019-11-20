//
//  RepositoryViewController.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import CoordinatorKit
import SupaGithubCoreKit
import SafariServices

typealias ClosureUserDetailViewModel = (UserDetailViewModel, UIView) -> ()

/// Protocol that abstract the RepositoryViewController
protocol RepositoryView: BaseView {
    var onfinish: ClosureEmptyParameter? { get set }
    var onSelectUser: ClosureUserDetailViewModel? { get set }

}

final class RepositoryViewController: NiblessViewController, RepositoryView {
    
    // MARK: - RepositoryView
    var onfinish: ClosureEmptyParameter?
    var onSelectUser: ClosureUserDetailViewModel?

    
    // MARK: - Properties
    
    /// A wrapper to the root view
    var rootView: RepositoryRootView {
        return view as! RepositoryRootView
    }
    
    lazy var orderButton: UIButton = {
        // TODO: Replace with swiftGen
        let sortAsc = UIImage(named: "sort-asc")!
        let sortDesc = UIImage(named: "sort-desc")!
        let button = UIButton()
        button.tintColor = .black
        button.setImage(sortAsc, for: .normal)
        button.setImage(sortDesc, for: .selected)
        button.addTarget(self,
                         action: #selector(orderTapped),
                         for: .touchDown)
        button.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        return button
    }()

    lazy var sortButton: UIButton = {
        // TODO: Replace with swiftGen
        let sortAsc = UIImage(named: "star")!
        let sortDesc = UIImage(named: "git-branch")!
        let button = UIButton()
        button.tintColor = .black
        button.setImage(sortAsc, for: .normal)
        button.setImage(sortDesc, for: .selected)
        button.addTarget(self,
                         action: #selector(sortTapped),
                         for: .touchDown)
        button.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        return button
    }()

    var orderType: OrderType {
        return orderButton.isSelected ? .asc : .desc
    }

    var sortType: SortType {
        return sortButton.isSelected ? .forks : .stars
    }

    
    /// The object responsible of the view business logic
    lazy var interactor = interactorFactory
        .makeRepositoryInteractor()
    
    /// The factory that instanciate the interactor
    private let interactorFactory: RepositoryInteractorFactory
    
    private let debouncer = Debouncer()
    
    let searchController = UISearchController(searchResultsController: nil)
    
    // MARK: - LifeCycle
    init(interactorFactory: RepositoryInteractorFactory) {
        self.interactorFactory = interactorFactory
        super.init()
    }
    
    override func loadView() {
        view = RepositoryRootView(delegate: self)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        interactor.delegate = self
        title = "Repositories"
        setupSearchController()
        setupNavigationBar()
    }
    
    private func setupSearchController() {
        searchController.searchResultsUpdater = self
        searchController.obscuresBackgroundDuringPresentation = false
        searchController.searchBar.placeholder = "Search a repository"
        searchController.searchBar.showsCancelButton = false
        searchController.hidesNavigationBarDuringPresentation = false
        navigationItem.searchController = searchController
        navigationItem.hidesSearchBarWhenScrolling = false
        definesPresentationContext = true
    }
    
    private func setupNavigationBar() {
        let sortbuttonItem = UIBarButtonItem(customView: sortButton)
        let orderbuttonItem = UIBarButtonItem(customView: orderButton)
        self.navigationItem
            .rightBarButtonItems = [sortbuttonItem, orderbuttonItem]
    }
}

// MARK: - User Action
extension RepositoryViewController {
    @objc func sortTapped() {
        self.sortButton.isSelected.toggle()
        if let text = searchController.searchBar.text {
            searchRepository(query: text,
                             sort: sortType,
                             order: orderType)
        }
    }

    @objc func orderTapped() {
        self.orderButton.isSelected.toggle()
        if let text = searchController.searchBar.text {
            searchRepository(query: text,
                             sort: sortType,
                             order: orderType)
        }
    }

}

// MARK: - UISearchResultsUpdating
extension RepositoryViewController: UISearchResultsUpdating {
    func updateSearchResults(for searchController: UISearchController) {
        guard let searchText = searchController.searchBar.text else {
                return
        }
        debouncer.debounce(delay: 0.3, work: { [weak self] in
            guard let strongSelf = self else {
                return
            }
            strongSelf
                .searchRepository(query: searchText,
                                  sort: strongSelf.sortType,
                                  order: strongSelf.orderType)
        })
    }
}

// MARK: - Request
extension RepositoryViewController {
    
    func searchRepository(query: String, sort: SortType, order: OrderType) {
        guard !query.isEmpty else {
            return
        }
        interactor
            .searchRepository(query: query,
                              sort: sort,
                              order: order)
    }
}

// MARK: - RepositoryInteractorDelegate
extension RepositoryViewController: RepositoryInteractorDelegate {
    
    func interactor(_ interactor: RepositoryInteractor, didUpdate state: RepositoryState) {
        switch state {
        case .idle:
            rootView.loadView()
            break
        case .loading:
            rootView.loadView()
            break
        case .success(let viewModel):
            rootView.stopLoadView()
            rootView.configure(with: viewModel)
        case .failure(let error):
            rootView.stopLoadView()
            self.showError(error)
        }
    }
    
    func interactor(_ interactor: RepositoryInteractor, didLoadNewCellsAt indexPaths: [IndexPath], viewModel: RepositoryViewModel) {
        rootView.configure(with: viewModel)
    }

}

// MARK: - RepositoryRootViewDelegate
extension RepositoryViewController: RepositoryRootViewDelegate {
    
    func repositoryRootView(_ view: RepositoryRootView,
                            didSelectUserImageView imageView: UIImageView,
                            userViewModel: UserDetailViewModel) {
        onSelectUser?(userViewModel, imageView)
    }
    
    func repositoryRootView(_ view: RepositoryRootView, didSelect cellViewModel: RepositoryCellViewModel) {
        guard let baseUrl = cellViewModel.baseUrl else {
            self.showError(title: "Error",
                           message: "This repository doesn't exist")
            return
        }
        let viewController = SFSafariViewController(url: baseUrl)
        self.present(viewController,
                     animated: true, completion: nil)
    }
    
    func repositoryRootView(_ view: RepositoryRootView,
                            needToLoaMoreAt indexPath: IndexPath) {
        self.interactor.loadMore()
    }

    
}

/// RepositoryView Factory
protocol RepositoryControllerFactory {
    func makeRepositoryController() -> RepositoryView
}
