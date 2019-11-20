//
//  RepositoryInteractor.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import PromiseKit

public typealias RepositoryState = InteractorState<RepositoryViewModel>

public protocol RepositoryInteractorDelegate: class {
    
    /// Tells the delegate that the interactor state did update
    ///
    /// - Parameters:
    ///   - interactor: The interactor object informing the delegate of this impending event.
    ///   - state: The `interactor`  state
    func interactor(_ interactor: RepositoryInteractor,
                    didUpdate state: RepositoryState)

    func interactor(_ interactor: RepositoryInteractor,
                    didLoadNewCellsAt indexPaths: [IndexPath],
                    viewModel: RepositoryViewModel)
}

public final class RepositoryInteractor {
    
    public weak var delegate: RepositoryInteractorDelegate?
    
    /// The object describing the state of the Interactor
    private var state: RepositoryState = .idle {
        didSet {
            DispatchQueue.main.async {
                self.delegate?
                    .interactor(self, didUpdate: self.state)
            }
        }
    }
    
    private var viewModel: RepositoryViewModel?
    
    private var total = 0
    private var currentPage = 1
    private var query = ""
    private var order: OrderType = .asc
    private var sort: SortType = .stars
    private var hasMore: Bool {
        guard let cells = viewModel?.sections.first?.rows else {
            return false
        }
        return total > cells.count
    }
    
    private var isLoading = false
    
    private let githubRepository: GithubRepository
    
    public init(githubRepository: GithubRepository) {
        self.githubRepository = githubRepository
    }
    
}

// MARK: - Request
extension RepositoryInteractor {
    
    public func loadMore() {
        guard hasMore, !isLoading else {
            return
        }
        searchRequest(query: self.query, sort: self.sort,
                      order: self.order, page: self.currentPage)
            .done { [weak self] (newViewModel) in
                guard let strongSelf = self,
                    let newsRows = newViewModel.sections.first?.rows,
                    let viewModel = strongSelf.viewModel,
                    !viewModel.sections.isEmpty else {
                    return
                }
                let indexPaths = strongSelf.calculateIndexPathsToReload(from: newViewModel)
                strongSelf
                    .viewModel?
                    .sections[0].rows
                    .append(contentsOf: newsRows)
                if let viewModel = strongSelf.viewModel {
                    DispatchQueue.main.async {
                        strongSelf.delegate?
                            .interactor(strongSelf,
                                        didLoadNewCellsAt: indexPaths,
                                        viewModel: viewModel)
                    }
                }
            }.catch(handleError)
    }
    
    public func searchRepository(query: String, sort: SortType,
                                 order: OrderType) {
        if self.query == query && self.sort == sort &&
            self.order == order {
            return
        }
        self.state = .loading
        self.query = query
        self.sort = sort
        self.order = order
        self.currentPage = 1
        self.total = 0
        searchRequest(query: query, sort: sort,
                      order: order, page: self.currentPage)
            .done { [weak self] (viewModel) in
                self?.viewModel = viewModel
                self?.state = .success(viewModel)
            }.catch(handleError)
    }
    
    private func searchRequest(query: String, sort: SortType, order: OrderType,
                               page: Int) -> Promise<RepositoryViewModel> {
        isLoading = true
        return self.githubRepository
            .searchRepositories(query: query,
                                sort: sort,
                                order: order,
                                page: page)
            .compactMap { [weak self] resultRepository in
                guard let strongSelf = self else {
                    return nil
                }
                strongSelf.isLoading = false
                strongSelf.currentPage += 1
                strongSelf.total = resultRepository.totalCount
                return RepositoryViewModel(with: resultRepository, repositoryName: strongSelf.query)
        }
        
    }
    
    private func handleError(_ error: Error) {
        self.state = .failure(error)
    }
    
}

extension RepositoryInteractor {
    private func calculateIndexPathsToReload(from newViewModel: RepositoryViewModel) -> [IndexPath] {
        guard let viewModel = self.viewModel,
            let rows = viewModel.sections.first?.rows,
            let newRows = newViewModel.sections.first?.rows else {
            return []
        }
        let startIndex = rows.count - newRows.count
        let endIndex = startIndex + rows.count
        return (startIndex..<endIndex)
            .map { IndexPath(row: $0, section: 0) }
    }
}

public protocol RepositoryInteractorFactory {
    func makeRepositoryInteractor() -> RepositoryInteractor
}
