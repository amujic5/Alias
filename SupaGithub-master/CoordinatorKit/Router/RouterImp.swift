import UIKit

typealias TransitionManager = UIViewControllerTransitioningDelegate

public final class RouterImp: NSObject, Router {

    var transitionManager: TransitionManager?
    private weak var rootController: UINavigationController?
    private var completions: [UIViewController : () -> Void]
    
    public init(rootController: UINavigationController) {
        self.rootController = rootController
        completions = [:]
    }
    
    public func toPresent() -> UIViewController? {
        return rootController
    }
    
    public func present(_ module: Presentable?) {
        present(module, animated: true)
    }
    
    public func present(_ module: Presentable?, animated: Bool) {
        guard let controller = module?.toPresent() else { return }
        rootController?.present(controller, animated: animated, completion: nil)
    }
    
    public func presentAsPopup(_ module: Presentable?) {
        presentAsPopup(module, with: PopupTransitionManager())
    }

    public func presentAsPopup(_ module: Presentable?,
                               with transitionManager: UIViewControllerTransitioningDelegate)
    {
        guard let controller = module?.toPresent() else { return }
        self.transitionManager = PopupTransitionManager()
        controller.transitioningDelegate = transitionManager
        controller.modalPresentationStyle = .custom
        present(controller)
    }

    
    public func presentCustomAnimation(_ module: Presentable?, from view: UIView) {
        guard let controller = module?.toPresent() else { return }
        transitionManager = PopupTransitionManager()
        controller.transitioningDelegate = transitionManager
        controller.modalPresentationStyle = .custom
        present(controller)
    }

    
    public func dismissModule() {
        dismissModule(animated: true, completion: nil)
    }
    
    public func dismissModule(animated: Bool, completion: (() -> Void)?) {
        rootController?.dismiss(animated: animated, completion: completion)
    }
    
    public func push(_ module: Presentable?)  {
        push(module, animated: true)
    }
    
    public func push(_ module: Presentable?, animated: Bool)  {
        push(module, animated: animated, completion: nil)
    }
    
    public func push(_ module: Presentable?, animated: Bool, completion: (() -> Void)?) {
        guard
            let controller = module?.toPresent(),
            (controller is UINavigationController == false)
            else { assertionFailure("Deprecated push UINavigationController."); return }
        
        if let completion = completion {
            completions[controller] = completion
        }
        rootController?.pushViewController(controller, animated: animated)
    }
    
    public func popModule()  {
        popModule(animated: true)
    }
    
    public func popModule(animated: Bool)  {
        if let controller = rootController?.popViewController(animated: animated) {
            runCompletion(for: controller)
        }
    }
    
    public func setRootModule(_ module: Presentable?) {
        setRootModule(module, hideBar: false)
    }
    
    public func setRootModule(_ module: Presentable?, hideBar: Bool) {
        setRootModule(module, hideBar: hideBar, animated: false)
    }

    public func setRootModule(_ module: Presentable?, hideBar: Bool, animated: Bool) {
        guard let controller = module?.toPresent() else { return }
        rootController?.setViewControllers([controller], animated: animated)
        rootController?.isNavigationBarHidden = hideBar
    }
    
    public func popToRootModule(animated: Bool) {
        if let controllers = rootController?.popToRootViewController(animated: animated) {
            controllers.forEach { controller in
                runCompletion(for: controller)
            }
        }
    }
    
    private func runCompletion(for controller: UIViewController) {
        guard let completion = completions[controller] else { return }
        completion()
        completions.removeValue(forKey: controller)
    }
}
