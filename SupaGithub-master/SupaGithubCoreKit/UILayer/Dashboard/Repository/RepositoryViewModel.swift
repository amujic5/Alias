//
//  RepositoryViewModel.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation

public struct RepositoryCellViewModel {
    public let repositoryName: String
    public let description: String?
    public let forksCount: String
    public let starsCount: String
    public let baseUrl: URL?
    public let language: String
    public let userDetailViewModel: UserDetailViewModel
}

extension RepositoryCellViewModel {
    init(with repository: Repository) {
        self.description = repository.itemDescription
        self.repositoryName = repository.name
        self.forksCount = "\(repository.forksCount)"
        self.starsCount = "\(repository.stargazersCount)"
        self.baseUrl = URL(string: repository.htmlURL)
        self.userDetailViewModel = UserDetailViewModel(from: repository.owner)
        self.language = repository.language ?? "unknown"
    }
}

public struct RepositoryViewModel {
    public typealias Section = ListSectionModel<Void, RepositoryCellViewModel?, Void>

    public var sections: [Section]
    public var emptyDataSetViewModel: EmptyDataSetViewModel

    public init(sections: [Section],
                emptyDataSetViewModel: EmptyDataSetViewModel = .searchGithub) {
        self.sections = sections
        self.emptyDataSetViewModel = emptyDataSetViewModel
    }
}

extension RepositoryViewModel {
    init(with repositoryResult: ResultRepository,
         repositoryName: String) {
        var sections = [Section]()
        let cells = repositoryResult
            .items
            .map(RepositoryCellViewModel.init)
        if cells.count > 0 {
            let repositorySection = Section(rows: cells)
            sections.append(repositorySection)
        }
        if repositoryResult.totalCount > cells.count {
            sections.append(Section(rows: [.none]))
        }
        self.sections = sections
        if repositoryName.isEmpty {
            self.emptyDataSetViewModel = .searchGithub
        } else {
            self.emptyDataSetViewModel = .notFoundRepository(for: repositoryName)
        }
    }
}

extension RepositoryViewModel {

    private func sections(sections: [Section],
                  contains indexPath: IndexPath) -> Bool {
        let section = indexPath.section
        let row = indexPath.row
        return sections.indices.contains(section) && sections[section].rows.indices.contains(row)
    }
    
    public func isLastRowInFirstSection(_ indexPath: IndexPath) -> Bool {
        guard sections(sections: sections, contains: indexPath),
            let lastIndex = self.sections[0].rows.indices.last else {
            return false
        }
        return lastIndex == indexPath.row
    }
    
    public func row(at indexPath: IndexPath) -> RepositoryCellViewModel? {
        guard sections(sections: sections, contains: indexPath) else {
            return nil
        }
        return sections[indexPath.section]
            .rows[indexPath.row]
    }

}
