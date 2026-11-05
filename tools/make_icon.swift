// Draws the Sunnote app icon (1024x1024 PNG): sunrise gradient with a pair of quote marks.
// Usage: swift tools/make_icon.swift <output.png>
import AppKit
import CoreGraphics

let size = 1024
let out = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "AppIcon.png"
let space = CGColorSpace(name: CGColorSpace.sRGB)!
let ctx = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0,
                    space: space, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!

func rgb(_ r: CGFloat, _ g: CGFloat, _ b: CGFloat) -> CGColor { CGColor(srgbRed: r, green: g, blue: b, alpha: 1) }

// Sunrise gradient, top-left to bottom-right (same colours as the widget's sunrise theme).
let gradient = CGGradient(colorsSpace: space,
                          colors: [rgb(1.00, 0.80, 0.67), rgb(0.98, 0.62, 0.62), rgb(0.76, 0.66, 0.91)] as CFArray,
                          locations: [0, 0.5, 1])!
ctx.drawLinearGradient(gradient, start: CGPoint(x: 0, y: CGFloat(size)), end: CGPoint(x: CGFloat(size), y: 0), options: [])

// A soft sun disc behind the quote marks.
ctx.setFillColor(CGColor(srgbRed: 1, green: 0.95, blue: 0.85, alpha: 0.55))
ctx.fillEllipse(in: CGRect(x: 212, y: 212, width: 600, height: 600))

// Two serif-style quote marks: a round dot with a curved tail, like the Android icon.
ctx.setFillColor(rgb(0.23, 0.12, 0.16))
func quoteMark(x: CGFloat) {
    let r: CGFloat = 92
    let cy: CGFloat = 400
    ctx.fillEllipse(in: CGRect(x: x - r, y: cy - r, width: 2 * r, height: 2 * r))
    let tail = CGMutablePath()  // a "6": tail rises from the dot's left side and leans right
    tail.move(to: CGPoint(x: x - r * 0.95, y: cy + 10))
    tail.addCurve(to: CGPoint(x: x + r * 0.2, y: cy + r * 2.6),
                  control1: CGPoint(x: x - r * 1.1, y: cy + r * 1.4),
                  control2: CGPoint(x: x - r * 0.6, y: cy + r * 2.3))
    tail.addLine(to: CGPoint(x: x + r * 0.35, y: cy + r * 2.25))
    tail.addCurve(to: CGPoint(x: x - r * 0.1, y: cy + 20),
                  control1: CGPoint(x: x - r * 0.15, y: cy + r * 1.9),
                  control2: CGPoint(x: x - r * 0.45, y: cy + r * 1.2))
    tail.closeSubpath()
    ctx.addPath(tail)
    ctx.fillPath()
}
quoteMark(x: 400)
quoteMark(x: 640)

let image = ctx.makeImage()!
let rep = NSBitmapImageRep(cgImage: image)
try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: out))
print("wrote \(out)")
