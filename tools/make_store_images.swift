// Builds Sunote's store images from raw simulator/emulator screenshots.
// Usage: swift tools/make_store_images.swift <raw-screenshots-dir> <output-dir>
// Expects in the raw dir: ios-{home,checkin,today,settings,myquotes,favourites}.png,
// and-{home,checkin,today,settings,myquotes,favourites}.png, and the icon at
// ios/QuoteApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png (run from the repo root).
import AppKit
import CoreGraphics
import CoreText

let args = CommandLine.arguments
let rawDir = URL(fileURLWithPath: args.count > 1 ? args[1] : "raw")
let outDir = URL(fileURLWithPath: args.count > 2 ? args[2] : "docs/store")
let space = CGColorSpace(name: CGColorSpace.sRGB)!
let ink = CGColor(srgbRed: 0.23, green: 0.12, blue: 0.16, alpha: 1)

func load(_ name: String) -> CGImage {
    let url = rawDir.appendingPathComponent(name)
    guard let src = CGImageSourceCreateWithURL(url as CFURL, nil),
          let img = CGImageSourceCreateImageAtIndex(src, 0, nil) else { fatalError("cannot read \(url.path)") }
    return img
}

func canvas(_ w: Int, _ h: Int) -> CGContext {
    let ctx = CGContext(data: nil, width: w, height: h, bitsPerComponent: 8, bytesPerRow: 0, space: space,
                        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    let g = CGGradient(colorsSpace: space, colors: [
        CGColor(srgbRed: 1.00, green: 0.86, blue: 0.75, alpha: 1),
        CGColor(srgbRed: 0.99, green: 0.72, blue: 0.70, alpha: 1),
        CGColor(srgbRed: 0.84, green: 0.76, blue: 0.95, alpha: 1)] as CFArray, locations: [0, 0.55, 1])!
    ctx.drawLinearGradient(g, start: CGPoint(x: 0, y: CGFloat(h)), end: CGPoint(x: CGFloat(w), y: 0), options: [])
    return ctx
}

/// Centred caption; `top` is the distance from the top edge to the first line's top.
func caption(_ ctx: CGContext, _ text: String, width: Int, height: Int, top: CGFloat, size: CGFloat) {
    let font = CTFontCreateWithName("Georgia-Bold" as CFString, size, nil)
    let para = NSMutableParagraphStyle(); para.alignment = .center; para.lineSpacing = size * 0.15
    let attr = NSAttributedString(string: text, attributes: [
        .font: font, .foregroundColor: NSColor(cgColor: ink)!, .paragraphStyle: para])
    let setter = CTFramesetterCreateWithAttributedString(attr)
    let boxW = CGFloat(width) * 0.84
    let fit = CTFramesetterSuggestFrameSizeWithConstraints(setter, CFRange(), nil, CGSize(width: boxW, height: .greatestFiniteMagnitude), nil)
    let rect = CGRect(x: (CGFloat(width) - boxW) / 2, y: CGFloat(height) - top - fit.height, width: boxW, height: fit.height)
    let frame = CTFramesetterCreateFrame(setter, CFRange(), CGPath(rect: rect, transform: nil), nil)
    CTFrameDraw(frame, ctx)
}

/// Draws an image into rect with rounded corners and a soft shadow.
func place(_ ctx: CGContext, _ img: CGImage, in rect: CGRect, radius: CGFloat) {
    ctx.saveGState()
    ctx.setShadow(offset: CGSize(width: 0, height: -radius * 0.25), blur: radius * 0.9,
                  color: CGColor(srgbRed: 0.3, green: 0.1, blue: 0.2, alpha: 0.28))
    ctx.addPath(CGPath(roundedRect: rect, cornerWidth: radius, cornerHeight: radius, transform: nil))
    ctx.setFillColor(CGColor(gray: 1, alpha: 1)); ctx.fillPath()
    ctx.restoreGState()
    ctx.saveGState()
    ctx.addPath(CGPath(roundedRect: rect, cornerWidth: radius, cornerHeight: radius, transform: nil))
    ctx.clip(); ctx.draw(img, in: rect)
    ctx.restoreGState()
}

func save(_ ctx: CGContext, _ name: String) {
    let rep = NSBitmapImageRep(cgImage: ctx.makeImage()!)
    let url = outDir.appendingPathComponent(name)
    try! FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
    try! rep.representation(using: .png, properties: [:])!.write(to: url)
    print("wrote \(name) \(ctx.width)x\(ctx.height)")
}

/// Screenshot under a caption, scaled to fill the space below it.
func screen(_ raw: String, _ text: String, w: Int, h: Int, out: String) {
    let ctx = canvas(w, h)
    let capTop = CGFloat(h) * 0.055, capSize = CGFloat(w) * 0.07
    caption(ctx, text, width: w, height: h, top: capTop, size: capSize)
    let img = load(raw)
    let areaTop = CGFloat(h) * 0.20, bottomPad = CGFloat(h) * 0.04
    let availH = CGFloat(h) - areaTop - bottomPad
    let scale = min(availH / CGFloat(img.height), CGFloat(w) * 0.82 / CGFloat(img.width))
    let iw = CGFloat(img.width) * scale, ih = CGFloat(img.height) * scale
    place(ctx, img, in: CGRect(x: (CGFloat(w) - iw) / 2, y: bottomPad, width: iw, height: ih), radius: iw * 0.09)
    save(ctx, out)
}

// ---- iPhone 6.9-inch: 1320 x 2868 ----
let iw = 1320, ih = 2868
screen("ios-home.png", "A happy little quote, every day", w: iw, h: ih, out: "ios/1-home.png")
screen("ios-checkin.png", "Tell it how you feel today", w: iw, h: ih, out: "ios/2-check-in.png")
screen("ios-today.png", "A quote picked for your day", w: iw, h: ih, out: "ios/3-today.png")
screen("ios-settings.png", "A style for every wallpaper", w: iw, h: ih, out: "ios/4-styles.png")
screen("ios-myquotes.png", "Put your own words on today's widget", w: iw, h: ih, out: "ios/5-my-quotes.png")
screen("ios-favourites.png", "Keep the ones you love", w: iw, h: ih, out: "ios/6-favourites.png")

// ---- Google Play phone: 1080 x 1920 (Play allows at most 2:1) ----
let aw = 1080, ah = 1920
screen("and-home.png", "A happy little quote, every day", w: aw, h: ah, out: "android/1-home.png")
screen("and-checkin.png", "Tell it how you feel today", w: aw, h: ah, out: "android/2-check-in.png")
screen("and-today.png", "A quote picked for your day", w: aw, h: ah, out: "android/3-today.png")
screen("and-settings.png", "A style for every wallpaper", w: aw, h: ah, out: "android/4-styles.png")
screen("and-myquotes.png", "Put your own words on today's widget", w: aw, h: ah, out: "android/5-my-quotes.png")
screen("and-favourites.png", "Keep the ones you love", w: aw, h: ah, out: "android/6-favourites.png")

// ---- Google Play feature graphic 1024 x 500 and 512 px icon ----
let iconURL = URL(fileURLWithPath: "ios/QuoteApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png")
let iconSrc = CGImageSourceCreateWithURL(iconURL as CFURL, nil)!
let icon = CGImageSourceCreateImageAtIndex(iconSrc, 0, nil)!
do {
    let ctx = canvas(1024, 500)
    place(ctx, icon, in: CGRect(x: 90, y: 110, width: 280, height: 280), radius: 62)
    let title = CTLineCreateWithAttributedString(NSAttributedString(string: "Sunote", attributes: [
        .font: CTFontCreateWithName("Georgia-Bold" as CFString, 96, nil), .foregroundColor: NSColor(cgColor: ink)!]))
    ctx.textPosition = CGPoint(x: 430, y: 262); CTLineDraw(title, ctx)
    let tag = CTLineCreateWithAttributedString(NSAttributedString(string: "A happy little quote, every day", attributes: [
        .font: CTFontCreateWithName("Georgia" as CFString, 38, nil), .foregroundColor: NSColor(cgColor: ink)!]))
    ctx.textPosition = CGPoint(x: 432, y: 190); CTLineDraw(tag, ctx)
    save(ctx, "android/feature-graphic.png")
}
do {
    let ctx = CGContext(data: nil, width: 512, height: 512, bitsPerComponent: 8, bytesPerRow: 0, space: space,
                        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    ctx.interpolationQuality = .high
    ctx.draw(icon, in: CGRect(x: 0, y: 0, width: 512, height: 512))
    save(ctx, "android/icon-512.png")
}
