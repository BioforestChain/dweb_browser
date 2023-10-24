### rust target add

```shell
rustup target add aarch64-apple-ios x86_64-apple-ios aarch64-apple-ios-sim
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
```

### Android

```shell
cargo install cbindgen
cargo install cargo-ndk
cargo ndk -t aarch64-linux-android -o ../src/androidMain/jniLibs build --release
cargo ndk -t armv7-linux-androideabi -o ../src/androidMain/jniLibs build --release
cargo ndk -t i686-linux-android -o ../src/androidMain/jniLibs build --release
cargo ndk -t x86_64-linux-android -o ../src/androidMain/jniLibs build --release
```

### iOS

```shell
cargo build --release --target aarch64-apple-ios
cargo build --release --target x86_64-apple-ios
cargo build --release --target aarch64-apple-ios-sim
```
