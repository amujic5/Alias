//
//  DisplayableError.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import UIKit
import SupaGithubCoreKit
import Moya

protocol DisplayableError {
    func showError(_ error: Error, retryClosure: ClosureEmptyParameter?)
    func showError(title: String, message: String,
                   retryClosure: ClosureEmptyParameter?)
}

extension UIViewController: DisplayableError {}

extension DisplayableError where Self: UIViewController {
    func showError(_ error: Error, retryClosure: ClosureEmptyParameter? = nil) {
        let title = "Error"
        // TODO: clean Error management this and throws always the same ErrorType
        let message = ErrorWrapper.message(for: error)
        
        self.showError(title: title,
                       message: message,
                       retryClosure: retryClosure)
    }
    
    func showError(title: String, message: String, retryClosure: ClosureEmptyParameter? = nil) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        let okAction = UIAlertAction(title: "OK", style: .default, handler: nil)
        
        alert.addAction(okAction)
        
        if let retry = retryClosure {
            let retryAction = UIAlertAction(title: "Retry", style: .default) { (action) in
                retry()
            }
            alert.addAction(retryAction)
        }
        self.present(alert, animated: true, completion: nil)
    }
    
}
