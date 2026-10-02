# Changelog

## [0.17.2](https://github.com/verygoodsecurity/starlarky/compare/v0.17.1...v0.17.2) (2026-10-02)


### Build System

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
