//
//  Repository.swift
//  CoreKit
//
//  Created by Mohamed Ali on 12/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation


// MARK: - Repository
public struct Repository: Codable {
    public let id: Int
    public let nodeID, name, fullName: String
    public let owner: User
    public let itemPrivate: Bool
    public let htmlURL: String
    public let itemDescription: String?
    public let fork: Bool
    public let url: String?
    public let createdAt, updatedAt: Date?
    public let homepage: String?
    public let size, stargazersCount, watchersCount: Int
    public let language: String?
    public let forksCount, openIssuesCount: Int
    public let masterBranch, defaultBranch: String?
    public let score: Double
    
    enum CodingKeys: String, CodingKey {
        case id
        case nodeID = "node_id"
        case name
        case fullName = "full_name"
        case owner
        case itemPrivate = "private"
        case htmlURL = "html_url"
        case itemDescription = "description"
        case fork, url
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case homepage, size
        case stargazersCount = "stargazers_count"
        case watchersCount = "watchers_count"
        case language
        case forksCount = "forks_count"
        case openIssuesCount = "open_issues_count"
        case masterBranch = "master_branch"
        case defaultBranch = "default_branch"
        case score
    }
    
}

// MARK: Repository convenience initializers and mutators

extension Repository {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Repository.self, from: data)
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
