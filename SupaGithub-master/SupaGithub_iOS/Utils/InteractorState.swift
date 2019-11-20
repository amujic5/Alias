//
//  ViewState.swift
//  CoreKit
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation


public enum InteractorState<T> {
    case loading
    case idle
    case success(_ viewModel: T)
    case failure(_ error: Error)
}
