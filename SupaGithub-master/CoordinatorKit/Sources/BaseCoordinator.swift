//
//  BaseCoordinator.swift
//  ClashTV
//
//  Created by Mohamed Ali BEN YAAGOUB on 17/12/2018.
//  Copyright © 2018 Mohamed Ali. All rights reserved.
//

import Foundation

public protocol Coordinator: class {
    func start()
}

open class BaseCoordinator: Coordinator {

    var childCoordinators: [Coordinator] = []

    public init() {}
    
    open func start() {
        fatalError("You have to override this function !")
    }

    // add only unique object
    public func addDependency(_ coordinator: Coordinator) {
        guard !childCoordinators.contains(where: { $0 === coordinator }) else { return }
        childCoordinators.append(coordinator)
    }

    public func removeDependency(_ coordinator: Coordinator?) {
        guard
            childCoordinators.isEmpty == false,
            let coordinator = coordinator
            else { return }

        // Clear child-coordinators recursively
        if let coordinator = coordinator as? BaseCoordinator, !coordinator.childCoordinators.isEmpty {
            coordinator.childCoordinators
                .filter({ $0 !== coordinator })
                .forEach({ coordinator.removeDependency($0) })
        }
        for (index, element) in childCoordinators.enumerated() where element === coordinator {
            childCoordinators.remove(at: index)
            break
        }
    }
}
