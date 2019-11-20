//
//  UIIMageView+Extension.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 12/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation
import Kingfisher

extension UIImageView {

    func loadImage(with url: URL, placeholder: UIImage? = nil, completion: ((_ success: Bool, _ image: UIImage?) -> Void)? = nil) {
        // Add indicator
        self.kf.indicatorType = .activity
        
        // Decode image in the background
        var options: KingfisherOptionsInfo = [
            //.cacheSerializer(FormatIndicatedCacheSerializer.jpeg),
            .backgroundDecode
        ]
        
        // Use DownsamplingImageProcessor if possible
        // Options with Downsampling https://developer.apple.com/videos/play/wwdc2018/219/
        if self.frame.size.width != 0 && self.frame.size.height != 0 {
            options.append(contentsOf: [
                .processor(DownsamplingImageProcessor(size: self.frame.size)),
                .scaleFactor(UIScreen.main.scale),
                .cacheOriginalImage
                ])
        }
        
        // Set image using Kingfisher
        self.kf.setImage(with: url, placeholder: placeholder, options: options, progressBlock: nil) { (result) in
            switch result {
            case .success(let value):
                // print("Image: \(value.image). Got from: \(value.cacheType) with url: \(resource.downloadURL.absoluteString)")
                completion?(true, value.image)
            case .failure(_):
                // print("Error: \(error) with description: \(error.localizedDescription)")
                completion?(false, nil)
            }
        }
        
    }
}
