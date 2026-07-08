#!/usr/bin/env swift

import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

struct Pixel {
    let red: UInt8
    let green: UInt8
    let blue: UInt8
    let alpha: UInt8
}

func writePng(
    path: String,
    width: Int,
    height: Int,
    pixelAt: (Int, Int) -> Pixel
) throws {
    var bytes = [UInt8](repeating: 0, count: width * height * 4)
    for y in 0..<height {
        for x in 0..<width {
            let pixel = pixelAt(x, y)
            let offset = (y * width + x) * 4
            // premultipliedLast 要求半透明通道预乘；不透明夹具保持原始 RGB。
            let alphaScale = Double(pixel.alpha) / 255.0
            bytes[offset] = UInt8((Double(pixel.red) * alphaScale).rounded())
            bytes[offset + 1] = UInt8((Double(pixel.green) * alphaScale).rounded())
            bytes[offset + 2] = UInt8((Double(pixel.blue) * alphaScale).rounded())
            bytes[offset + 3] = pixel.alpha
        }
    }

    let data = Data(bytes)
    guard let provider = CGDataProvider(data: data as CFData) else {
        throw FixtureError.imageCreationFailed(path)
    }
    let bitmapInfo = CGBitmapInfo.byteOrder32Big.union(
        CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue)
    )
    guard let image = CGImage(
        width: width,
        height: height,
        bitsPerComponent: 8,
        bitsPerPixel: 32,
        bytesPerRow: width * 4,
        space: CGColorSpaceCreateDeviceRGB(),
        bitmapInfo: bitmapInfo,
        provider: provider,
        decode: nil,
        shouldInterpolate: false,
        intent: .defaultIntent
    ) else {
        throw FixtureError.imageCreationFailed(path)
    }

    let url = URL(fileURLWithPath: path)
    guard let destination = CGImageDestinationCreateWithURL(
        url as CFURL,
        UTType.png.identifier as CFString,
        1,
        nil
    ) else {
        throw FixtureError.imageCreationFailed(path)
    }
    CGImageDestinationAddImage(destination, image, nil)
    guard CGImageDestinationFinalize(destination) else {
        throw FixtureError.imageCreationFailed(path)
    }
}

enum FixtureError: Error {
    case imageCreationFailed(String)
    case missingOutputDirectory
}

guard CommandLine.arguments.count == 2 else {
    throw FixtureError.missingOutputDirectory
}

let outputDirectory = CommandLine.arguments[1]
try FileManager.default.createDirectory(
    atPath: outputDirectory,
    withIntermediateDirectories: true
)

let opaque: UInt8 = 255
let solidFixtures: [(String, UInt8)] = [
    ("01_black_256.png", 0),
    ("02_white_256.png", 255),
    ("03_dark_gray80_256.png", 80),
    ("04_bright_gray180_256.png", 180),
]
for (name, gray) in solidFixtures {
    try writePng(path: "\(outputDirectory)/\(name)", width: 256, height: 256) { _, _ in
        Pixel(red: gray, green: gray, blue: gray, alpha: opaque)
    }
}

try writePng(
    path: "\(outputDirectory)/05_low_contrast_112_144.png",
    width: 256,
    height: 256
) { x, _ in
    let gray = UInt8(112 + (x % 33))
    return Pixel(red: gray, green: gray, blue: gray, alpha: opaque)
}

try writePng(
    path: "\(outputDirectory)/06_normal_contrast_64_192.png",
    width: 256,
    height: 256
) { x, _ in
    let gray: UInt8 = x < 128 ? 64 : 192
    return Pixel(red: gray, green: gray, blue: gray, alpha: opaque)
}

try writePng(
    path: "\(outputDirectory)/07_rgb_primary_300x100.png",
    width: 300,
    height: 100
) { x, _ in
    if x < 100 { return Pixel(red: 255, green: 0, blue: 0, alpha: opaque) }
    if x < 200 { return Pixel(red: 0, green: 255, blue: 0, alpha: opaque) }
    return Pixel(red: 0, green: 0, blue: 255, alpha: opaque)
}

try writePng(
    path: "\(outputDirectory)/08_alpha_red_checker_256.png",
    width: 256,
    height: 256
) { x, y in
    let alpha: UInt8 = ((x / 32 + y / 32) % 2 == 0) ? 128 : 255
    return Pixel(red: 255, green: 0, blue: 0, alpha: alpha)
}

try writePng(
    path: "\(outputDirectory)/09_performance_gradient_3072x4096.png",
    width: 3072,
    height: 4096
) { x, y in
    let red = UInt8((x * 255) / 3071)
    let green = UInt8((y * 255) / 4095)
    let blue = UInt8(((x + y) * 255) / (3071 + 4095))
    return Pixel(red: red, green: green, blue: blue, alpha: opaque)
}

try writePng(
    path: "\(outputDirectory)/10_max_valid_4000x4000.png",
    width: 4000,
    height: 4000
) { _, _ in
    Pixel(red: 128, green: 128, blue: 128, alpha: opaque)
}

try writePng(
    path: "\(outputDirectory)/11_over_limit_4001x4000.png",
    width: 4001,
    height: 4000
) { _, _ in
    Pixel(red: 128, green: 128, blue: 128, alpha: opaque)
}
