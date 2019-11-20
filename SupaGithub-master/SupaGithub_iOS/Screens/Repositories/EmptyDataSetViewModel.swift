//
//  EmptyDataSetViewModel.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 20/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import UIKit

public struct EmptyDataSetViewModel {
    public let title: String
    public let subtitle: String
    public let image: UIImage
}

extension EmptyDataSetViewModel {
    
    public static var searchGithub: EmptyDataSetViewModel {
        return EmptyDataSetViewModel(title: "Search Github",
                                     subtitle: "Find your favorite repositories",
                                     image: UIImage(named: "search")!)
    }
    
    static func notFoundRepository(for repositoryName: String) -> EmptyDataSetViewModel {
        return EmptyDataSetViewModel(title: "Oups",
                                     subtitle: "Cannot find a repository with name `\(repositoryName)`",
            image: UIImage(named: "close")!)
    }
}
