//
//  UIColor+Additions.swift
//  BeautyRoom
//
//  Generated on Zeplin. (5/19/2018).
//  Copyright (c) 2018 __MyCompanyName__. All rights reserved.
//

import UIKit

public extension UIColor {

    @nonobjc class var greyishBrown: UIColor {
        return UIColor(white: 74.0 / 255.0, alpha: 1.0)
    }

    @nonobjc class var success: UIColor {
        return UIColor(hex: 0x00c800)
    }

    @nonobjc class var borderTextField: UIColor {
        return UIColor(white: 241.0 / 255.0, alpha: 0.5)
    }

    @nonobjc class var primary: UIColor {
        return UIColor(hex: 0xC58662)
    }

    @nonobjc class var secondary: UIColor {
        return UIColor(hex: 0x667EEA)
    }

    @nonobjc class var separator: UIColor {
        return UIColor(red: 230/255, green: 230/255, blue: 230/255, alpha: 1)
    }

    @nonobjc class var backgroundColor: UIColor {
        return UIColor(white: 241 / 255, alpha: 1.0)
    }

    @nonobjc class var askipBlue: UIColor {
        return  UIColor(red: 0, green: 122/255, blue: 1, alpha: 1)
    }

    @nonobjc class var purple: UIColor {
        return  UIColor(hexColor: "933EC5")
    }
    @nonobjc class var blueLight: UIColor {
        return  UIColor(hexColor: "8FC4FF")
    }
    @nonobjc class var orangeNotif: UIColor {
        return  UIColor(hexColor: "FF9F68")
    }
    @nonobjc class var gray: UIColor {
        return  UIColor(hexColor: "8F949D")
    }
    
//    @nonobjc class var lightGray: UIColor {
//        return  UIColor(hexColor: "ececeb")
//    }

    @nonobjc class var borderShadow: UIColor {
        return  UIColor(hexColor: "455B63", alpha: 0.3)
    }
    
    @nonobjc class var yellowSnap: UIColor {
        return  UIColor(hexColor: "FFFA54")
    }
    @nonobjc class var buttonColor: UIColor {
        return  UIColor(hexColor: "007AFF")
    }
    @nonobjc class var redBackground: UIColor {
        return  UIColor(hexColor: "a70b21")
    }
    @nonobjc class var greyedBlue: UIColor {
        return  UIColor(hexColor: "#355C7D")
    }
    @nonobjc class var topGradientColors: [String] {
        return  ["#B4EC51", "#F5515F", "#FAD961", "#3023AE", "#FF0099", "#4ECDC4", "#0B486B"] // Has to contain as many elem as BOTTOM_GRADIENT_COLORS
    }
    @nonobjc class var bottomGradientColors: [String] {
    return  ["#429321", "#9F041B", "#F76B1C", "#C86DD7", "#493240", "#556270", "#F56217"]
    }

}

public extension UIColor {
    convenience init<T>(rgbValue: T, alpha: CGFloat = 1) where T: BinaryInteger {
        let blue = CGFloat(rgbValue & 0xff) / 255
        let green = CGFloat(rgbValue >> 8 & 0xff) / 255
        let red = CGFloat(rgbValue >> 16 & 0xff) / 255
        let alpha = CGFloat(rgbValue >> 24 & 0xff) / 255
        self.init(red: red, green: green, blue: blue, alpha: alpha)
    }

    var argb: Int? {
        var fRed: CGFloat = 0
        var fGreen: CGFloat = 0
        var fBlue: CGFloat = 0
        var fAlpha: CGFloat = 0
        if self.getRed(&fRed, green: &fGreen, blue: &fBlue, alpha: &fAlpha) {
            let iRed = Int(fRed * 255.0)
            let iGreen = Int(fGreen * 255.0)
            let iBlue = Int(fBlue * 255.0)
            let iAlpha = Int(fAlpha * 255.0)

            //  (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are blue).
            let rgb = (iAlpha << 24) + (iRed << 16) + (iGreen << 8) + iBlue
            return rgb
        } else {
            // Could not extract RGBA components:
            return nil
        }
    }

    @nonobjc class var random: UIColor {
        let red = CGFloat.random(in: 0 ..< 0.6)
        let green = CGFloat.random(in: 0 ..< 0.6)
        let blue = CGFloat.random(in: 0 ..< 0.6)
        return UIColor(red: red, green: green, blue: blue, alpha: 1)
    }
}

extension UIColor {
    convenience init(hex: UInt) {
        self.init(
            red: CGFloat((hex & 0xFF0000) >> 16) / 255.0,
            green: CGFloat((hex & 0x00FF00) >> 8) / 255.0,
            blue: CGFloat(hex & 0x0000FF) / 255.0,
            alpha: CGFloat(1.0)
        )
    }

    convenience init(hexColor: String, alpha: CGFloat = 1.0) {
        var red: UInt32 = 0, green: UInt32 = 0, blue: UInt32 = 0

        let hex = hexColor as NSString
        Scanner(string: hex.substring(with: NSRange(location: 0, length: 2))).scanHexInt32(&red)
        Scanner(string: hex.substring(with: NSRange(location: 2, length: 2))).scanHexInt32(&green)
        Scanner(string: hex.substring(with: NSRange(location: 4, length: 2))).scanHexInt32(&blue)

        self.init(red: CGFloat(red)/255.0, green: CGFloat(green)/255.0, blue: CGFloat(blue)/255.0, alpha: alpha)
    }

}
