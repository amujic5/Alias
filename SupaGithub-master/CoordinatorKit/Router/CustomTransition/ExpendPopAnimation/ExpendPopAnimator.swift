//
//  ExpendPopAnimator.swift
//  CoordinatorKit
//
//  Created by Mohamed Ali on 15/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation

final public class ExpendPopAnimator: NSObject {
    private let originFrame: CGRect
    public var isPresenting: Bool

    init(originFrame: CGRect, isPresenting: Bool) {
        self.originFrame = originFrame
        self.isPresenting = isPresenting
        super.init()
    }
}

extension ExpendPopAnimator: UIViewControllerAnimatedTransitioning {
    public func transitionDuration(using transitionContext: UIViewControllerContextTransitioning?) -> TimeInterval {
        return 0.5
    }
    
    public func animateTransition(using transitionContext: UIViewControllerContextTransitioning) {
        if isPresenting {
            handlePresentingAnimation(using: transitionContext)
        } else {
            handleDismissingAnimation(using: transitionContext)
        }
    }
    
    
    func handleDismissingAnimation(using transitionContext: UIViewControllerContextTransitioning) {
        guard let controller = transitionContext.viewController(forKey: .from) else {
                return
        }
        let animationDuration = transitionDuration(using: transitionContext)
        UIView.animate(withDuration: animationDuration, delay: 0.0, usingSpringWithDamping: 1, initialSpringVelocity: 0, options: [.allowUserInteraction, .curveEaseInOut], animations: {
            controller.view.alpha = 0
        }, completion: {(completed: Bool) -> Void in
            transitionContext.completeTransition(completed)
        })

    }
    
    func handlePresentingAnimation(using transitionContext: UIViewControllerContextTransitioning) {
        guard let controller = transitionContext.viewController(forKey: .to),
            let toView = transitionContext.view(forKey: .to) else {
                return
        }
        
        let containerView = transitionContext.containerView
        let initialFrame = self.originFrame
        let finalFrame = transitionContext.finalFrame(for: controller)
        let userRootView = toView as? UserDetailRootView
        let animationDuration = transitionDuration(using: transitionContext)
        
        containerView.addSubview(toView)

        let imageView = UIImageView()
        imageView.image = userRootView!.userImageView.image
        containerView.addSubview(imageView)
        imageView.frame = initialFrame
        containerView.clipsToBounds = true
        controller.view.frame = initialFrame

        imageView.setCorner(radius: initialFrame.width / 2)

        toView.alpha = 0
        toView.frame = finalFrame
        userRootView!.userImageView
            .snapshotView(afterScreenUpdates: true)
        userRootView!.userImageView.alpha = 0
        toView.snapshotView(afterScreenUpdates: true)
        UIView.animateKeyframes(withDuration: animationDuration, delay: 0.0, options: .calculationModeCubic, animations: {
            UIView.addKeyframe(withRelativeStartTime: 0/4, relativeDuration: 1/4) {
                let imageFinalFrame = userRootView!.userImageView.convert(userRootView!.userImageView.bounds, to: nil)
                imageView.frame = imageFinalFrame
                imageView.setCorner(radius: imageFinalFrame.width / 2)
            }
            UIView.addKeyframe(withRelativeStartTime: 2/4, relativeDuration: 2/4) {
                toView.alpha = 1
                toView.transform = .identity
            }
        }) { (completed: Bool) -> Void in
            imageView.alpha = 0
            userRootView!.userImageView.alpha = 1
            transitionContext.completeTransition(completed)
        }
        
    }
    
    func handleAnimation(for view: UIView, initialFrame: CGRect, finalFrame: CGRect) -> CGAffineTransform {
        let xScaleFactor = finalFrame.width / initialFrame.width
        
        let yScaleFactor = finalFrame.height / initialFrame.height
        let scaleTransform = CGAffineTransform(scaleX: xScaleFactor, y: yScaleFactor)
        
        view.transform = scaleTransform
        view.center = CGPoint(
            x: initialFrame.midX,
            y: initialFrame.midY)
        return scaleTransform
    }
}
