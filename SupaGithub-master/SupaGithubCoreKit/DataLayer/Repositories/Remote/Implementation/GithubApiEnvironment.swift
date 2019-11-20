//
//  GithubApiEnvironment.swift
//  CoreKit
//
//  Created by Mohamed Ali on 11/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import Moya

typealias GithubProvider = MoyaProvider<GithubApi>

public enum GithubApiEnvironment {
    case production
    case mock
}

extension GithubApiEnvironment {
    var provider: GithubProvider {
        switch self {
        case .mock:
            return GithubProvider(stubClosure: { _ in
                return .immediate
            })
        default:
            let responseProcessing = ResponseProcessorPlugin()
            var plugins: [PluginType] = [responseProcessing]
            #if DEBUG
            plugins.append(NetworkLoggerPlugin(verbose: true))
            #endif
            return GithubProvider(plugins: plugins)
        }
    }
}

extension GithubApiEnvironment {
    public var baseStringUrl: String {
        switch self {
        case .production:
            return "https://api.github.com/"
        case .mock:
            return ""
        }
    }
}
