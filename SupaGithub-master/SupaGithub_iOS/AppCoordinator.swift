//
//  AppCoordinator.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import CoordinatorKit
import SupaGithubCoreKit

final class AppCoordinator: BaseCoordinator {
    
    typealias FactoryView = LaunchViewControllerFactory
        & RepositoryControllerFactory
        & UserDetailControllerFactory
    typealias FactoryCoordinator = String
    
    let router: Router
    let factory: FactoryView
    let factoryCoordinator: FactoryCoordinator
    
    init(router: Router,
         factory: FactoryView,
         factoryCoordinator: FactoryCoordinator) {
        self.router = router
        self.factory = factory
        self.factoryCoordinator = factoryCoordinator
        super.init()
    }
    
    override func start() {
        self.showRepository()
    }
    
    func showLaunching() {
        let view = factory.makeLaunchViewController()
        self.router.setRootModule(view, hideBar: true)
    }
    
    func showRepository() {
        let view = factory.makeRepositoryController()
        view.onSelectUser = { [weak self] (viewModel, originView) in
            self?.showUserDetail(viewModel: viewModel,
                                 originView: originView)
        }
        self.router.setRootModule(view, hideBar: false)
    }
    
    func showUserDetail(viewModel: UserDetailViewModel,
                        originView: UIView) {
        let view = factory.makeUserDetailController(viewModel: viewModel)
        let transition = ExpendPopAnimationTransitionManager(fromView: originView)
        self.router.presentAsPopup(view, with: transition)
    }
    
}
