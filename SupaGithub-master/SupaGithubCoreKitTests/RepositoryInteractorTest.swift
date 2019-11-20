//
//  RepositoryInteractorTest.swift
//  SupaGithubCoreKitTests
//
//  Created by Mohamed Ali on 19/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import XCTest
@testable import SupaGithubCoreKit

class RepositoryInteractorTest: XCTestCase {

    let remoteApi = FakeGithubRemoteApi()
    lazy var repository = SupaGithubRepository(remoteApi: self.remoteApi)
    var interactor: RepositoryInteractor!
    var interactorDelegate: FakeInteractorDelegate!
    
    class FakeInteractorDelegate: RepositoryInteractorDelegate {
        
        var state: RepositoryState = .idle
        var viewModel = RepositoryViewModel(sections: [])
        var didUpdateState: (() -> ())?
        var didLoadNewCell: (() -> ())?
        
        func interactor(_ interactor: RepositoryInteractor, didUpdate state: RepositoryState) {
            self.state = state
            didUpdateState?()
        }
        
        func interactor(_ interactor: RepositoryInteractor, didLoadNewCellsAt indexPaths: [IndexPath], viewModel: RepositoryViewModel) {
            self.viewModel = viewModel
            didLoadNewCell?()
        }
        
    }
    
    override func setUp() {
        // Put setup code here. This method is called before the invocation of each test method in the class.
        super.setUp()
        self.interactorDelegate = FakeInteractorDelegate()
        self.interactor = RepositoryInteractor(githubRepository: repository)
        self.interactor.delegate = interactorDelegate
    }

    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
        remoteApi.isFailed = false
    }

    func test_SearchRepositoryWithLogin() {
        interactor.searchRepository(query: "google",
                                    sort: .stars,
                                    order: .asc)
        let expectation = self.expectation(description: "Loading")
        interactorDelegate.didUpdateState = {
            if case .loading = self.interactorDelegate.state {
                expectation.fulfill()
            }
        }
        waitForExpectations(timeout: 2, handler: nil)
        let state = interactorDelegate.state
        if case .loading = state {
            XCTAssert(true)
        } else {
            XCTFail("the state \(state) should be loading")
        }

    }

    func test_SearchRepositoryWithSuccess() {
        interactor.searchRepository(query: "google",
                                    sort: .stars,
                                    order: .asc)
        let expectation = self.expectation(description: "Success")
        interactorDelegate.didUpdateState = {
            if case .success(_) = self.interactorDelegate.state {
                expectation.fulfill()
            }
        }
        waitForExpectations(timeout: 2, handler: nil)
        let state = interactorDelegate.state
        if case .success(_) = state {
            XCTAssert(true)
        } else {
            XCTFail("the state \(state) should be success")
        }
        
    }
    
    func test_SearchRepositoryWithError() {
        remoteApi.isFailed = true
        interactor.searchRepository(query: "google",
                                    sort: .stars,
                                    order: .asc)
        let expectation = self.expectation(description: "Failed")
        interactorDelegate.didUpdateState = {
            if case .failure(_) = self.interactorDelegate.state {
                expectation.fulfill()
            }
        }
        waitForExpectations(timeout: 2, handler: nil)
        let state = interactorDelegate.state
        if case .failure(_) = state {
            XCTAssert(true)
        } else {
            XCTFail("the state \(state) should be failed")
        }
        
    }

    func test_interactorSuccessViewModel() {
        let initialViewModel = interactorDelegate.viewModel
        interactor.searchRepository(query: "google",
                                    sort: .stars,
                                    order: .asc)
        let expectation = self.expectation(description: "Success")
        interactorDelegate.didUpdateState = {
            if case .success(_) = self.interactorDelegate.state {
                expectation.fulfill()
            }
        }
        waitForExpectations(timeout: 2, handler: nil)
        let state = interactorDelegate.state
        if case .success(let viewModel) = state {
            XCTAssertGreaterThan(viewModel.sections.count, initialViewModel.sections.count)
        } else {
            XCTFail("the state \(state) should be success")
        }
    }

}
