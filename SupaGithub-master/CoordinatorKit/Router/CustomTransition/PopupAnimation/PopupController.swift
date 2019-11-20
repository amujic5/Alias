//
//  PopupController.swift
//  intuitiv
//
//  Created by Mohamed Ali BEN YAAGOUB on 15/05/2018.
//  Copyright © 2018 Netatmo. All rights reserved.
//

import Foundation
import UIKit
import Yalta

class PopupController: UIPresentationController {
    // MARK: - Properties
    private var dimmingView = UIView()
    
    public let destinationFrame: CGRect
    
    override var frameOfPresentedViewInContainerView: CGRect {
        return self.destinationFrame
    }
    
    // MARK: - Initializers
    init(presentedViewController: UIViewController, presenting presentingViewController: UIViewController?, destinationFrame: CGRect) {
        self.destinationFrame = destinationFrame
        
        super.init(presentedViewController: presentedViewController, presenting: presentingViewController)
        self.setupDimmingView()
    }
    
    
    override func presentationTransitionWillBegin() {
        containerView?.insertSubview(dimmingView, at: 0)
        guard let safeContainerView = containerView else {
            print("Can't get containerView")
            return
        }
        dimmingView
            .topAnchor
            .constraint(equalTo: safeContainerView.topAnchor)
            .isActive = true
        dimmingView
            .bottomAnchor
            .constraint(equalTo: safeContainerView.bottomAnchor)
            .isActive = true
        dimmingView
            .leftAnchor
            .constraint(equalTo: safeContainerView.leftAnchor)
            .isActive = true
        dimmingView
            .rightAnchor
            .constraint(equalTo: safeContainerView.rightAnchor)
            .isActive = true
        
        guard let coordinator = presentedViewController.transitionCoordinator else {
            dimmingView.alpha = 1.0
            return
        }
        
        coordinator.animate(alongsideTransition: { _ in
            self.dimmingView.alpha = 1.0
        })
    }
    
    override func dismissalTransitionWillBegin() {
        guard let coordinator = presentedViewController.transitionCoordinator else {
            dimmingView.alpha = 0.0
            return
        }
        
        coordinator.animate(alongsideTransition: { _ in
            self.dimmingView.alpha = 0.0
        })
    }
    
    override func preferredContentSizeDidChange(forChildContentContainer container: UIContentContainer) {
        super.preferredContentSizeDidChange(forChildContentContainer: container)
        containerView?.sizeThatFits(container.preferredContentSize)
    }
}

// MARK: - Private
private extension PopupController {
    
    func setupDimmingView() {
        dimmingView = UIView()
        dimmingView.translatesAutoresizingMaskIntoConstraints = false
        dimmingView.backgroundColor = UIColor(white: 0.0, alpha: 0.5)
        dimmingView.alpha = 0.0
        
        let recognizer = UITapGestureRecognizer(target: self, action: #selector(handleTap(recognizer:)))
        dimmingView.addGestureRecognizer(recognizer)
    }
    
    @objc dynamic func handleTap(recognizer: UITapGestureRecognizer) {
        presentingViewController.dismiss(animated: true)
    }
}

