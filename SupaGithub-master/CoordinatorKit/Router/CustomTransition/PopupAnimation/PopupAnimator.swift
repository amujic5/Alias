//
//  AdvancedOptionsAnimator.swift
//  intuitiv
//
//  Created by Mohamed Ali BEN YAAGOUB on 15/05/2018.
//  Copyright © 2018 Netatmo. All rights reserved.
//

import Foundation
import UIKit

final public class PopupOptionsAnimator: NSObject {
    fileprivate let originFrame: CGRect
    
    public var isPresenting: Bool

    init(originFrame: CGRect, isPresentation: Bool) {
        self.originFrame = originFrame
        self.isPresenting = isPresentation
        super.init()
    }
}

extension PopupOptionsAnimator: UIViewControllerAnimatedTransitioning {
    public func transitionDuration(using transitionContext: UIViewControllerContextTransitioning?) -> TimeInterval {
        return 3
    }

    public func animateTransition(using transitionContext: UIViewControllerContextTransitioning) {
        let key = self.isPresenting ? UITransitionContextViewControllerKey.to : UITransitionContextViewControllerKey.from
        guard let controller = transitionContext.viewController(forKey: key) else { return }
        if self.isPresenting {
            transitionContext.containerView.addSubview(controller.view)
            transitionContext.view(forKey: .from)
        }

        let presentedFrame = transitionContext.finalFrame(for: controller)
        var dismissedFrame = presentedFrame

        dismissedFrame = self.originFrame

        let initialFrame = self.isPresenting ? dismissedFrame : presentedFrame
        let finalFrame = self.isPresenting ? presentedFrame : dismissedFrame

        let animationDuration = transitionDuration(using: transitionContext)
        controller.view.frame = initialFrame

        controller.view.autoresizingMask = [.flexibleLeftMargin, .flexibleBottomMargin, .flexibleTopMargin]

        if !self.isPresenting {
            UIView.animate(withDuration: animationDuration, delay: 0.0, usingSpringWithDamping: 1, initialSpringVelocity: 0, options: [.allowUserInteraction, .curveEaseInOut], animations: {
                controller.view.alpha = 0
            }, completion: {(completed: Bool) -> Void in
                transitionContext.completeTransition(completed)
            })
        } else {
             UIView.animate(withDuration: animationDuration, delay: 0.0, usingSpringWithDamping: 1, initialSpringVelocity: 0, options: [.allowUserInteraction, .curveEaseInOut], animations: {
                controller.view.frame = finalFrame
            }, completion: {(completed: Bool) -> Void in
                transitionContext.completeTransition(completed)
            })
        }
    }

}
