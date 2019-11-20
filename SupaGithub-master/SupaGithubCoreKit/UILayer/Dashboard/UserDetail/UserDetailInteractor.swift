// UserDetailInteractor.swift
// SupaGithub
//
// Created by Mohamed Ali on 14/06/2019.
//Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation

public typealias UserDetailState = InteractorState<UserDetailViewModel>

public protocol UserDetailInteractorDelegate: class {
    
    /// Tells the delegate that the interactor state did update
    ///
    /// - Parameters:
    ///   - interactor: The interactor object informing the delegate of this impending event.
    ///   - state: The `interactor`  state
    func interactor(_ interactor: UserDetailInteractor,
                    didUpdate state: UserDetailState)
}

public final class UserDetailInteractor {
 
    public weak var delegate: UserDetailInteractorDelegate?
    
    /// The object describing the state of the Interactor
    private var state: UserDetailState = .idle {
        didSet {
            DispatchQueue.main.async {
                self.delegate?
                    .interactor(self, didUpdate: self.state)
            }
        }
    }
    
    private let githubRepository: GithubRepository
    
    public init(githubRepository: GithubRepository) {
        self.githubRepository = githubRepository
    }

    
    public func fetchUser(with username: String) {
        githubRepository
            .getUser(username: username)
            .map(UserDetailViewModel.init)
            .done { [weak self] (viewModel) in
                self?.state = .success(viewModel)
            }
            .catch(handleError)
    }
    
    private func handleError(_ error: Error) {
        self.state = .failure(error)
    }}

public protocol UserDetailInteractorFactory {
    func makeUserDetailInteractor() -> UserDetailInteractor
}
