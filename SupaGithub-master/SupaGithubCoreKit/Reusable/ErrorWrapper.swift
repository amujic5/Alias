//
//  ErrorWrapper.swift
//  SupaGithubCoreKit
//
//  Created by Mohamed Ali on 20/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import Moya

public struct ErrorWrapper {
    
    public static func message(for error: Error) -> String {
        var message = error.localizedDescription
        if let moyaError = error as? MoyaError {
            switch moyaError {
            case .underlying(let error, _):
                if let apiError = error as? APIError {
                    message = apiError.message
                }
            default:
                break
            }
        } else if let decodingError = error as? DecodingError {
            message = "\(decodingError)"
            print(decodingError)
        }
        return message
    }
}
