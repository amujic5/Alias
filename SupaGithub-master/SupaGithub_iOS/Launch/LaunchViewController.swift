//
//  LaunchViewController.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import UIKit

public final class LaunchViewController: NiblessViewController {
    
    // MARK: - Methods
    public override func loadView() {
        view = LaunchRootView()
    }

}

protocol LaunchViewControllerFactory {
    
    func makeLaunchViewController() -> LaunchViewController
}
