//
//  ResultRepository.swift
//  CoreKit
//
//  Created by Mohamed Ali on 12/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation

// MARK: - ResultRepository
public struct ResultRepository: Codable {
    public let totalCount: Int
    public let incompleteResults: Bool?
    public let items: [Repository]
    
    enum CodingKeys: String, CodingKey {
        case totalCount = "total_count"
        case incompleteResults = "incomplete_results"
        case items
    }
    
    public init(totalCount: Int,
                incompleteResults: Bool?,
                items: [Repository]) {
        self.totalCount = totalCount
        self.incompleteResults = incompleteResults
        self.items = items
    }
}

// MARK: ResultRepository convenience initializers and mutators

extension ResultRepository {
    public init(data: Data) throws {
        self = try newJSONDecoder().decode(ResultRepository.self, from: data)
    }
    
    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }
    
    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }
        
    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }
    
    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}
