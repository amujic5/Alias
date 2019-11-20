//
//  MoyaGithubRemoteApi.swift
//  CoreKit
//
//  Created by Mohamed Ali on 11/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import PromiseKit

public final class MoyaGithubRemoteApi {

    let provider: GithubProvider
    private(set) static var baseUrl = ""
    
    public init(environment: GithubApiEnvironment) {
        self.provider = environment.provider
        MoyaGithubRemoteApi.baseUrl = environment.baseStringUrl
    }
    
}

extension MoyaGithubRemoteApi: GithubRemoteApi {

    public func getUser(username: String) -> Promise<User> {
        return provider
            .request(target: .getUser(username: username))
            .map { try User(data: $0.data) }
    }
    

    public func searchRepositories(query: String, sort: SortType, order: OrderType, page: Int) -> Promise<ResultRepository> {
        return provider
            .request(target: .repositories(query: query,
                                           sort: sort,
                                           order: order,
                                           page: page))
            .map { try ResultRepository(data: $0.data) }
    }

}
