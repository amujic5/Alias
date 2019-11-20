// UserDetailViewController.swift
// SupaGithub
//
// Created by Mohamed Ali on 14/06/2019.
//Copyright © 2019 GeekDev. All rights reserved.
//

import CoordinatorKit
import SupaGithubCoreKit

/// Protocol that abstract the UserDetailViewController
protocol UserDetailView: BaseView {
    var onfinish: ClosureEmptyParameter? { get set }
}

final class UserDetailViewController: NiblessViewController, UserDetailView {
    
    // MARK: - UserDetailView
    var onfinish: ClosureEmptyParameter?


    // MARK: - Properties

    /// A wrapper to the root view
    var rootView: UserDetailRootView {
        return view as! UserDetailRootView
    }
    
    /// The object responsible of the view business logic
    lazy var interactor = interactorFactory
        .makeUserDetailInteractor()
    
    /// The factory that instanciate the interactor
    private let interactorFactory: UserDetailInteractorFactory

    let viewModel: UserDetailViewModel
    
    // MARK: - LifeCycle
    init(interactorFactory: UserDetailInteractorFactory,
         viewModel: UserDetailViewModel) {
        self.interactorFactory = interactorFactory
        self.viewModel = viewModel
        super.init()
    }

    override func loadView() {
        view = UserDetailRootView(delegate: self)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.view.setCorner(radius: 12)
        interactor.delegate = self
        rootView.configure(with: viewModel)
        interactor.fetchUser(with: viewModel.authorName)
    }
}

// MARK: - UserDetailInteractorDelegate
extension UserDetailViewController: UserDetailInteractorDelegate {
    
    func interactor(_ interactor: UserDetailInteractor, didUpdate state: UserDetailState) {
        switch state {
        case .idle:
            break
        case .loading:
            break
        case .success(let viewModel):
            rootView.configure(with: viewModel)
        case .failure(let error):
            self.showError(error)
        }

    }
}

// MARK: - UserDetailRootViewDelegate
extension UserDetailViewController: UserDetailRootViewDelegate {
    
}

/// UserDetailView Factory
protocol UserDetailControllerFactory {
    func makeUserDetailController(viewModel: UserDetailViewModel) -> UserDetailView
}


