cargo install cross --git https://github.com/cross-rs/cross

git clone https://github.com/cross-rs/cross
cd cross
git submodule update --init --remote
cargo xtask configure-crosstool
cargo build-docker-image aarch64-apple-darwin-cross --build-arg 'MACOS_SDK_URL=https://github.com/joseluisq/macosx-sdks/releases/download/12.3/MacOSX12.3.sdk.tar.xz' --tag local
cargo build-docker-image x86_64-apple-darwin-cross --build-arg 'MACOS_SDK_URL=https://github.com/joseluisq/macosx-sdks/releases/download/12.3/MacOSX12.3.sdk.tar.xz' --tag local
cargo build-docker-image aarch64-pc-windows-msvc-cross --tag local
cd ..

echo "Linux ARM"
cross build --release --target aarch64-unknown-linux-gnu
echo "Linux x86"
cross build --release --target x86_64-unknown-linux-gnu

echo "MacOS ARM"
cross build --release --target aarch64-apple-darwin
echo "MacOS x86"
cross build --release --target x86_64-apple-darwin

echo "Windows ARM"
cross build --release --target aarch64-pc-windows-msvc
echo "Windows x86"
cross build --release --target x86_64-pc-windows-gnu

rm -rf cross