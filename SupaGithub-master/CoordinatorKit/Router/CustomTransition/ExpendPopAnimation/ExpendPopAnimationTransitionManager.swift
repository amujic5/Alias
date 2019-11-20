//
//  CustomAnimationTransitionManager.swift
//  CoordinatorKit
//
//  Created by Mohamed Ali on 15/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation

/// The viewController that will be used as a popup has to conform to this protocol
/// to inform it's size
protocol PopupDisplayable {
    
    /// Content size desired
    /// if you want a dynamic size depending on the contentSize
    var contentSize: CGSize { get }
}

/// This class handle the transition between the PopupController and the PopupOptionsAnimator
/// It allows to synchronize the animation of transition and the size frame of the destination ViewController
final public class ExpendPopAnimationTransitionManager: NSObject {
    // the origin sender that pressed
    private weak var fromView: UIView?
    
    // maxSize of the frame that will be shown
    private var maxSize: CGSize
    
    // originFrame where the animation will start
    private var originFrame: CGRect {
        get {
            guard let sender = fromView else {
                return CGRect.zero
            }
            // convert relative coordinate of UIView to window coordinate
            return sender.convert(sender.bounds, to: nil)
        }
    }
    
    lazy var transition = ExpendPopAnimator(originFrame: self.originFrame,
                                            isPresenting: true)
    
    
    /// Contructor of PopupTransitionManager
    ///
    /// - Parameters:
    ///   - fromView: origin view where the animation will start
    ///   - maxSize (optional): max size of the frame that will be shown
    ///     by default CGSize(width: 300, height: 300)
    init(fromView: UIView, maxSize: CGSize = CGSize(width: 300, height: 300)) {
        self.fromView = fromView
        self.maxSize = maxSize
        super.init()
    }
    
    
    /// Compute the destination frame depend on the size
    ///
    /// - Parameter size: size desired
    /// - Returns: destinationFrame computed
    fileprivate func computeDestinationFrame(size: CGSize) -> CGRect {
        let midWidth = UIScreen.main.bounds.width / 2
        let midHeight = UIScreen.main.bounds.height / 2
        
        return CGRect(x: midWidth - size.width / 2,
                      y: midHeight - size.height / 2,
                      width: size.width, height: size.height)
    }
}


// MARK: - UIViewControllerTransitioningDelegate
extension ExpendPopAnimationTransitionManager: UIViewControllerTransitioningDelegate {
    public func presentationController(forPresented presented: UIViewController, presenting: UIViewController?, source: UIViewController) -> UIPresentationController? {
        var destinationFrame: CGRect
        
        if let popupView = presented as? PopupDisplayable {
            // we compute contentFrame for popupView contentSize
            let contentFrame = computeDestinationFrame(size: popupView.contentSize)
            // we compute maxFrame for maxSize
            let maxFrame = computeDestinationFrame(size: maxSize)
            // We take the intersection between maxFrame and contentFrame, which reflects the size to apply to the popup.
            destinationFrame = maxFrame.intersection(contentFrame)
        } else {
            print("You attempt to present a ViewController that not conform to PopupDisplayable, then the popup size by default will be \(maxSize)")
            destinationFrame = computeDestinationFrame(size: maxSize)
        }
        let popupController = PopupController(presentedViewController: presented, presenting: presenting, destinationFrame: destinationFrame)
        popupController.delegate = self
        return popupController
    }
    
    public func animationController(forPresented presented: UIViewController, presenting: UIViewController, source: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        transition.isPresenting = true
        return transition
    }
    
    public func animationController(forDismissed dismissed: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        transition.isPresenting = false
        return transition
    }
}


// MARK: - UIAdaptivePresentationControllerDelegate
extension ExpendPopAnimationTransitionManager: UIAdaptivePresentationControllerDelegate {
    public func adaptivePresentationStyle(for controller: UIPresentationController, traitCollection: UITraitCollection) -> UIModalPresentationStyle {
        if traitCollection.verticalSizeClass == .compact {
            return .custom
        } else {
            return .none
        }
    }
}
