//
//  GithubApi.swift
//  CoreKit
//
//  Created by Mohamed Ali on 11/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import Moya

enum GithubApi {
    case repositories(query: String, sort: SortType,
        order: OrderType, page: Int)
    case getUser(username: String)
}

extension GithubApi: TargetType {
    var baseURL: URL {
        let stringUrl = MoyaGithubRemoteApi.baseUrl
        guard let url = URL(string: stringUrl) else {
            fatalError("Cannot run the app without a valid url")
        }
        return url
    }
    
    var path: String {
        switch self {
        case .repositories:
            return "search/repositories"
        case .getUser(let username):
            return "users/\(username)"
        }
    }
    
    var method: Moya.Method {
        switch self {
        case .repositories, .getUser:
            return .get
        }
    }
    
    /// Todo implement fake DATA
    var sampleData: Data {
        return Data()
    }
    
    var task: Task {
        switch self {
        case .repositories(let query, let sort, let order, let page):
            return .requestParameters(parameters: [
                "q" : query,
                "sort" : sort,
                "order" : order,
                "page" : page
                ], encoding: URLEncoding.default)
        case .getUser:
            return .requestPlain
        }

    }
    
    var headers: [String : String]? {
        return ["Accept" : "application/vnd.github.mercy-preview+json"]
    }
    
    
}
