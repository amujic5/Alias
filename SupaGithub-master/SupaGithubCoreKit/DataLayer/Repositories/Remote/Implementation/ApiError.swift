//
//  ApiError.swift
//  SupaGithubCoreKit
//
//  Created by Mohamed Ali on 20/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation

// MARK: - APIError
public struct APIError: Codable, Error {
    public let message: String
    public let errors: [ErrorDetail]?
}

// MARK: APIError convenience initializers and mutators

extension APIError {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(APIError.self, from: data)
    }
}

// MARK: - Error
public struct ErrorDetail: Codable {
    public let resource, field, code: String?
}
