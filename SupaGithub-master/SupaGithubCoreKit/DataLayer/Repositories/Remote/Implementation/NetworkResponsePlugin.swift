//
//  NetworkResponsePlugin.swift
//  SupaGithubCoreKit
//
//  Created by Mohamed Ali on 20/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import Moya
import Result

public struct ResponseProcessorPlugin: PluginType {
    
    public func process(_ result: Result<Response, MoyaError>, target: TargetType) -> Result<Response, MoyaError> {
        switch result {
        case .success(let response):
            let errorApi = try? APIError(data: response.data)
            if let error = errorApi {
                return .failure(.underlying(error, nil))
            } else if response.statusCode >= 400 {
                let errorMessage = String(data: response.data, encoding: .utf8) ?? ""
                let error = APIError(message: errorMessage,
                                     errors: nil)
                return .failure(.underlying(error, nil))
            }
            return result
        case .failure(_):
            return result
        }
    }
}
