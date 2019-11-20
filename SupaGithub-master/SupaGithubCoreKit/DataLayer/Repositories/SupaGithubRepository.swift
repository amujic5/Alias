//
//  SupaGithubRepository.swift
//  CoreKit
//
//  Created by Mohamed Ali on 11/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import PromiseKit

public final class SupaGithubRepository {
    
    let remoteApi: GithubRemoteApi
    
    public init(remoteApi: GithubRemoteApi) {
        self.remoteApi = remoteApi
    }
    
}

extension SupaGithubRepository: GithubRepository {
    
    public func getUser(username: String) -> Promise<User> {
        return remoteApi
            .getUser(username: username)
    }
    
    public func searchRepositories(query: String, sort: SortType, order: OrderType, page: Int) -> Promise<ResultRepository> {
        return remoteApi.searchRepositories(query: query,
                                            sort: sort,
                                            order: order,
                                            page: page)
    }
}
