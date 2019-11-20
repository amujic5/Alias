//
//  GithubRemoteApi.swift
//  CoreKit
//
//  Created by Mohamed Ali on 11/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import PromiseKit

public enum OrderType: String {
    case asc
    case desc
}

public enum SortType: String {
    case stars
    case forks
}

public protocol GithubRemoteApi {

    func searchRepositories(query: String,
                            sort: SortType,
                            order: OrderType,
                            page: Int) -> Promise<ResultRepository>
    
    func getUser(username: String) -> Promise<User>
    
}
