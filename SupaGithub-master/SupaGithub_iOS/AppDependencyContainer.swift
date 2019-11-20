//
//  AppDependencyContainer.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import CoordinatorKit
import UIKit
import SupaGithubCoreKit

public final class AppDependencyContainer {
    
    // MARK:  - Properties
    private let rootViewController = UINavigationController()
    
    private lazy var router = RouterImp(rootController: self.rootViewController)
    
    private lazy var appCoordinator = self.makeAppCoordinator()
    
    private lazy var githubRepository = self.makeGithubRepository()
    
    // MARK: - Methods
    public init() {
    }
    
    public func makeMainViewController() -> UIViewController {
        self.appCoordinator.start()
        return rootViewController
    }
    
    private func makeAppCoordinator() -> AppCoordinator {
        return AppCoordinator(router: router,
                              factory: self,
                              factoryCoordinator: "self")
    }
    
    private func makeGithubRepository() -> GithubRepository {
        let remoteApi = MoyaGithubRemoteApi(environment: .production)
        return SupaGithubRepository(remoteApi: remoteApi)
    }
}

// MARK: - RepositoryControllerFactory, RepositoryInteractorFactory
extension AppDependencyContainer: RepositoryControllerFactory, RepositoryInteractorFactory {
    
    public func makeRepositoryInteractor() -> RepositoryInteractor {
        return RepositoryInteractor(githubRepository: self.githubRepository)
    }
    
    func makeRepositoryController() -> RepositoryView {
        let viewController = RepositoryViewController(interactorFactory: self)
        return viewController
    }
}


// MARK: - UserDetailControllerFactory, UserDetailInteractorFactory
extension AppDependencyContainer: UserDetailControllerFactory, UserDetailInteractorFactory {
 
    func makeUserDetailController(viewModel: UserDetailViewModel) -> UserDetailView {
        return UserDetailViewController(interactorFactory: self,
                                        viewModel: viewModel)
    }

    public func makeUserDetailInteractor() -> UserDetailInteractor {
        return UserDetailInteractor(githubRepository: githubRepository)
    }
}

// MARK: - LaunchViewControllerFactory
extension AppDependencyContainer: LaunchViewControllerFactory {

    func makeLaunchViewController() -> LaunchViewController {
        return LaunchViewController()
    }
    
}
