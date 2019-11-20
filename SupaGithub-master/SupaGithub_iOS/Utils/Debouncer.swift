//
//  Debouncer.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 12/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation

/// Ignores work items which are followed by another work item within a specified relative time duration.
final class Debouncer {
    
    typealias DebouncerClosure = (() -> Void)
    
    /// The scheduled work item.
    /// - Note: Can be cancelled at any time by calling `cancel()`
    private var workItem: DispatchWorkItem?
    
    /// Ignores work items which are followed by another work item within a specified relative time duration.
    ///
    /// - Parameters:
    ///   - delay: Throttling duration for each work item.
    ///   - queue: The `DispatchQueue` where the work will be dispatched.
    ///   - work: The work item to execute.
    func debounce(delay: TimeInterval, queue: DispatchQueue = .main, work: @escaping DebouncerClosure) {
        self.workItem?.cancel()
        self.workItem = DispatchWorkItem(block: work)
        queue.asyncAfter(deadline: .now() + delay, execute: workItem!)
    }
    
    /// Force the queued work to be perform and remove it from the dispatch queue.
    func performNow() {
        // Cancel the work item in order to remove it from the dispatch queue.
        self.workItem?.perform()
        self.workItem?.cancel()
    }
    
    /// Cancels the current queued work item.
    func cancel() {
        self.workItem?.cancel()
    }
    
}
