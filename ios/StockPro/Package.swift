// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "StockPro",
    defaultLocalization: "fr",
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .library(
            name: "StockPro",
            type: .static,
            targets: ["StockPro"]
        ),
    ],
    dependencies: [],
    targets: [
        .target(
            name: "StockPro",
            dependencies: [],
            path: ".",
            resources: [
                .process("Resources")
            ]
        ),
        .testTarget(
            name: "StockProTests",
            dependencies: ["StockPro"]
        ),
    ]
)
