//
//  OptionsTransitionManager.swift
//  intuitiv
//
//  Created by Mohamed Ali BEN YAAGOUB on 15/05/2018.
//  Copyright © 2018 Netatmo. All rights reserved.
//

import Foundation
import UIKit

protocol PopupView {
    var contentSize: CGSize { get }
}

final public class PopupTransitionManager: NSObject {
    fileprivate var maxSize: CGSize

    fileprivate var originFrame: CGRect {
        return CGRect(x: (UIScreen.main.bounds.width / 2) - (maxSize.width / 2),
                      y: UIScreen.main.bounds.height,
                      width: maxSize.width, height: maxSize.height)
    }

    init(maxSize: CGSize = CGSize(width: 300, height: 300)) {
        // convert relative coordinate of uiButton to window coordinate
        self.maxSize = maxSize
        super.init()
    }

    fileprivate func computeDestinationFrame(size: CGSize) -> CGRect {
        let midWidth = UIScreen.main.bounds.width / 2
        let midHeight = UIScreen.main.bounds.height / 2

        return CGRect(x: midWidth - size.width / 2,
                      y: midHeight - size.height / 2,
                      width: size.width, height: size.height)
    }

}

extension PopupTransitionManager: UIViewControllerTransitioningDelegate {
    public func presentationController(forPresented presented: UIViewController, presenting: UIViewController?, source: UIViewController) -> UIPresentationController? {
        var destinationFrame: CGRect

        if let popupView = presented as? PopupView {
            let contentFrame = computeDestinationFrame(size: popupView.contentSize)
            let maxFrame = computeDestinationFrame(size: maxSize)
            destinationFrame = maxFrame.intersection(contentFrame)
        } else {
            destinationFrame = computeDestinationFrame(size: maxSize)
        }
        let popupController = PopupController(presentedViewController: presented,
                                              presenting: presenting,
                                              destinationFrame: destinationFrame)
        popupController.delegate = self
        return popupController
    }

    public func animationController(forPresented presented: UIViewController, presenting: UIViewController, source: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        return PopupOptionsAnimator(originFrame: originFrame,
                                    isPresentation: true)
    }

    public func animationController(forDismissed dismissed: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        return PopupOptionsAnimator(originFrame: originFrame,
                                    isPresentation: false)
    }
}

extension PopupTransitionManager: UIAdaptivePresentationControllerDelegate {
    public func adaptivePresentationStyle(for controller: UIPresentationController, traitCollection: UITraitCollection) -> UIModalPresentationStyle {
        if traitCollection.verticalSizeClass == .compact {
            return .custom
        } else {
            return .none
        }
    }
}
