//
//  Style.swift
//  SupaGithub_iOS
//
//  Created by Mohamed Ali on 10/06/2019.
//  Copyright © 2019 GeekDev. All rights reserved.
//

import Foundation

public enum Style {
    
    public enum ImageView {
        public static func smallCircled(_ imageView: UIImageView) {
            let size: CGFloat = 40
            imageView.al.height.set(size)
            imageView.al.width.set(size)
            imageView.setCorner(radius: size / 2)
            imageView.contentMode = .scaleAspectFill
        }

        public static func bigThumbnail(_ imageView: UIImageView) {
            let size: CGFloat = 100
            imageView.al.height.set(size)
            imageView.al.width.set(size)
            imageView.setCorner(radius: size / 2)
            imageView.contentMode = .scaleAspectFill
        }

    }
    
    public enum Button {
        
        public static func smallCircled(_ button: UIButton) {
            let size: CGFloat = 40
            button.al.height.set(size)
            button.al.width.set(size)
            button.setCorner(radius: size / 2)
        }
        
        public static func mediumCircled(_ button: UIButton) {
            let size: CGFloat = 80
            button.al.height.set(size)
            button.al.width.set(size)
            button.setCorner(radius: size / 2)
        }
        
        public static func primary(_ button: UIButton) {
            button.titleLabel?.font = .buttonStyle
            button.backgroundColor = .primary
            button.setTitleColor(.white, for: .normal)
            button.titleLabel?.numberOfLines = 0
            button.setCorner(radius: 8)
            switch UIDevice.current.userInterfaceIdiom {
            case .pad, .tv:
                button.al.height.set(80)
            default:
                button.al.height.set(48)
            }
        }
        
        public static func secondary(_ button: UIButton) {
            button.titleLabel?.font = .buttonStyle
            button.setTitleColor(.primary, for: .normal)
            button.setCorner(radius: 8)
            switch UIDevice.current.userInterfaceIdiom {
            case .pad, .tv:
                button.al.height.set(40)
            default:
                button.al.height.set(25)
            }
        }
    }
    
    public enum Label {
        public static func body(_ label: UILabel) {
            label.font = .subtitleStyle
            label.textAlignment = .left
            label.numberOfLines = 0
            label.lineBreakMode = .byWordWrapping
            label.textColor = .gray
            label.setContentCompressionResistancePriority(.required, for: .vertical)
        }
        
        public static func title(_ label: UILabel) {
            label.font = UIFont.titleStyle
            label.textAlignment = .left
            label.numberOfLines = 0
            label.textColor = .darkGray
            label.setContentCompressionResistancePriority(.required, for: .vertical)
        }
        
    }
    
    public enum TextField {
        public static func primary(_ textField: UITextField) {
            textField.al.height.set(50)
            textField.setCorner(radius: 5)
            textField.borderStyle = .roundedRect
            textField.layer.borderColor = UIColor.white.cgColor
            textField.layer.borderWidth = 3
        }
    }
}
