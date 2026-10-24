// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_shared",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_shared",
      type: .none,
      targets: ["_shared"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/googlemaps/ios-maps-sdk.git",
      exact: "11.2.0"
    )
  ],
  targets: [
    .target(
      name: "_shared",
      dependencies: [
        .product(
          name: "GoogleMaps",
          package: "ios-maps-sdk"
        )
      ]
    )
  ]
)
