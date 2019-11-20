// UserDetailViewModel.swift
// SupaGithub
//
// Created by Mohamed Ali on 14/06/2019.
//Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation


public struct UserDetailViewModel {
    public let authorName: String
    public let authorThumbnail: URL?
    public let company: String?
    public let location: String?
    public let email: String?
    public let blog: String?
    public let bio: String?
}

extension UserDetailViewModel {
    public init(from user: User) {
        let unkown = "Unknown"
        self.authorName = user.login
        self.authorThumbnail = user.avatarURL
        self.company = user.company ?? unkown
        self.location = user.location ?? unkown
        self.blog = user.blog ?? unkown
        self.email = user.email ?? unkown
        self.bio = user.bio ?? unkown
    }
}
