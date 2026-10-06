# Changelog

## [1.0.0](https://github.com/verygoodsecurity/starlarky/compare/v0.17.1...v1.0.0) (2026-10-06)


### ⚠ BREAKING CHANGES

* Load native modules only from their own namespace ([#732](https://github.com/verygoodsecurity/starlarky/issues/732))
* Limit ints to 65,536 bits ([#719](https://github.com/verygoodsecurity/starlarky/issues/719))
* Fix bytes and bytearray semantics ([#712](https://github.com/verygoodsecurity/starlarky/issues/712))
* the libstarlark and larky jars are built for Java 21 (class file version 65); consumers need Java 21 or later.

### Features

* Accept type annotations in Larky scripts ([#724](https://github.com/verygoodsecurity/starlarky/issues/724)) ([0fd5a57](https://github.com/verygoodsecurity/starlarky/commit/0fd5a5710cbfa3ed7352d29f525817e613fafc0c))
* Add Python's format(value, format_spec) built-in to Larky ([#723](https://github.com/verygoodsecurity/starlarky/issues/723)) ([84ea310](https://github.com/verygoodsecurity/starlarky/commit/84ea3103ecc4ac4a5d343b5281802d87e1f02d18))
* Format str % args as Python does in Larky ([#722](https://github.com/verygoodsecurity/starlarky/issues/722)) ([ee3ef57](https://github.com/verygoodsecurity/starlarky/commit/ee3ef57ed0d17b9391e674bb8576103aae318ee0))
* Limit ints to 65,536 bits ([#719](https://github.com/verygoodsecurity/starlarky/issues/719)) ([1c2fdcc](https://github.com/verygoodsecurity/starlarky/commit/1c2fdcc8c06d8087efc4262dd079211fa89efb6e))
* Load a script's named files as separate modules, cached per namespace, with errors at the script's own line ([#731](https://github.com/verygoodsecurity/starlarky/issues/731)) ([16bc351](https://github.com/verygoodsecurity/starlarky/commit/16bc351c9a739bb746f8a766d3270e93ac1d06d7))
* Update libstarlark to Bazel bd258719f and require Java 21 ([#704](https://github.com/verygoodsecurity/starlarky/issues/704)) ([28a3988](https://github.com/verygoodsecurity/starlarky/commit/28a39885e0ab870591aca73e5d63934b5112c197))


### Bug Fixes

* Fix bytes and bytearray semantics ([#712](https://github.com/verygoodsecurity/starlarky/issues/712)) ([7565726](https://github.com/verygoodsecurity/starlarky/commit/7565726afdc7754ab813c1fd6293fdd06f44f2c8))
* Fix bytes repr and codecs; test Padding.unpad with pycryptodome's inputs ([#718](https://github.com/verygoodsecurity/starlarky/issues/718)) ([5aae553](https://github.com/verygoodsecurity/starlarky/commit/5aae553f06460f4e344060759165b6d8698ae714))
* Fix OpenPGP MPI encoding and base32 ([#711](https://github.com/verygoodsecurity/starlarky/issues/711)) ([e75296d](https://github.com/verygoodsecurity/starlarky/commit/e75296defeda19e3b9e0d12d8a602f7bac5cf6d0))
* Give bytes and bytearray Starlark type names via @StarlarkBuiltin ([#707](https://github.com/verygoodsecurity/starlarky/issues/707)) ([df2fa24](https://github.com/verygoodsecurity/starlarky/commit/df2fa244762b5e85f024179321c43624095cf52b))
* Isolate scripts: immutable built-in types, per-evaluation XML namespaces, safe() fixes ([#710](https://github.com/verygoodsecurity/starlarky/issues/710)) ([63228e6](https://github.com/verygoodsecurity/starlarky/commit/63228e629fdcfd6f9f63b3a361e8241614d612d9))
* Load native modules only from their own namespace ([#732](https://github.com/verygoodsecurity/starlarky/issues/732)) ([b0d93f7](https://github.com/verygoodsecurity/starlarky/commit/b0d93f7c980b09f317486545a823454add45265b))
* Match Python in str search bounds and Unicode case mapping ([#714](https://github.com/verygoodsecurity/starlarky/issues/714)) ([0614acb](https://github.com/verygoodsecurity/starlarky/commit/0614acbef8cf1aac33e043f74427788adbd1910e))


### Performance Improvements

* Cache compiled programs and loaded modules ([#729](https://github.com/verygoodsecurity/starlarky/issues/729)) ([50ea9b4](https://github.com/verygoodsecurity/starlarky/commit/50ea9b49bbc6528ab1f19c0f23015aec140659a5))
* Read the clock on every 64th expiration check, and make the interval configurable ([#721](https://github.com/verygoodsecurity/starlarky/issues/721)) ([7151d82](https://github.com/verygoodsecurity/starlarky/commit/7151d82d2aea10751429ff19c38bdf70e0992099))
* Remove per-request work from Larky evaluations ([#720](https://github.com/verygoodsecurity/starlarky/issues/720)) ([47ddbff](https://github.com/verygoodsecurity/starlarky/commit/47ddbffbae6b455d4267396dc7f62b659e38a712))


### Code Refactoring

* Move the expiration check into StarlarkThread.checkExpired ([#708](https://github.com/verygoodsecurity/starlarky/issues/708)) ([a2c0d74](https://github.com/verygoodsecurity/starlarky/commit/a2c0d74e860db92b73a41eaf7c21e733bb723385))


### Build System

* Move VGS libstarlark sources to src/main/vgs and run .star tests under Maven ([#709](https://github.com/verygoodsecurity/starlarky/issues/709)) ([3dece7e](https://github.com/verygoodsecurity/starlarky/commit/3dece7e0dd8c87965a68680fd53192b9b3ff8f2d))
* Replace Poetry with uv for pylarky ([#759](https://github.com/verygoodsecurity/starlarky/issues/759)) ([561c00e](https://github.com/verygoodsecurity/starlarky/commit/561c00e412a2f464e1de2ff956be227f3eb69920))

## [0.17.1](https://github.com/verygoodsecurity/starlarky/compare/v0.17.0...v0.17.1) (2026-10-02)


### Miscellaneous Chores

* [LARKY-1] align renovate config with card-on-file and calm ([#739](https://github.com/verygoodsecurity/starlarky/issues/739)) ([8fe7176](https://github.com/verygoodsecurity/starlarky/commit/8fe71761c1e5950f169e9f0cbfa5fadd856e0ee7))


### Continuous Integration

* Publish libstarlark and larky to GitHub Packages on release tags ([#741](https://github.com/verygoodsecurity/starlarky/issues/741)) ([ad74bc0](https://github.com/verygoodsecurity/starlarky/commit/ad74bc07b806bc2cda28b2145ec6191a8a88d251))

## [0.17.0](https://github.com/verygoodsecurity/starlarky/compare/v0.16.0...v0.17.0) (2026-10-02)


### Features

* add @vgs//proxy with ShortCircuitResponse ([#705](https://github.com/verygoodsecurity/starlarky/issues/705)) ([d61aa36](https://github.com/verygoodsecurity/starlarky/commit/d61aa36e9f72fecd14e99ba7c8a84363b1beaa1e))


### Continuous Integration

* add semgrep SAST workflow ([fe5260d](https://github.com/verygoodsecurity/starlarky/commit/fe5260d58512b434d2fab30a335207711a78ccdb))

## [0.16.0](https://github.com/verygoodsecurity/starlarky/compare/v0.15.2...v0.16.0) (2026-02-03)


### Features

* **compatibility:** Expose the Python `__LENGTH_HINT__` magic method in `PyProtocols`. This is used to help identify how many items a particular iterator will possess. ([ad6430d](https://github.com/verygoodsecurity/starlarky/commit/ad6430d5c7632a4a53eb1640f08b80225b70564f))
* **compat:** mimic python's builtins.callable + builtins.reversed + builtins.NotImplemented for easier porting. ([5a21a5a](https://github.com/verygoodsecurity/starlarky/commit/5a21a5a0f2a776add19ad56d5e22ea6df270d865))


### Bug Fixes

* add apt-get update for dist-linux step ([#23](https://github.com/verygoodsecurity/starlarky/issues/23)) ([1303b01](https://github.com/verygoodsecurity/starlarky/commit/1303b0195f5385f1eddf958e79b928a0170f4bcf))
* **bug:** When using a BufferedBlockCipher, check if there's any further buffer left over before calling doFinal. ([#311](https://github.com/verygoodsecurity/starlarky/issues/311)) ([d419b6b](https://github.com/verygoodsecurity/starlarky/commit/d419b6b3c2b6c5dcfba9001b485ef93f9b352ae8))
* Dockerfile to reduce vulnerabilities ([#247](https://github.com/verygoodsecurity/starlarky/issues/247)) ([e66bc94](https://github.com/verygoodsecurity/starlarky/commit/e66bc9430206b52b9ba9f83682dbe218ca5c8a65))
* Dockerfile to reduce vulnerabilities ([#376](https://github.com/verygoodsecurity/starlarky/issues/376)) ([4d3625f](https://github.com/verygoodsecurity/starlarky/commit/4d3625febc00d7d85bc825b1121a0a69558e0a70))
* larky/pom.xml to reduce vulnerabilities ([#318](https://github.com/verygoodsecurity/starlarky/issues/318)) ([1606337](https://github.com/verygoodsecurity/starlarky/commit/1606337220fec3b654dbd91e26efd0d0d0bc2103))
* runlarky/pom.xml to reduce vulnerabilities ([#333](https://github.com/verygoodsecurity/starlarky/issues/333)) ([3e41b48](https://github.com/verygoodsecurity/starlarky/commit/3e41b48afeba7ef0f874128fd962525bcda0ead7))
* **SD-4274:** gha migration ([#696](https://github.com/verygoodsecurity/starlarky/issues/696)) ([2b542ad](https://github.com/verygoodsecurity/starlarky/commit/2b542ad7c11d1b29d424558384557974f35992e9))
* upgrade com.google.auto.value:auto-value-annotations from 1.8.2 to 1.9 ([#236](https://github.com/verygoodsecurity/starlarky/issues/236)) ([4df5084](https://github.com/verygoodsecurity/starlarky/commit/4df50846b50daa15c4dfd711833999f53eec0475))
* upgrade com.google.crypto.tink:tink from 1.5.0 to 1.6.1 ([#174](https://github.com/verygoodsecurity/starlarky/issues/174)) ([cbe6ef8](https://github.com/verygoodsecurity/starlarky/commit/cbe6ef814d342722616678b342d92af85af4c805))
* upgrade com.google.flogger:flogger-system-backend from 0.7.1 to 0.7.2 ([#233](https://github.com/verygoodsecurity/starlarky/issues/233)) ([b3c5e96](https://github.com/verygoodsecurity/starlarky/commit/b3c5e96962a5b144f22fdd37383c15b3909784a1))
* upgrade com.google.guava:guava from 30.1-jre to 30.1.1-jre ([#98](https://github.com/verygoodsecurity/starlarky/issues/98)) ([e355aae](https://github.com/verygoodsecurity/starlarky/commit/e355aae939a1019bf4fa26ff8ec3b88dd458c948))
* upgrade com.google.re2j:re2j from 1.5 to 1.6 ([#94](https://github.com/verygoodsecurity/starlarky/issues/94)) ([9206e2b](https://github.com/verygoodsecurity/starlarky/commit/9206e2b32a91684cf25f889743dba621c5441405))
* upgrade junit:junit from 4.13.1 to 4.13.2 ([#85](https://github.com/verygoodsecurity/starlarky/issues/85)) ([3f0fa6b](https://github.com/verygoodsecurity/starlarky/commit/3f0fa6b4fc625f448735c2c13cf2bf898ad65f21))
* upgrade org.bouncycastle:bcprov-debug-jdk15to18 from 1.69 to 1.70 ([#229](https://github.com/verygoodsecurity/starlarky/issues/229)) ([8c77aeb](https://github.com/verygoodsecurity/starlarky/commit/8c77aebc869a80bfbdc61fecb8d7d0efe69bca99))
* upgrade org.projectlombok:lombok from 1.18.20 to 1.18.22 ([#193](https://github.com/verygoodsecurity/starlarky/issues/193)) ([0f01e5f](https://github.com/verygoodsecurity/starlarky/commit/0f01e5f1fbd51f3522158826366afab61a39562f))


### Miscellaneous Chores

* `PoorManGenerator` move to `larky` and rename to `DeterministicGenerator` ([ad6430d](https://github.com/verygoodsecurity/starlarky/commit/ad6430d5c7632a4a53eb1640f08b80225b70564f))
