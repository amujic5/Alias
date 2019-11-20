//
//  ViewConfigurable.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import SupaGithubCoreKit

/// This protocol can be implemented by views to be configurable
protocol ViewConfigurable {
    associatedtype ViewModel
    
    /// Configure the view with the given view model
    ///
    /// - Parameter viewModel: The view model used to configure the view
    func configure(with viewModel: ViewModel)
    
    func configure(for state: InteractorState<ViewModel>)
}

// MARK: - Default implementation
extension ViewConfigurable {
    func configure(for state: InteractorState<ViewModel>) {
        print("You should implement it")
    }
}

extension ViewConfigurable where Self: LoadableView & DisplayableError {
    func configure(for state: InteractorState<ViewModel>) {
        switch state {
        case .loading:
            startLoading()
        case .success(let viewModel):
            stopLoading()
            configure(with: viewModel)
        case .failure(let error):
            stopLoading()
            self.showError(error, retryClosure: nil)
        default:
            break
        }
    }
}
