//
//  FakeGithubRepository.swift
//  SupaGithubCoreKitTests
//
//  Created by Mohamed Ali on 19/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import SupaGithubCoreKit
import PromiseKit

enum FakeError: Error {
    case badContent
}

final class FakeGithubRemoteApi: GithubRemoteApi {
    var isFailed = false
    
    func searchRepositories(query: String, sort: SortType, order: OrderType, page: Int) -> Promise<ResultRepository> {
        guard !isFailed else {
            return .init(error: FakeError.badContent)
        }
        let data = FakeData.fakeRepositories.data(using: .utf8)!
        let result = try! ResultRepository(data: data)
        return .value(result)
    }
    
    func getUser(username: String) -> Promise<User> {
        guard !isFailed else {
            return .init(error: FakeError.badContent)
        }
        let data = Data()
        return .value(try! User(data: data))
    }
    
    
}
