// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "QuoteCore",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "QuoteCore", targets: ["QuoteCore"])
    ],
    targets: [
        .target(name: "QuoteCore"),
        .testTarget(name: "QuoteCoreTests", dependencies: ["QuoteCore"])
    ]
)
