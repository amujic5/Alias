//
//  ListSectionModel.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 19/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation

public struct ListSectionModel<HeaderModelType, RowModelType, FooterModelType> {
    
    /// The section header model
    public let header: HeaderModelType?
    
    /// The section footer model
    public let footer: FooterModelType?
    
    /// The section's rows model
    public var rows: [RowModelType]
    
    public init(header: HeaderModelType? = nil,
         footer: FooterModelType? = nil,
         rows: [RowModelType]) {
        self.header = header
        self.footer = footer
        self.rows = rows
    }
}
