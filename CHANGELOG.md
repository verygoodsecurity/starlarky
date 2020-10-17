## Release 3.6.0 (2020-10-06)

```
Baseline: aa0d97c0bfc4c09ec6f45303aa80052ba28afbd9

Cherry picks:

   + 32c88da98f301333dc447b75564459165368d418:
     Patch RuleContext for android_binary.deps to restore legacy
     behavior.
   + db9fc88fed387f09067a9250a731f8bf9ad74b05:
     android_test also needs the legacy behavior in
     RuleContext.getPrerequisites.
   + 144d5149a0c50e464dd1be0769fed2ce33ab26a4:
     Update android_sdk_repository to create a valid, but useless,
     repository
   + bb11f9235da52eb3b3e462ce0286f1a89188cb89:
     Patch upb to fix build error with gcc 10
   + 9f06be482aea3fcadeaf8fca6e48b32f224eba2e:
     Patch upb to fix build error with gcc 10 (third_party)
   + b67b75e3a62f5433d812993f3702f431b6967e86:
     Fix issue where libtool_check_unique isn't found for sandbox
     builds
```

Incompatible changes:

  - `--experimental_ui_limit_console_output` is removed. Users of
    `--experimental_ui_limit_console_output=1` for silencing terminal
    output should use `--ui_event_filters=` instead.
  - --proto:instantiation_stack must be enabled in addition to
    --record_rule_instantiation_callstack to see call stack in proto
    output from blaze query.

New features:

  - cc_common.compile support for include_prefix/strip_include_prefix
  - Multiplexed persistent workers: Use
    --experimental_worker_max_multiplex_instances to configure the
    number of WorkRequests that are sent concurrently to one worker
    process. The --worker_max_instances flag will no longer be used
    to determine max instances for multiplex workers, since the two
    have different resource requirements. Multiplex workers will by
    default have a max instances of 8.

Important changes:

  - The prelude file (//tools/build_rules:prelude_bazel) is now
    processed as a Starlark module, rather than being sourced into
    the BUILD file textually. This may cause slight breakages
    depending on the content of the prelude file. (Use of the prelude
    file is discouraged as it will be removed in the long term.)
  - Removed --experimental_ignore_deprecated_instrumentation_spec and
    cleaned up the old deprecated behavior.
  - Added CODEBASE.md, a description of the Bazel codebase.
  - Removed the flag --experimental_transparent_compression.
  - Removed the flag --experimental_action_args.
  - Stop needlessly parsing WORKSPACE files from external
    repositories.
  - Dot ('.') is now allowed in workspace names. See
    https://github.com/bazelbuild/bazel/issues/11837.
  - This change can cause memory and performance regressions for some
    builds with C++ dependencies, due to extra actions being executed.
    RELNOTES: None
  - Building Android apps for legacy multi-dex (pre-L) now require a
    main-dex list if the application does not fit into a single DEX
    file.
  - Puts the experimental_worker_multiplex flag to use.
  - In Starlark, the Args object supports a new parameter file format
    'flag_per_line', compatible with the Abseil flags library.
  - The flag --incompatible_no_support_tools_in_action_inputs is
    removed.
  - Support for NDK 21 added
  - Bazel will now skip printing action stdout/stderr contents if
    they exceed --experimental_ui_max_stdouterr_memory_bytes.
  - The Starlark interpreter now correctly emits an error
     if the operand of the first loop in a list comprehension
     refers to a variable bound by a later loop, such as y in
     this example:
       [e1 for x in f(y) in e2 for y in e3] # error: undefined y
                      ^
     This may cause latent dynamic errors to become static errors.
  - Added support for a 'supports-graceful-termination' execution
    requirement and tag, which causes Bazel to send a SIGTERM to any
    tagged
    actions before sending a delayed SIGKILL. This is to give
    actions, and more
    specifically tests, a chance to clean up after themselves.
  - Non-android targets can again be built when
    android_sdk_repository is present but invalid.

This release contains contributions from many people at Google, as well as Benjamin Peterson, Daniel Wagner-Hall, Dave MacLachlan, David Ostrovsky, Emil Kattainen, George Gensure, Greg Estren, Keith Smiley, mai12, Mai Hussien, Michael Eisel, Per Halvor Tryggeseth, Ruixin Bao, Samuel Giddins, Steeve Morin, Thi Doan, Tom de Goede, Ulf Adams, Zhongpeng Lin.

## Release 3.5.0 (2020-09-02)

```
Baseline: 889bc0b523b47eeb38a72bf9bb6858ee525a7c7e

Cherry picks:

   + a7a0d48fbeb059ee60e77580e5d05baeefdd5699:
     Make no-op starlark transition not affect the output directory.
   + b37c51c7085f0aefe04034dd451acb847605ddb5:
     Add include_prefix and strip_include_prefix to cc_common.compile
   + f6ad35fcde93f92c591778ed7db38d167f5bbc03:
     Delete --experimental_transparent_compression
   + 39bc97eab295bddb35b38bfc4a2ff3d2b15d034e:
     Remove --experimental_action_args
   + b9706675a7abf6ceebb250f0b3dfa4087a0c35f6:
     Stop needlessly parsing WORKSPACE files from external
     repositories.
   + e574d558da17cfd0f818e7a937a07926aa270069:
     Allow hyphen char in workspace name
   + 9993785fa0c4fa4172aa31d306f3abea76833abf:
     Allow dot ('.') in workspace names.
```

New features:

  - cc_common.compile support for include_prefix/strip_include_prefix

Important changes:

  - Removed the flag --experimental_transparent_compression.
  - Removed the flag --experimental_action_args.
  - Stop needlessly parsing WORKSPACE files from external
    repositories.
  - Dot ('.') is now allowed in workspace names. See
    https://github.com/bazelbuild/bazel/issues/11837.

This release contains contributions from many people at Google, as well as David Ostrovsky.

## Release 3.5.0 (2020-09-02)

```
Baseline: 889bc0b523b47eeb38a72bf9bb6858ee525a7c7e

Cherry picks:

   + d6b9469efebd200a39d7fd43876a18822fcdbe7b:
     Make no-op starlark transition not affect the output directory.
   + b37c51c7085f0aefe04034dd451acb847605ddb5:
     Add include_prefix and strip_include_prefix to cc_common.compile
   + 0ebb1d5a5388109e3f026a355c77fdf0121f3a43:
     Delete --experimental_transparent_compression
   + 312e121c70aebfaa91b0a3106fa964e0bc12d1df:
     Remove --experimental_action_args
   + 7e6e855bb82734f582e03c2c7fad3148c139d0e0:
     Stop needlessly parsing WORKSPACE files from external
     repositories.
   + d4049f6f85efb8f48d1f6b72764115af5b184831:
     Allow hyphen char in workspace name
   + 0a35be1843a2e4d49d5e5c3893cd6673705b7fb1:
     Allow dot ('.') in workspace names.
```

Incompatible changes:

  - The --experimental_process_wrapper_wait_fix flag (used
    purely to roll out a risky bug fix) has been removed.
  - Removed the --experimental_ui_deduplicate flag.
  - Bazel now correctly prefers Xcode versions in `/Applications`
    over any other paths, which resolves an issue with accidentally
    picking up an Xcode version from a Time Machine backup or network
    disk. In the improbable case that you relied on the old behavior
    and Bazel now picks up Xcode from the wrong location, you can fix
    it by moving that Xcode version to /Applications.

New features:

  - cquery now follows aspects with --include_aspects.
  - cc_common.compile support for include_prefix/strip_include_prefix

Important changes:

  - Add support to bazel/crosstool for building arm64 on macos aka
    darwin
  - Add opt in 'oso_prefix_is_pwd' feature for Apple builds
  - Add InstrumentedFilesInfo provider to Starlark globals.
  - Fixed resource shrinking when <overlayable/> tags are used.
  - Remove old incompatible flag
    --incompatible_symlinked_sandbox_expands_tree_artifacts_in_runfile
    s_tree.
  - Update coverage configuration for Python, filegroup, and shell
    script rules to distinguish between source and dependency
    attributes.
  - Add support to bazel/crosstool for building arm64e on macos aka
    darwin
  - Make filegroup always forward InstrumentedFilesProvider and not
    collect any sources directly.
  - Support signing key rotation in android_binary
  - Remove legacy handling of --extra_checks
  - Support signing key rotation in android_binary
    GO...
  - `--apple_bitcode` now takes an optional platform and only applies
    the Bitcode mode to that platform if present. The option may be
    provided multiple times.
  - Support signing key rotation in android_binary
  - NS_BLOCK_ASSERTIONS is now passed for all Apple architectures.
  - Major changes to reporting of Starlark errors and the call stack.
    (Please be alert to possible regressions, such as errors that
    lack relevant location information.)
  - Removed the flag --experimental_transparent_compression.
  - Removed the flag --experimental_action_args.
  - Stop needlessly parsing WORKSPACE files from external
    repositories.
  - Dot ('.') is now allowed in workspace names. See
    https://github.com/bazelbuild/bazel/issues/11837.

This release contains contributions from many people at Google, as well as Adam Gross, Andrew Suffield, Benjamin Peterson, bnczk, David Ostrovsky, Ed Schouten, Greg Estren, Grzegorz Lukasik, Holger Freyther, Kalle Johansson, Keith Smiley, Kerrick Staley, Kyle Teske, Mostyn Bramley-Moore, Ryan Beasley, Ryan Pavlik, Siggi Simonarson, Stiopa Koltsov, Ulf Adams, Xiaoyi Shi, Yannic Bonenberger, Yesudeep Mangalapilly.

## Release 3.4.1 (2020-07-14)

```
Baseline: 7404d17ac76da876ae0b432d1fccf222a9e991fe

Cherry picks:

   + f31f2d787116120b2b16e9aa9a64fab171c0d954:
     fixup! Gracefully handle the lack of subreaper support in Linux.
   + 3a4f221e3c57495c1ed0d1ec8128f92323b13079:
     Revert "Replace the remaining dependencies for Bazel Debian
     build (third_party)"
   + c55ec0f2cb3f5b44e5025bf9d3c5dc91d94db287:
     Revert "Upgrade gRPC to 1.26.0"
```

This release contains contributions from many people at Google, as well as Ryan Beasley.

## Release 3.4.0 (2020-07-13)

```
Baseline: 7404d17ac76da876ae0b432d1fccf222a9e991fe

Cherry picks:

   + a4334be50a206bf8d676a0196af11056c48ac35b:
     fixup! Gracefully handle the lack of subreaper support in Linux.
```

Incompatible changes:

  - This removes the short-lived --process_wrapper_extra_flags
    flag, which was introduced primarily to roll out a bug fix.
    Unfortunately,
    this made us inadvertently expose all of the process-wrapper's
    command line
    interface to the public, which should not have happened.  Given
    the corner
    case of the utility of this flag, the lack of documentation for
    it, and the
    fact that it only appeared in a single release, we are treating
    this as a
    bug instead of a backwards compatibility breakage.

New features:

  - bazel info: Allow to specify multiple keys.
  - Support code coverage with GCC 9.

Important changes:

  - Allow InstrumentedFilesInfo fields to be read from Starlark.
  - The --starlark_cpu_profile=<file> flag writes a profile in
    pprof format containing a statistical summary of CPU usage
    by all Starlark execution during the bazel command. Use it
    to identify slow Starlark functions in loading and analysis.
  - The --debug_depset_flag has been removed as it is in effect
    always on at no cost.
  - Rule authors should use the
    incompatible_use_toolchain_transition rule attribute to migrate
    to using
    the toolchain transition. jcater to udpate notes further.
  - `apple_binary` rules now accept the `stamp` attribute with the
    same
    semantics that it has in `cc_binary` rules.
  - --incompatible_objc_provider_remove_compile_info turns off
    the compile info/mege_zip Starlark APIs in ObjcProvider.  See
    #11359.
  - The --debug_depset_flag has been removed as it is in effect
    always on at no cost.
  - Fix behavior of ctx.actions.write so content is written without
    an incorrect encoding to UTF-8.
    See https://github.com/bazelbuild/bazel/issues/10174 for details.
  - Collect more performance metrics for worker execution.
  - Add flag --incompatible_force_strict_header_check_from_starlark
  - Configure coverage and runfiles for sh_library.
  - Adds --incompatible_blacklisted_protos_requires_proto_info to
    indicate whether proto_lang_toolchain.blacklisted_protos requires
    ProtoInfo.

This release contains contributions from many people at Google, as well as Andrzej Guszak, Benjamin Peterson, Benjamin Romano, Carlos Eduardo Seo, Claudio Bley, dannysullivan, David Ostrovsky, George Gensure, Graham Jenson, Grzegorz Lukasik, Gunnar Wagenknecht, Henk van der Laan, Jin, John Millikin, Marin Baron, Nikhil Marathe, Robin Nabel, Ryan Beasley, Samuel Giddins, Sergey Balabanov, utsav-dbx, Vo Van Nghia, Yannic Bonenberger.

## Release 3.3.1 (2020-06-30)

```
Baseline: c063b5caf776dee665497b64c5c17d4ed7e6750a

Cherry picks:

   + cb798a475eb54087e1e83f8aa1dc1c54550877b5:
     Restore missing Building with Platforms docs.
   + 9be97678b02bbd45d164c8458c8fd4f7791cb7aa:
     Release 3.3.0 (2020-06-17)
   + 3b0439e37247a480e08337a6314d06231bdbafd3:
     Fix incorrect assumption of desugar persistent worker conditional
```

This release contains contributions from many people at Google, as well as .

## Release 3.3.0 (2020-06-17)

```
Baseline: c063b5caf776dee665497b64c5c17d4ed7e6750a

Cherry picks:

   + 23bd69d7499de097b15e6025cc0796bdbc9886b8:
     Restore missing Building with Platforms docs.
```

Incompatible changes:

  - The startup option --fatal_event_bus_exceptions is now a no-op
    and will be removed soon.

New features:

  - Bazel offers basic completion for the fish shell.

Important changes:

  - Add configuration_field for --custom_malloc to cpp config fragment
  - Flip --incompatible_objc_compile_info_migration to true.  See
    #10854.
  - It is now possible to use different action mnemonics while still
    sharing the same pool of persistent workers. It requires setting
    a new property
    on the execution requirements (`worker-key-mnemonic`). The value
    overrides
    the action's mnemonic when it comes to reusing worker processes.
  - linkshared=1 in cc_binary no longer requires '.so' or '.dll' in
    the target name
  - Revert --incompatible_objc_compile_info_migration to false.  See
    #10854.
  - Invoke clang with the correct -target flag when building for
    watchOS.
  - NA
  - Add experiment flag to forward InstrumentedFilesInfo from
    non-tool deps by default.

This release contains contributions from many people at Google, as well as Abhishek Kumar, Adam Azarchs, Akira Baruah, Daniel Wagner-Hall, Derek Argueta, glukasiknuro, Greg Estren, Greg, Jason Furmanek, Jiri Dank, Keith Smiley, Kseniia Vasilchuk, Laurent Le Brun, Matt Mackay, Michael Klemm, Nikolay Shelukhin, Patrick Balestra, Rui Chen, Siggi Simonarson, sventiffe, Tobias Werth, Tom de Goede, Vladimir Chebotarev, Yannic Bonenberger.

## Release 3.2.0 (2020-05-27)

```
Baseline: 1d93d26d9900328dcba0026bf21cb45cc37a4596
```

Important changes:

  - Renamed --experimental_slim_json_profile to --slim_profile.
  - Expose ctx.attr.write Args content for analysistest when possible
  - Added aquery_differ_v2 that works with the new aquery proto
    output format.
  - Add new flag, 'experimental_no_product_name_out_symlink', to omit
    creating '<product>-out' symlink if user specifies
    '--symlink_prefix' option.
  - --ram_utilization_factor is removed (as is
    --incompatible_remove_ram_utilization_factor). Please use
    --local_ram_resources.
  - Improve include scanner support for cl.exe and clang-cl command
    lines
  - ctx.split_attr now includes attributes with Starlark split
    transitions.
  - Added --incompatible_string_replace_count, to make
    string.replace() behave as in Python.
  - Bazel can now recursively delete non-executable but readable
    directories.
  - Restore case-sensitivity to -I and /I include scanning detection
    to avoid conflicts.

This release contains contributions from many people at Google, as well as Alessandro Patti, Antoine Eiche, Beebs, Brian Silverman, George Gensure, Gerhard Pretorius, Gibson Fahnestock, Greg, Gregor Jasny, Gustav Westling, hannometer, Jin, Jonathan Gerrish, Josh Smith, Justin Y Wei, Laurent Le Brun, Links, liubang, Lszl Csomor, Matt Mackay, Moritz Krger, Ryan Beasley, Siddhartha Bagaria, Yannic Bonenberger, Yannic, Zhongpeng Lin.

## Release 3.1.0 (2020-04-21)

```
Baseline: 17ebbf15bea3733c0c21e0376fb5906e391edb49

Cherry picks:

   + 71fb56b4bb669a419f473598c8722e125dbb5c9e:
     Suppress last-ditch download exceptions w/cleanup
   + fd60614c38da0db22431d91a793423904dff801f:
     In repo rules, don't warn about generator_* attributes being
     non-canonical
   + 2a372792f5cc09bd0e8389d5fc4bbfdd2f5d6e37:
     Refactor configureFeatures and prevent NPE
   + 777a6ee6ed95cae4ddb949d52b63ee8a66024f03:
     Automated rollback of commit
     0bbe38176e15d891a4e4cab2d8425e292de6cc5f.
   + 2ee53c9267eb6de8eae109950ca8f3953bf1ae66:
     Guard parseActionResultMetadata with bulk wrapper
```

Incompatible changes:

  - The deprecated startup options
    --experimental_oom_more_eagerly(_threshold) are removed.

Important changes:

  - Remove flag
    --incompatible_load_proto_toolchain_for_javalite_from_com_google_p
    rotobuf which has been flipped in Bazel 3.0.
  - Allow assembly file sources in cc_common.compile.
  - Set default .netrc file on Windows to %USERPROFILE%
  - A maximum 150 attributes per RuleClass is enforced
  - Increase max targets per rule class limit to 200
  - ctx.split_attr now includes attributes with Starlark split
    transitions.
  - The --max_computation_steps flag bounds the computation done by a
    BUILD file.
  - Native patch can handle file permission properly
  - Aspects may now propagate through dependencies on rule outputs by
    being defined with `apply_to_generating_rules = True`.
  - 'query --output=build' now shows where rule classes (not just
    rules) are created.
  - The flag `incompatible_bzl_disallow_load_after_statement` is
    removed.
  - ctx.split_attr now includes attributes with Starlark split
    transitions.
  - The flag `incompatible_no_output_attr_default` is removed.
  - Fix wall-time of the SpawnResult in WorkerSpawnRunner
  - Add stamp parameter for cc_common.link to enable including build
    info
  - The flag `--incompatible_restrict_named_params` is removed.
  - The flag `--incompatible_depset_union` is removed.
  - Bazel uses fewer compiler flags by default when building
    Objective-C. In
    particular, Bazel no longer sets `-O0`, `-DDEBUG=1`,
    `-fstack-protector`, `-fstack-protector-all`, or `-g` in dbg
    mode, and
    Bazel no longer sets `-Os`, `-DNDEBUG=1`, `-Wno-unused-variable`,
    `-Winit-self`, or `-Wno-extra` in opt mode. If you want to apply
    these
    flags to your project, you can reenable them in your CROSSTOOL.
  - A maximum attribute name length is 128 is enforced

This release contains contributions from many people at Google, as well as Alessandro Patti, Benjamin Peterson, Benjamin Romano, Bor Kae Hwang, Chris Heisterkamp, Cristian Hancila, Dmitri G, Douglas Parker, George Gensure, Gregor Jasny, John Millikin, Keith Smiley, Leo, Mike Fourie, Patrick Balestra, Robbert Van Ginkel, Ryota, Samuel Giddins, Ulf Adams, Ulf Adams, Vertexwahn, Xavier Bonaventura, Yannic Bonenberger.

## Release 3.0.0 (2020-04-06)

```
Baseline: 3c7cc747ac653dca8b88a9e43726a794e2c27a9c

Cherry picks:

   + 19e214b44df9c82a8a3bd3381344f7145813c572:
     Use prefix encoding for paths.
   + 63b01f7b1cd6603a08bf2a8ae813388c201e3448:
     Avoid file operations in the sandbox creation critical path.
   + 80a2d7cc5f8a22816934dcd2ca9bdf87050f3d9f:
     Implementation (but not plumbing) of the gRPC remote downloader
   + 586eabf419972c74fdd5fef328cfe9a259e035ed:
     Implement RemoteDownloader w/ `--experimental_remote_downloader`
```

Incompatible changes:

  - Flip --incompatible_remove_enabled_toolchain_types, so that rules
    can no longer access enabled toolchain types from the platform
    fragment.
  - The --incompatible_use_jdk11_as_host_javabase flag has been
    removed (it was flipped in Bazel 0.25.0).
  - Using JDK 9 or 10 as a `--host_javabase` is no longer officially
    supported. As always, you can use the
    `@bazel_tools//tools/jdk:toolchain_vanilla` Java toolchain to use
    older or newer JDKs than what Bazel currently supports.
  - --fatal_event_bus_exceptions is deprecated and should not be
    used. Any crashes should be reported so that they can be fixed.
  - The old-style binary profile format is no longer suppported, use
    the new JSON trace profile instead.

Important changes:

  - Improve the performance of creating a sandboxed execution root
    for workers when the number of inputs is large (>1000).
  - Treat .cu and .cl files as C++ source. CUDA or OpenCL are not
    natively supported and will require custom flags to compile with
    e.g. clang.
  - Add actions' primary output details in JSON profile and
    analysis_v2.proto.
  - Add --cds_archive option for embedding CDS archive into deploy
    JAR.
  - Add new global attribute: applicable_licenses
    - Package level default with default_applicable_licenses
    - guarded by --incompatible_applicable_licenses (default true for
    Blaze, false for Bazel)
    - In support of
    https://docs.google.com/document/d/1uwBuhAoBNrw8tmFs-NxlssI6VRolid
    GYdYqagLqHWt8/edit#
  - Non-test Java rules no longer require the TestConfiguration and
    thus --trim_test_configuration is again usable for Java builds
  - The flag `--incompatible_always_check_depset_elements` is enabled
    by default.
  - --incompatible_objc_compile_info_migration determines
    whether native rules can assume compile info has been migrated to
    CcInfo. See https://github.com/bazelbuild/bazel/issues/10854.
  - --incompatible_remove_local_resources is true by default. Please
    use --local_ram_resources and --local_cpu_resources instead of
    --local_resources.

This release contains contributions from many people at Google, as well as Alessandro Patti, Benjamin Peterson, Christy Norman, Dave hughes, David Haxton, David Neil, garyschulte, George Chiramel, George Gensure, Gibson Fahnestock, Greg Estren, Greg, Jason Hoch, Jin, John Millikin, Jonathan Springer, Keith Smiley, Laurent Le Brun, Ulf Adams, Yannic Bonenberger, Yannic, Yihong Wang, Yuchen Dai.

## Release 2.2.0 (2020-03-03)

```
Baseline: 78055efad0917b848078bf8d97b3adfddf91128d
```

Incompatible changes:

  - The --[no]incompatible_windows_bashless_run_command flag is no
    longer supported. It was flipped in Bazel 1.0
  - The --[no]incompatible_windows_native_test_wrapper flag is no
    longer supported. It was flipped in Bazel 1.0

Important changes:

  - Consistent target naming style in example target names.
  - cquery's config() now supports arbitrary configurations.
  - The flag --incompatible_disallow_dict_lookup_unhashable_keys is
    removed.
  - Include target label in Python version error message.
  - The flag --incompatible_remap_main_repo is removed.
  - Windows: we now discourage running Bazel from MSYS2 because of a
    newly found bug (#10573)
  - Reduced the packaging time (`package-bazel.sh`) for the
    `//src:bazel-dev` Bazel development build target from 14s to 6s.
    Use `//src:bazel-dev` if you're iterating rapidly on a local
    Bazel changes, and use `//src:bazel --compilation_mode=opt` for
    release builds.
  - cquery: "//foo:bar" now means "all configured targets with label
    //foo:bar" instead of "choose an arbitrary configured target with
    label //foo:bar". See cquery docs for details.
  - WORKSPACE and BUILD.bazel files of http_archive repositories can
    now be patched using the "patch_cmds" and "patches" attributes.
  - Actions with "parse" on the critical path should no longer finish
    in the future.
  - Flags that affect external repositories like
    "--override_repository" can now be addressed in bazelrc files
    using the "common" command, without causing commands like "bazel
    shutdown" to fail.
  - The flag --incompatible_disallow_unverified_http_downloads is
    removed.
  - Create the incompatibleApplicableLicenses flag.
    We plan to flip this from false to true in Bazel 4.x.
    Implementation to follow.
  - Treat .cu and .cl files as C++ source. CUDA or OpenCL are not
    natively supported and will require custom flags to compile with
    e.g. clang.
  - Treat .cu and .cl files as C++ source. CUDA or OpenCL are not
    natively supported and will require custom flags to compile with
    e.g. clang.
  - The --starlark_cpu_profile=<file> flag writes a profile in
    pprof format containing a statistical summary of CPU usage
    by all Starlark execution during the bazel command. Use it
    to identify slow Starlark functions in loading and analysis.
  - --ram_utilization_factor will be deprecated. Please use
    --local_ram_resources=HOST_RAM*<float>
  - Docs: glob() documentation is rewritten, and now it points out a
    pitfall of rules shadowing glob-matched files.

This release contains contributions from many people at Google, as well as Alessandro Patti, Alex Kirchhoff, aman, Artur Dryomov, Benjamin Peterson, Benjamin Peterson, David Ostrovsky, Elliotte Rusty Harold, Eric Klein, George Chiramel, George Gensure, Guillaume Bouchard, Hui-Zhi, John Millikin, Jonathan Springer, Laurent Le Brun, Michael McLoughlin, nikola-sh, Nikolaus Wittenstein, Nikolay Shelukhin, Yannic Bonenberger, Yannic.

## Release 2.1.0 (2020-02-07)

```
Baseline: 41ec5a28fb30a8d6c5c60194c4bb29528352cf78

Cherry picks:

   + 733ac0ff7ad1e13ab68ceb2fde694d7ab71b2366:
     Adds an alias for d8_jar_import in android.WORKSPACE.
   + c94b1474b368dedf9f68f34fad90cc8664f2b0e0:
     Automatic code cleanup.
   + 2a8cc7075f741721563efd9dc050ca3458cde30b:
     Do not fail or print errors when Shellzelisk cannot find a
     requested ?
   + bdf34d7fd9539411e93348ba25307f34362d9a42:
     Fix race when extracting the install base when Bazel runs in a
     PID namespace.
   + d381c25bb7dd205c283f7ad91cac13190d2dfede:
     Fix wrong mode of install base directory.
   + eab39cc7b5a612f891fd1e5af5b45bdc1b66e6e1:
     Disable broken test_max_open_file_descriptors.
```

Incompatible changes:

  - The following attributes of CcToolchainProvider, which formerly
    were
    accessible both as fields (x.f) and as methods (x.f()), are now
    only fields:

New features:

  - Similar to the
    [.bazelignore](https://docs.bazel.build/versions/master/guide.html
    #.bazelignore) in the main repository, a `.bazelignore` file in
    external repository will cause the specified directories to be
    ignored by Bazel. Bazel won't try to identify any packages under
    the directories, but the files can still be referenced in other
    BUILD files.
  - bazelignore files now support line comments, e.g. "# this is a
    comment"

Important changes:

  - Experimental support for d8 merger is now available for use using
    --define=android_dexmerger_tool=d8_dexmerger.
  - Critical path run time should not have a longer run time than
    total execution run time.
  - Remove "please do not import" warning for cc rules.
  - x.f() is now equivalent to y=x.f; y(). That is, x.f should return
    the same
    attribute value regardless of whether it is accessed as a field
    or called
    like a method. Any arguments to the call are evaluated after the
    x.f operation.
  - Add desugaring support for RoleManager#addRoleHolderAsUser
  - Adds ctx.target_platform_has_constraint to allow rules to check
    the target platform's constraints.
  - Critical path run time should not have a longer run time than
    total execution run time.
  - Post new ConvenienceSymlinksIdentifiedEvent to the
    BuildEventProtocol when
    --experimental_convenience_symlinks_bep_event is enabled.
  - Keyword-only arguments are now allowed:
        def foo(a, *, k): pass
        def foo(a, *b, k): pass
      where k can be specified only by keyword:
        foo(5, k=6)
  - Generated Go protobufs now depend on //net/proto2/go:protodeps in
    addition to //net/proto2/go:proto
  - cquery 'somepath' returns more reliable results when the
    dep has a different configuration than the parent. To get a
    result for
    `somepath(//foo, //bar`) where //bar isn't in the top-level
    configuration,
    run your query with `--universe_scope=//foo`. See cquery docs for
    details.

This release contains contributions from many people at Google, as well as Adam Liddell, Alessandro Patti, Andreas Herrmann, Bor Kae Hwang, Brian Silverman, Emran BatmanGhelich, George Gensure, Greg Estren, Jeff Palm, Jonathan Beverly, Mark Nevill, Patrick Niklaus, Peter, Philipp Wollermann, Ryan Beasley, Shin Yamamoto, Yen-Chi Chen.

## Release 2.0.0 (2019-12-19)

```
Baseline: 807ed23e4f53a5e008ec823e9c23e2c9baa36d0d

Cherry picks:

   + db0e32ca6296e56e5314993fe9939bc7331768ec:
     build.sh: Fix bug in build script for RC release
   + 85e84f7812f04bc0dbc36376f31b6dd2d229b905:
     Set --incompatible_prohibit_aapt1 default to true.
   + 84eae2ff550c433a3d0409cf2b5525059939439d:
     Let shellzelisk fallback to bazel-real if it's the requested
     version.
   + d5ae460f1581ddf27514b4be18255481b47b4075:
     Fix a typo in bazel.sh
```

Incompatible changes:

  - --incompatible_remap_main_repo is enabled by default. Therefore,
    both ways of addressing the main repository, by its name and by
    '@' are now considered referring to the same repository.
    see https://github.com/bazelbuild/bazel/issues/7130
  - --incompatible_disallow_dict_lookup_unhashable_keys is enabled by
    default https://github.com/bazelbuild/bazel/issues/9184
  - --incompatible_remove_native_maven_jar is now enabled by default
    and the flag removed. See https://github.com/bazelbuild/bazel/issues/6799
  - --incompatible_prohibit_aapt1 is enabled by default.
    See https://github.com/bazelbuild/bazel/issues/10000

Important changes:

  - --incompatible_proto_output_v2: proto v2 for aquery proto output
    formats, which reduces the output size compared to v1. Note that
    the messages' ids in v2 are in uint64 instead of string like in
    v1.
  - Adds --incompatible_remove_enabled_toolchain_types.
  - Package loading now consistently fails if package loading had a
    glob evaluation that encountered a symlink cycle or symlink
    infinite expansion. Previously, such package loading with such
    glob evaluations would fail only in some cases.
  - The --disk_cache flag can now also be used together
    with the gRPC remote cache.
  - An action's discover inputs runtime metrics is now categorized as
    parse time on the CriticalPathComponent.
  - Make the formatting example more like to the written text by
    adding an initial description.
  - An action's discover inputs runtime metrics is now categorized as
    parse time on the CriticalPathComponent.
  - Bazel's Debian package and the binary installer now include an
    improved wrapper that understands `<WORKSPACE>/.bazelversion`
    files and the `$USE_BAZEL_VERSION` environment variable. This is
    similar to what Bazelisk offers
    (https://github.com/bazelbuild/bazelisk#how-does-bazelisk-know-whi
    ch-bazel-version-to-run-and-where-to-get-it-from), except that it
    works offline and integrates with apt-get.
  - We are planning to deprecate the runfiles manifest files, which
    aren't safe in the presence of whitespace, and also unnecessarily
    require local CPU when remote execution is used. This release
    adds --experimental_skip_runfiles_manifests to disable the
    generation of the input manifests (rule.manifest files) in most
    cases. Note that this flag has no effect on Windows by default or
    if --experimental_enable_runfiles is explicitly set to false.

This release contains contributions from many people at Google, as well as aldersondrive, Benjamin Peterson, Bor Kae Hwang, David Ostrovsky, Jakob Buchgraber, Jin, John Millikin, Keith Smiley, Lauri Peltonen, nikola-sh, Peter Mounce, Tony Hsu.

## Release 1.2.1 (2019-11-26)

```
Baseline: 11deef7582dfeec7a04ee3f7236393d9b8027367

Cherry picks:

   + c76c3e539c73ecf6e96e3e098be7be59e17bf276:
     Replace macOS CC path with relative path
   + 63332eb556fadfe9edd0806add79942482adddef:
     Hardcode path to dirname on macOS
   + ceadf0a063cb97c32aced143d2447781d1dafc38:
     Add tool executables (from FilesToRunProvider) to action inputs.
   + dbe63b00954a25fa4405f7cbf273df78c16498dd:
     Fix some of the bazel Windows tools code to work with GCC.
```

This release fixes a single regression:
- #10297: Bazel 1.2.0 does no longer run on macOS High Sierra.

It is the same code as Bazel 1.2.0, except that we rebuild the macOS release
binaries with Xcode 10.2.1 to make them backwards compatible with older macOS
versions again.

## Release 1.2.0 (2019-11-20)

```
Baseline: 11deef7582dfeec7a04ee3f7236393d9b8027367

Cherry picks:

   + c76c3e539c73ecf6e96e3e098be7be59e17bf276:
     Replace macOS CC path with relative path
   + 63332eb556fadfe9edd0806add79942482adddef:
     Hardcode path to dirname on macOS
   + ceadf0a063cb97c32aced143d2447781d1dafc38:
     Add tool executables (from FilesToRunProvider) to action inputs.
   + dbe63b00954a25fa4405f7cbf273df78c16498dd:
     Fix some of the bazel Windows tools code to work with GCC.
```

Incompatible changes:

  - Tree artifacts and regular artifact paths can no longer overlap.

New features:

  - Added a special "_validation" output group to enable moving
    "validation actions" off the critical path of builds.

Important changes:

  - The query flag "--host_deps" (commonly used as "--nohost_deps")
    has been renamed to "--tool_deps", and now also removes
    dependencies in any execution configuration from being reported
    in the query output. The previous flag name is deprecated and
    will be removed in a future release.
  - The `cc_common.{compile,link}` APIs can now be used without
    passing the `--experimental_cc_skylark_api_enabled_packages` flag.
  - A list of log paths will be provided in build output.
  - Improve runfiles documentation.
  - Improve documentation on rule outputs.
  - BUILD/.bzl execution errors cause execution to stop, even at
    top-level
  - Multiple Starlark validation errors are reported in a single pass.
  - Introduce --experimental_nested_set_as_skykey_threshold
  - Blaze will prevent idle sleep during test and build actions. Note
    that this does not affect screen savers and will not keep a
    laptop awake if the user forces sleep or closes the lid. This is
    purely to avoid idle sleeping when the user is not interacting
    with the device.
  - Improve testing docs.
  - Incompatible flag
    `--incompatible_validate_top_level_header_inclusions` has been
    added. See https://github.com/bazelbuild/bazel/issues/10047 for
    details.
  - Fix an aquery bug with handling malformed queries that crashes
    bazel.
  - List fields on CcLinkingOutputs.
  - [Python] Added flag --incomaptible_default_to_explicit_init_py to
    switch the default value of legacy_create_init to True. With this
    flag enabled, your py_binary and py_test targets will no longer
    behave as if empty __init__.py files were implicitly littered in
    your runfiles tree. See
    [#10076](https://github.com/bazelbuild/bazel/issues/10076).
  - Fix documentation on allowed target names.
  - --target_platform_fallback now also applies to exec/host
    configurations
  - android_binary and android_libary can now depend on targets
    providing
    CcInfos.
  - Add support for tracking suspensions (sleeps or SIGSTOP) on macOS.
  - d8 dexers (both standalone and incremental) are now available for
    use.
  - Add Desugar support for FreezePeriod#<init>

This release contains contributions from many people at Google, as well as Alex Kirchhoff, Andrew Suffield, Asaf Flescher, Austin Schuh, Benjamin Peterson, Bor Kae Hwang, Brian Richardson, Christy Norman, Clint Harrison, Dan Halperin, Daniel Martn, Dave Lee, David Neil, David Ostrovsky, George Gensure, Greg Estren, Greg, Ira Shikhman, Jacob Parker, Jakub Bujny, John Millikin, John Millikin, Keith Smiley, Laurent Le Brun, marcohu, Marwan Tammam, Mostyn Bramley-Moore, Peter Mounce, Ruben Das, Stepan Koltsov, Thi Don, Thi, Tomasz Strejczek, Walt Panfil, Yannic Bonenberger, Zackary Lowery.

## Release 1.0.1 (2019-10-21)

```
Baseline: 97a82646dadd93bf52d47828bda42e3383b657c6

Cherry picks:

   + a0e3bb207fe2044120a2555a37162ee1f2b17500:
     Remove support for authentication and .netrc
   + ada2c55dcc106cd55bafbbe5d9a966e21e4770e0:
     Add explicit --sdk argument to xcrun calls
   + 847df729528f6e5919ec8374247eadf792cba544:
     toolchain_vanilla: Unset source and target language level
     versions
   + 5cfa0303d6ac3b5bd031ff60272ce80a704af8c2:
     Update java_tools version to javac11-v5.1.
   + 9c257df5b9b1de5459fc066e77c435ae085957a7:
     Release 1.0.0 (2019-10-10)
   + f7a31b30f5319639c36a6c6b1c14f358597d98d2:
     Prevent action-cache duplicate suppression
   + 618e5a28f7f735c37724377b15775a4975349c74:
     Use FileSystemUtils.moveFile instead of renameTo function of Path
```

This release contains contributions from many people at Google, as well as David Ostrovsky, George Gensure, Keith Smiley.

## Release 1.0.0 (2019-10-10)

```
Baseline: 97a82646dadd93bf52d47828bda42e3383b657c6

Cherry picks:

   + a0e3bb207fe2044120a2555a37162ee1f2b17500:
     Remove support for authentication and .netrc
   + ada2c55dcc106cd55bafbbe5d9a966e21e4770e0:
     Add explicit --sdk argument to xcrun calls
   + 847df729528f6e5919ec8374247eadf792cba544:
     toolchain_vanilla: Unset source and target language level
     versions
   + 5cfa0303d6ac3b5bd031ff60272ce80a704af8c2:
     Update java_tools version to javac11-v5.1.
```

Incompatible changes:

  - Python, Windows: the
    --[no]incompatible_windows_escape_python_args is no longer
    supported. (It was flipped to true in Bazel 0.27.0)
  - --incompatible_use_native_patch is enabled by default
  - Windows: --incompatible_windows_bashless_run_command is now true
    by default, meaning "bazel run //foo:bin" will run the binary as
    a subprocess of the Bazel client. (When the flag is false, the
    binary is executed as a subprocess of Bash.)
  - Windows: --incompatible_windows_native_test_wrapper is enabled by
    default

New features:

  - Genrule now supports `cmd_bash`, `cmd_ps`, `cmd_bat` attributes.
    More details at
    https://docs.bazel.build/versions/master/be/general.html#genrule.c
    md
  - config_setting can now check multiple values on "--foo=firstVal
    --foo=secondVal ..."-style flags
  - tags: use `--experimental_allow_tags_propagation` flag to
    propagate tags to the action's execution requirements from
    targets. Such tags should start with: `no-`, `requires-`,
    `supports-`, `block-`, `disable-`, `cpu:`. See #8830 for details.
  - Users can now get generated def file from cc_library via
    "def_file" output group on Windows.
  - Platform-specific bazelrc: with --enable_platform_specific_config
    you can
    enable flags in bazelrc according to your host platform.
  - tags: use `--experimental_allow_tags_propagation` flag to
    propagate tags to the action's execution requirements from
    cc_library or cc_binary targets. Such tags should start with:
    `no-`, `requires-`, `supports-`, `block-`, `disable-`, `cpu:`.
    See #8830 for details.
  - tags: use --experimental_allow_tags_propagation flag to propagate
    tags to the action's execution requirements from java targets.
    Such tags should start with: no-, requires-, supports-, block-,
    disable-, cpu:. See #8830 for details.

Important changes:

  - Bazel Android builds now use aapt2 by default. To revert to aapt,
    set `--android_aapt=aapt`.
  - Make either --subcommands or --verbose_failures imply
    --materialize_param_files
  - Bazel Android builds now use aapt2 by default. To revert to aapt,
    set `--an...
    RELNOTES: None
  - by default all remote connections considered to be via `gRPC`
    with TLS enabled, unless other specified. To disable TLS use
    `grpc://` prefix for you endpoints. All remote connections via
    `gRPC` affected - `--remote_cache`, `--remote_executor` or
    `--bes_backend`. http cache/executor is not affected. See #8061
    for details.
  - cc_* rules support non-transitive defines through a
    'local_defines' attribute.
  - Enable
    incompatible_disallow_rule_execution_platform_constraints_allowed
    by default (https://github.com/bazelbuild/bazel/issues/8136).
  - incompatible_disallow_split_empty_separator is enabled by default
  - Fixed Android build issues with aapt2 on Windows. See the [GitHub
    issue](https://github.com/bazelbuild/bazel/issues/9102) for more
    information.
  - --incompatible_disable_static_cc_toolchains has been flipped. See
    https://github.com/bazelbuild/bazel/issues/8546.
  - --remote_default_platform_properties has been deprecated in favor
    of --remote_default_exec_properties.
  - The --incompatible_make_thinlto_command_lines_standalone flag has
    been flipped, see https://github.com/bazelbuild/bazel/issues/6791
    for more information.
  - The --incompatible_use_specific_tool_files flag has been flipped.
    See https://github.com/bazelbuild/bazel/pull/9126 for more
    information.
  - Clarify default visibility.
  - Enables incompatible_auto_configure_host_platform
  - New incompatible flag --incompatible_disable_depset_items
    disables the "items" parameter in the Starlark depset
    constructor. Use "direct" and "transitive" parameters instead.
  - --incompatible_assignment_identifiers_have_local_scope is enabled
  - incompatible_disable_partition_default_parameter is enabled by
    default ()
  - incompatible_restrict_attribute_names is enabled
    (https://github.com/bazelbuild/bazel/issues/6437)
  - The --incompatible_disable_nocopts flag has been flipped. See
    https://github.com/bazelbuild/bazel/issues/8546 for more
    information.
  - Deprecated Java-Starlark API java_common.create_provider is
    removed. JavaInfo() legacy args (actions, sources, source_jars,
    use_ijar, java_toolchain, host_javabase) are removed.
  - The flag incompatible_disallow_hashing_frozen_mutables is enabled
    (https://github.com/bazelbuild/bazel/issues/7800)
  - `maven_jar` and `maven_server` now disallow using plain HTTP URLs
    without a specified checksum. If you are still using `maven_jar`,
    consider migrating to
    [`rules_jvm_external`](https://github.com/bazelbuild/rules_jvm_ext
    ernal) for transitive dependency management. See
    [#8607](https://github.com/bazelbuild/bazel/issues/8607) for more
    information.
  - Added `sha256` and `sha256_src` attributes to `maven_jar`. Please
    consider migrating to SHA-256 as SHA-1 has been deemed
    cryptographically insecure ([https://shattered.io]()). Or, use
    [`rules_jvm_external`](https://github.com/bazelbuild/rules_jvm_ext
    ernal) to manage your transitive Maven dependencies with artifact
    pinning and SHA-256 verification support.
  - introducing per-target exec_properties
  - Bazel now supports ThinLTO builds on Linux for Clang versions >=
    6.0. ThinLTO can be enabled through --features=thin_lto
  - The Target.output_group field in Starlark is removed. Use
    OutputGroupInfo instead. See
    https://github.com/bazelbuild/bazel/issues/7949 for details.
  - Make a number of parameters of Starlark builtin functions
    positional-only (as opposed to specifiable by keyword). See
    https://github.com/bazelbuild/bazel/issues/8147 for details.
  - incompatible_skip_genfiles_symlink is enabled by default (#8651)
  - Change Pruned events will fire immediately after being checked.
  - --incompatible_remove_legacy_whole_archive has been flipped. See
    https://github.com/bazelbuild/bazel/issues/7362 for more
    information

This release contains contributions from many people at Google, as well as Adam Liddell, Alessandro Patti, Arshabh Kumar Agarwal, Artem Pelenitsyn, Artem Zinnatullin, Benjamin Peterson, David Ostrovsky, Emmanuel Goh, Farhim Ferdous, George Gensure, iirina, Keith Smiley, Kiril Videlov, Laurent Le Brun, Mantas Sakalauskas, Marwan Tammam, Matt Mukerjee, panzhongxian, Shachar Anchelovich, Stepan Koltsov, Stephan Wolski, Travis Clarke, Yannic Bonenberger, Yuta Saito.

## Release 0.29.0 (2019-08-28)

```
Baseline: 6c5ef5369a3ffceb8a65cc159a2fff1401242810

Cherry picks:

   + 338829f2633e91ae0492ee4169446465e10b5994:
     Fix retrying of SocketTimeoutExceptions in HttpConnector
   + 14651cd86b6fc1d48f56a208a9b5278b3e2dcf75:
     Fallback to next urls if download fails in HttpDownloader
   + b7d300c6be3e130dec0e62a4f19493105f595d57:
     Fix incorrect stdout/stderr in remote action cache. Fixes #9072
   + 960217631abdcab0a7ed95e2ab10acd55f636639:
     Automated rollback of commit
     0f0a0d58725603cf2f1c175963360b525718a195.
   + da557f96c697102ad787e57bbf7db2460f6a60a8:
     Windows: fix "bazel run" argument quoting
   + ef8b6f68cc8ffd2e6523a894034ae383e87ec74c:
     Return JavaInfo from java proto aspects.
   + 209175ff8ffeb05628ed8a187dd414a3d2935c55:
     Revert back to the old behavior of not creating a proto source
     root for generated .proto files.
   + 644060b7a4bc98384b66e3d2343b950b875b5e35:
     Fix PatchUtil for parsing special patch format
   + 067040d7bcb3b24a88432e210a96adacee3f37b4:
     Put the removal of the legacy repository-relative proto path
     behind the --incompatible_generated_protos_in_virtual_imports
     flag.
   + 76ed014e77d7b862f6eb2894600ae525ea570f11:
     repository mapping lookup: convert to canonical name first
```

Important changes:

  - rule_test: fix Bazel 0.27 regression ("tags" attribute was
    ingored, https://github.com/bazelbuild/bazel/issues/8723
  - Adds --incompatible_enable_execution_transition, which enables
    incremental migration of host attributes to exec attributes.
  - objc_proto_library rule has been deleted from Bazel.
  - repository_ctx.read is no longer restricted to files
        in the repository contructed.
  - tags 'no-remote', 'no-cache', 'no-remote-cache',
    'no-remote-exec', 'no-sandbox' are propagated now to the actions
    from targets when '--ncompatible_allow_tags_propagation' flag set
    to true. See #8830.
  - Adds flag
    --//tools/build_defs/pkg:incompatible_no_build_defs_pkg. This
    flag turns off the rules //tools/build_defs/pkg:{pkg_deb,
    pkg_rpm, pkg_tar}.
  - The Android NDK is now integrated with toolchains. To use them,
    pass the `--extra_toolchains=@androidndk//:all` flag or register
    them in your WORKSPACE with
    `register_toolchains("@androidndk//:all")`.
  - Stdout and stderr are checked to determine if output is going to a
    terminal. `--is_stderr_atty` is deprecated and `--isatty` is
    undeprecated.
  - --incompatible_load_proto_rules_from_bzl was added to forbid
    loading the native proto rules directly. See more on tracking
    issue #8922
  - Docker Sandbox now respects remote_default_platform_properties
  - pkg_deb, pkg_rpm & pkg_tar deprecation plan announced in the
    documentation.
  - The new java_tools release:
    * fixes https://github.com/bazelbuild/bazel/issues/8614
    * exposes a new toolchain `@java_tools//:prebuilt_toolchain`
    which is using all the pre-built tools, including singlejar and
    ijar, even on remote execution. This toolchain should be used
    only when host and execution platform are the same, otherwise the
    binaries will not work on the execution platform.
  - java_common.compile supports specifying
    annotation_processor_additional_inputs and
    annotation_processor_additional_outputs for the Java compilation
    action for supporting annotation processors that consume or
    produce artifacts. Fixes #6415
  - There is now documentation on optimizing Android app build
    performance. Read it at
    https://docs.bazel.build/versions/0.29.0/android-build-performance
    .html
  - Execution log now respects --remote_default_platform_properties
  - Include a link to the relevant documenation on transitive Python
    version errors.
  - New incompatible flag
    --incompatible_disable_target_provider_fields removes the ability
    (in Starlark) to access a target's providers via the field syntax
    (for example, `ctx.attr.dep.my_provider`). The provider-key
    syntax should be used instead (for example,
    `ctx.attr.dep[MyProvider]`). See
    https://github.com/bazelbuild/bazel/issues/9014 for details.
  - A new platform exec_properties is added to replace
    remote_execution_properties.
  - Added --incompatible_load_python_rules_from_bzl, which will be
    flipped in Bazel 1.0. See
    https://github.com/bazelbuild/bazel/issues/9006.
  - add --break_build_on_parallel_dex2oat_failure to shortcut tests
    on dex2oat errors

This release contains contributions from many people at Google, as well as Alexander Ilyin, Arek Sredzki, Artem Zinnatullin, Benjamin Peterson, Fan Wu, John Millikin, Loo Rong Jie, Marwan Tammam, Oscar Bonilla, Peter Mounce, Sergio Rodriguez Orellana, Takeo Sawada, and Yannic Bonenberger.

## Release 0.28.1 (2019-07-19)

```
Baseline: 2e374a9c6e3d4ed71f0145de287c4b2fe43c76d6

Cherry picks:

   + 6d0b14b95a71175362030b4811ca74512b00a890:
     rule_test: apply "tags" to all rules in the macro
   + 18cd9048526f067a4950c6ddbf4b1b3604760af0:
     Release 0.28.0 (2019-07-10)
   + 2260b970cb2a10c21a2fde116aa82c79be52991f:
     When copying dynamic libraries to binary, copy them into the
     runfiles as well.
   + a0af170f87c8230fcd8860599b983df097b646bd:
     Undo breaking change of hiding android_common behind
     --experimental_google_legacy_api
   + c6ca6c2ccd9e11a87f837341ae05d2ba037975b1:
     Ignore external/ directory in users' source tree when creating
     execroot symlink tree.
```

Important changes:

  - rule_test: fix Bazel 0.27 regression ("tags" attribute was
    ingored, https://github.com/bazelbuild/bazel/issues/8723

This release contains contributions from many people at Google, as well as .

## Release 0.28.0 (2019-07-10)

```
Baseline: 2e374a9c6e3d4ed71f0145de287c4b2fe43c76d6

Cherry picks:

   + 6d0b14b95a71175362030b4811ca74512b00a890:
     rule_test: apply "tags" to all rules in the macro
```

Incompatible changes:

  - Add --incompatible_enable_profile_by_default to enable the JSON
    profile by default.
  - The --incompatible_windows_style_arg_escaping flag is flipped to
    "true", and the "false" case unsupported. Bazel no longer accepts
    this flag.

Important changes:

  - Bazel now supports hiding compiler warnings for targets that
    you're not explicitly building (see
    https://docs.bazel.build/versions/master/user-manual.html#flag--au
    to_output_filter).
  - Flag `--incompatible_restrict_escape_sequences` is added. See
    https://github.com/bazelbuild/bazel/issues/8380
  - The "info" command now supports the "starlark-semantics"
    argument, which outputs a representation of the effective Starlark
    semantics option values.
  - The `outputs` parameter of the `rule()` function is deprecated
    and attached to flag `--incompatible_no_rule_outputs_param`.
    Migrate rules to use `OutputGroupInfo` or `attr.output` instead.
    See https://github.com/bazelbuild/bazel/issues/7977 for more info.
  - When `--incompatible_strict_action_env` is enabled, the default
    `PATH` now includes `/usr/local/bin`.
  - Turn on --experimental_build_setting_api by default for starlark
    build settings (see
    https://docs.bazel.build/versions/master/skylark/config.html#user-
    defined-build-settings for more info)
  - `@bazel_tools//tools/jdk:toolchain_java10` and
    `@bazel_tools//tools/jdk:toolchain_java11` are now available to
    enable java 10, respectively java 11 language level support.
  - The `command` parameter of the `actions.run_shell()` function
    will be restricted to only accept strings (and not string
    sequences). This check is attached to flag
    `--incompatible_run_shell_command_string`. One may migrate by
    using the `arguments` parameter of `actions.run()` instead. See
    https://github.com/bazelbuild/bazel/issues/5903 for more info.
  - Incompatible change
    `--incompatible_use_platforms_repo_for_constraints` has been
    added. See https://github.com/bazelbuild/bazel/issues/8622 for
    details.
  - Incompatible change
    `--incompatible_use_platforms_repo_for_constraints` has been
    added. See https://github.com/bazelbuild/bazel/issues/8622 f...
  - Bazel's C++ autoconfiguration now understands `BAZEL_LINKLIBS`
    environment variable to specify system libraries that should be
    appended to the link command line.
  - paths under the execution root starting with "." or "_" will be
    re-linked across builds
  - execution_log_json_file now allows actions without outputs.
  - Labels aapt as deprecated for aapt_version, and heavily endorses
    aapt2.
  - Update doc links still pointing to cc_binary.features to point to
    common features
  - Incompatible change
    `--incompatible_use_platforms_repo_for_constraints` has been
    added. See https://github.com/bazelbuild/bazel/issues/8622 for
    details.
    RELNOTES:
  - --incompatible_disable_nocopts flag has been added. See
    https://github.com/bazelbuild/bazel/issues/8706 for details.
  - Fixed treatment of <dist:module /> tags in AndroidManifest.xml
  - Fixed asset precedence for android_binary rules with aapt2.
  - Bazel now officially supports running on CentOS 7.
  - The runtime dynamic libraries are no longer in default output
    group of cc_binary.
  - set the FDOBuildType as CSFDO for binaries built with
    --cs_fdo_absolute_path.
  - Bazel can now be bootstrapped and built on arm64 platforms
    without requiring any flags or patches.
  - Fixed treatment of AndroidManifest.xml attributes which contained
    XML escaping
  - Retire experimental blaze flag
    experimental_link_compile_output_separately. The same behavior is
    available through the feature dynamic_link_test_srcs.
  - --incompatible_load_java_rules_from_bzl was added to forbid
    loading the native java rules directly. See more on tracking
    issue #8746
  - Turn on --experimental_build_setting_api by default for starlark
    build settings (see
    https://docs.bazel.build/versions/master/skylark/config.html#user-
    defined-build-settings for more info)
  - Attribute names are going to be restricted and must be
    syntactically valid identifiers.
    https://github.com/bazelbuild/bazel/issues/6437
  - rule_test: fix Bazel 0.27 regression ("tags" attribute was
    ingored, https://github.com/bazelbuild/bazel/issues/8723

This release contains contributions from many people at Google, as well as Ben Diuguid, Benjamin Peterson, Dave Lee, Loo Rong Jie, Mark Butcher, Marwan Tammam, Pedro Alvarez.

## Release 0.27.1 (2019-07-02)

```
Baseline: 8c3b3fba3f68833bd97d3df2db9c48f0539efc3b

Cherry picks:

   + 123c68daed17b19927372e4df7f7a2256db6b80e:
     Warn in more cases of possible Python version mismatch in host
     config
   + 052167e907373ac7ea43238c3049739f6e94a9d1:
     Add a non-strict autodetecting Python toolchain
   + 6ef6d879ab69225d54ecab3db847fb4eff33bbeb:
     Default java toolchain target
   + 50fa3ec27efdd95771c70faa38a4543d4fed44f2:
     Fix problems with the non-strict Python toolchain
   + e2a626c4f61fe4ceb79a5675d09a6f25ca7b5b22:
     Automated rollback of commit
     bc6f7cb330bb531f062bb301f3703876051191f5.
   + 6efc5b787ad3164cc2fb779c73377695032b4524:
     Treat existence of managed directories as a part of repository
     dirtiness.
   + 3a4be3c93813987a27a97dade3f9ebbc5770e349:
     Add /usr/local/bin to default PATH under strict action env
   + 5c1005c0947b010ee36ca851b8ba07c9479cf682:
     Automated rollback of commit
     536a166270590a8dbc701718550383f3a07cc763.
   + c82eb4878c7dc829455caeb915affe36c89df06f:
     Release 0.27.0 (2019-06-17)
   + d4589630428c1c1bc08a9baf36257e636fe9d746:
     Check for both :lcov_merger and $lcov_merger  attributes in
     TestActionBuilder.
```

Important changes:

  - Add new options --cs_fdo_absolute_path= to support the absolute
    path
    profile for LLVM's context-sensitive FDO.
  - When `--incompatible_strict_action_env` is enabled, the default
    `PATH` now includes `/usr/local/bin`.
  - Turn on --experimental_build_setting_api by default for starlark
    build settings (see
    https://docs.bazel.build/versions/master/skylark/config.html#user-
    defined-build-settings for more info)

This release contains contributions from many people at Google, as well as Ricky Pai.

## Release 0.27.0 (2019-06-17)

```
Baseline: 5935259724bebd1c4bdebc90e159d0f655c8c219

Cherry picks:

   + fe81b49e727efdcc90a270520af193af75f4e31d:
     Support of using the absolute path profile for LLVM's Context
     Sensitive FDO
   + ce5e7180d98e1244fdfba0349952727826cbd173:
     Demote OptionProcessor from globals
   + 3ed9d3681c3e130aafcf3c405ff1795c601bdf95:
     Properly wire up BES half-close. The lack thereof was a simple
     oversight.
   + 4ca768e9f87701fb92598d0a8325a5fc8881a852:
     standardize graph output indentation with 2 spaces
   + aff189a7c514a0171a53a2dcdd37c93ecaa672ad:
     Make sure default Linux artifacts have an associated action,
     even when artifact names are altered.
   + 8c3b3fba3f68833bd97d3df2db9c48f0539efc3b:
     Failures early in package loading will now fail all --keep_going
     builds.
   + 123c68daed17b19927372e4df7f7a2256db6b80e:
     Warn in more cases of possible Python version mismatch in host
     config
   + 052167e907373ac7ea43238c3049739f6e94a9d1:
     Add a non-strict autodetecting Python toolchain
   + 6ef6d879ab69225d54ecab3db847fb4eff33bbeb:
     Default java toolchain target
   + 50fa3ec27efdd95771c70faa38a4543d4fed44f2:
     Fix problems with the non-strict Python toolchain
   + e2a626c4f61fe4ceb79a5675d09a6f25ca7b5b22:
     Automated rollback of commit
     bc6f7cb330bb531f062bb301f3703876051191f5.
   + 6efc5b787ad3164cc2fb779c73377695032b4524:
     Treat existence of managed directories as a part of repository
     dirtiness.
   + 3a4be3c93813987a27a97dade3f9ebbc5770e349:
     Add /usr/local/bin to default PATH under strict action env
   + 5c1005c0947b010ee36ca851b8ba07c9479cf682:
     Automated rollback of commit
     536a166270590a8dbc701718550383f3a07cc763.
```

Incompatible changes:

  - --incompatible_disable_objc_provider_resources no longer has
    effect. Use of deprecated resource fields on the Objc provider is
    now disallowed regardless of this flag.
  - deleted deprecated --experimental-remote-retry* flags, please use
    --remote_retries instead
  - flipped --incompatible_list_based_execution_strategy_selection
    flag to be true by default. See
    https://github.com/bazelbuild/bazel/issues/7480 for details.
  - Octal integer literals in Starlark are required to start with
    "Oo".
  - The "native" module is no longer available in BUILD files, all
    its members can be accessed directly as global symbols. This can
    be temporarily reverted by providing
    --incompatible_disallow_native_in_build_file=false as a flag to
    Blaze.
  - The "native" module is no longer available in BUILD files, all
    its members can be accessed directly as global symbols. This can
    be temporarily reverted by providing
    --incompatible_disallow_native_in_build_file=false as a flag to
    Blaze.
  - Turn off binary style profile format.
  - The "native" module is no longer available in BUILD files, all
    its members can be accessed directly as global symbols. This can
    be temporarily reverted by providing
    --incompatible_disallow_native_in_build_f...
  - cleanup that affects user provided apple frameworks is
    now enabled by default.  See
    https://github.com/bazelbuild/bazel/issues/7944 for more info.
  - Python rules now determine the Python runtime using toolchains
    rather than `--python_top` and `--python_path`, which are
    deprecated. See
    [#7899](https://github.com/bazelbuild/bazel/issues/7899) for
    information on declaring Python toolchains and migrating your
    code. As a side-benefit, this addresses
    [#4815](https://github.com/bazelbuild/bazel/issues/4815)
    (incorrect interpreter version used) on non-Windows platforms.
    Note however that some builds break due to getting the version
    they asked for -- consider setting `python_version = "PY2"` on
    Python 2 targets and `--host_force_python=PY2` if any Python 2
    targets are used in the host configuration. You can temporarily
    opt out of this change with
    `--incompatible_use_python_toolchains=false`.
  - Depsets can't be iterated over unless they're converted to lists
    using the .to_list() method. Use
    --incompatible_depset_is_not_iterable=false to
    temporarily restore the previous behaviour.

New features:

  - Bash, runfiles: the copy-pasted init code of the Bash runfiles
    library is now shorter, see `tools/bash/runfiles/runfiles.bash`.
    The rlocation() implementation is the same.
  - Bash, runfiles: the copy-pasted init code of the Bash runfiles
    library is now shorter, see `tools/bash/runfiles/runfiles.bash`.
    The rlocation() implementation is the same.
  - Bash, runfiles: the copy-pasted init code of the Bash runfiles
    library is now shorter, see `tools/bash/runfiles/runfiles.bash`.
    To use the new init code, you need Bazel 0.27 or newer. The old
    (longer) init code still works.

Important changes:

  - The `outputs` parameter of the `rule()` function is deprecated
    and attached to flag `--incompatible_no_rule_outputs_param`.
    Migrate rules to use `OutputGroupInfo` or `attr.output` instead.
    See https://github.com/bazelbuild/bazel/issues/7977 for more info.
  - The --incompatible_disable_objc_library_resources flag is being
    removed. Please remove it from your configs as it otherwise will
    fail the build.
  - Add a generic additional_linker_inputs attribute on cc_binary
    rules.
  - Windows, C++ autoconfigure: BAZEL_VC and BAZEL_VS may now have
    quotes, so if you set these envvars' values in cmd.exe via
    TAB-completion then you no longer need to remove the surrounding
    quotes.
  - pkg_deb has new attributes: `config` and `templates` that can be
    used for integration with debconf
  - Allow cc_import() of a DLL with no interface library on Windows,
    used to document runtime dependencies.
  - All host-configured Python tools that are built for the wrong
    Python version will now emit a warning message when they exit
    with non-zero status. See #7899.
  - deprecated --remote_local_fallback_strategy. Use
    `--strategy=remote,local` instead. See
    https://github.com/bazelbuild/bazel/issues/7480.
  - Introduce --incompatible_disable_native_android_rules flag
  - The Android desugaring actions now support a persistent worker
    mode for faster local build performance. Enable it with
    `--strategy=Desugar=worker`.
  - --incompatible_static_name_resolution_in_build_files is now
    enabled by default
  - --incompatible_disable_deprecated_attr_params is now enabled by
    default (#5818)
  - Repository containing autoconfigured C++ toolchain
    `@local_config_cc` has been split in 2 - see
    `local_config_cc_toolchains`.
  - --incompatible_string_join_requires_strings is now enabled by
    default
  - Flag --incompatible_new_actions_api is enabled by dewfault (#5825)
  - New flag `--incompatible_disallow_empty_glob`. See
    https://github.com/bazelbuild/bazel/issues/8195
  - --incompatible_no_kwargs_in_build_files is enabled by default
  - Incompatible flag
    `--incompatible_require_ctx_in_configure_features` has been
    flipped. See https://github.com/bazelbuild/bazel/issues/7793 for
    more information.
  - `BAZEL_USE_XCODE_TOOLCHAIN=1` tells Bazel not to look for Xcode to
    decide whether to enable toolchains for Apple rules, but to
    assume Xcode is
    available. Can be also used when building on Darwin and no C++ or
    ObjC is being
    built, so there is no need to detect Xcode.
  - Android desugaring actions now use persistent workers by default.
    This has been measured to provide up to 20% reduction in build
    times. To disable it, use the `--strategy=Desugar=sandboxed`
    flag. See https://github.com/bazelbuild/bazel/issues/8342 and
    https://github.com/bazelbuild/bazel/issues/8427 for more details
    on local build speed optimization for Android apps.
  - Fixed an issue with Android builds where `--fat_apk_cpu` doesn't
    pack all selected shared libraries from `aar_import` targets into
    the APK. See
    [#8283](https://github.com/bazelbuild/bazel/issues/8283).
  - Turn on --experimental_starlark_config_transitions by default for
    starlark transitions (see
    https://docs.bazel.build/versions/master/skylark/config.html#user-
    defined-transitions for more info)
  - Turn on --experimental_build_setting_api by default for starlark
    build settings (see
    https://docs.bazel.build/versions/master/skylark/config.html#user-
    defined-build-settings for more info)
  - Incompatible flag
    `--incompatible_dont_enable_host_nonhost_crosstool_features` has
    been flipped. See https://github.com/bazelbuild/bazel/issues/7407
    for more information.
  - Added support for Android NDK 19 and 20.
  - Flip --incompatible_no_support_tools_in_action_inputs
  - --remote_executor, --remote_cache or --bes_backend=someurl.com
    would be treated as grpcs://someurl.com, if the
    --incompatible_tls_enabled_removed flag enabled. See
    https://github.com/bazelbuild/bazel/issues/8061 for details.
  - Add new options --cs_fdo_absolute_path= to support the absolute
    path
    profile for LLVM's context-sensitive FDO.
  - When `--incompatible_strict_action_env` is enabled, the default
    `PATH` now includes `/usr/local/bin`.
  - Turn on --experimental_build_setting_api by default for starlark
    build settings (see
    https://docs.bazel.build/versions/master/skylark/config.html#user-
    defined-build-settings for more info)

This release contains contributions from many people at Google, as well as Alex Thompson, Andy Scott, Benjamin Peterson, David McNett, Drew Gassaway, Ira Shikhman, James deBoer, Jay Vercellone, Jingwen Chen, Josh Rosen, Keith Smiley, Laurent Le Brun, Lee Mracek, Marwan Tammam, Matt Passell, Michael Hackner, Michal Majcherski, Patrick Niklaus, Peter Mounce, Ricky Pai, Steeve Morin, szakmary, Takuto Ikuta, Vladimir Chebotarev, Yen-Chi Chen.

## Release 0.26.1 (2019-06-06)

```
Baseline: cb82ed84d44db0169a8fbf15f9cee434b77002bb

Cherry picks:

   + d1c0d205945f5a765efb0a48593b1cd82699ce32:
     Allow WORKSPACE file to be a symlink if no managed directories
     is used.
   + c3d2aa74ccd23dfb8a8173c2b3e2955f0c5892cb:
     Fix ios, tvos and watchos arm64 constraints
   + 55e42052a22a60b68d88a89932b2a068311b1a95:
     Bump java_tools_javac10 from 3.1 to 3.2
```

Patch release on top of 0.26.0, fixing
- https://github.com/bazelbuild/bazel/issues/8475
- https://github.com/bazelbuild/bazel/issues/8520
- https://github.com/bazelbuild/intellij/issues/845

## Release 0.26.0 (2019-05-28)

```
Baseline: daa8ae565ab2023e49134f0aad233b0a8bd7a5d0

Cherry picks:

   + 61c7ffa60ae9c1937dd6d8ee30a8c0471094ee71:
     Automated rollback of commit
     87388e24814b177f54ca7697b4233489f90c587e.
   + 898d7b6138af73f03daf35b767d252560087de70:
     Add test for repository overrides, conflicting with managed
     directories being added when Bazel server is already started.
   + c2001a4569483596d9dc74ba9cabcbe4b6f1887f:
     Automated rollback of commit
     bbe47a1564a832e1a175206f2dfbc92af94c120b.
   + e67c961905792cd63950c6f6efc33275ad243c49:
     Fix a non-determinism in create_embedded_tools.py.
   + 81aefe7ee01cc73646a53f9c72ed40ead09f9f5a:
     Remove unsupported cpu attribute from cc_toolchains.
   + 597e289b097d3bfed8eea1cb0924bbeb04877e42:
     remote: made CombinedCache a composition of Disk and Http Cache
   + 942f7cf6a0da0a4ecc804615424f039e50963933:
     C++: Fixes bug in C++ API with external repo aspects
   + 85a5a2bd569a5274950fc7327a044c395248c024:
     Configure @androidsdk//:emulator_x86 and :emulator_arm to point
     to the unified emulator binary
   + 9835cb4135503768cdf1161746b95d7969ccb938:
     Automated rollback of commit
     844e4e297b404d6ff28b818d8150d4b9c47de887.
   + c963ba21073b514961946b8b4b45b091f08fdaa1:
     Windows, Python: fix arg. esc. also in host config
   + a1ea487e0a9e180a36fa4aab57f7c746ddcf367a:
     Do not pre-cache changed files under managed directories
   + 7dc78cdd04eedf2f4373b170053ba5fc2a990929:
     Add explicit execution and target constraints for autodiscovered
     cc t?
   + dd9ac13f7e3b71bdf2eca717bc7681bdd12389a2:
     Fix a bug when a relative path is used for the execution log
   + 0ff19c6d0adf3c0df94fff59ca3bd13cbcf99897:
     Fix StandaloneTestStrategy.appendStderr
   + 7f495315749478e75a3424726cc273a535b7c3b8:
     Fix the autodetecting Python toolchain on Mac
   + ddce7235ef29a0aba727c265eae865d15af4ed09:
     Avoid exporting PATH unnecessarily
   + 35dd05a059fa7fddfdd888cfc69102994e3c04dc:
     Allow Starlark rules to be able to use the `exec_compatible_with`
```

### Incompatible changes

- [`--incompatible_windows_escape_jvm_flags`](https://github.com/bazelbuild/bazel/issues/7486)
  is enabled by default, and removed.

- [`--incompatible_windows_style_arg_escaping`](https://github.com/bazelbuild/bazel/issues/7454)
  is enabled by default.

- [`--incompatible_no_output_attr_default`](https://github.com/bazelbuild/bazel/issues/7950)
  is enabled by default.

- [`--incompatible_depset_union`](https://github.com/bazelbuild/bazel/issues/5817)
  is enabled by default.

- [`--incompatible_disable_genrule_cc_toolchain_dependency`](https://github.com/bazelbuild/bazel/issues/6867) removed

- [`--Incompatible_disallow_rule_execution_platform_constraints_allowed`](https://github.com/bazelbuild/bazel/issues/8136):
  When enabled, disallows the use of the `execution_platform_constraints_allowed`
  attribute when defining new rules.

- [`--incompatible_disable_objc_provider_resources`](https://github.com/bazelbuild/bazel/issues/5710)
  is now enabled by default.

### Apple

- The `--ios_multi_cpus`, `--watchos_cpus`, `--macos_cpus` and `--tvos_cpus` are
  now additive. This means that you can now split the `--ios_multi_cpus=arm64,armv7`
  into `--ios_multi_cpus=arm64` and `--ios_multi_cpus=armv7`.

- Add `--incompatible_objc_framework_cleanup` to control whether to enable some
  objc framework cleanup that changes the API. Specifically, the cleanup changes
  the objc provider API pertaining to frameworks. This change is expected to be
  transparent to most users unless they write their own Starlark rules to handle
  frameworks. See https://github.com/bazelbuild/bazel/issues/7594 for details.

- `objc_library` does not support resource attributes any more. Please read
  [#7594](https://github.com/bazelbuild/bazel/issues/7594) for more info.

### Android

- Android resource conflicts will no longer be reported between a strong attr
  resource and a weak attr resource, if the weak attr does not have format
  specified.

### C++

- [Allow debugging C++ features logic](https://github.com/bazelbuild/bazel/commit/53156db1e30fe7197889c3cbc8d1321ff9b85143

- Add new options [`--cs_fdo_instrument`](https://docs.bazel.build/versions/master/command-line-reference.html#flag--cs_fdo_instrument)
  and [`--cs_fdo_profile`](https://docs.bazel.build/versions/master/command-line-reference.html#flag--cs_fdo_profile)
  to support LLVM's context-sensitive FDO (CSFDO).

- [Bazel C++ compile/link Starlark API](https://github.com/bazelbuild/bazel/issues/4570).
  Can be used with experimental flag `--experimental_cc_skylark_api_enabled_packages`.

- `cc_toolchain.static_runtime_lib` and `cc_toolchain.dynamic_runtime_lib` are
  now exposed to Starlark.

- C++ Starlark API for compilation and linking is no longer whitelisted

- Incompatible flag `--incompatible_make_thinlto_command_lines_standalone` has
  been added. See https://github.com/bazelbuild/bazel/issues/6791 for details.

- Feature `disable_whole_archive_for_static_lib` has been added to allow turning
  off legacy_whole_archive for individual targets.
  https://github.com/bazelbuild/bazel/issues/7362

### Python

- Fixed an issue where some `py_runtimes` were incompatible with using
  `--build_python_zip` ([#5104](https://github.com/bazelbuild/bazel/issues/5104)).

### Starlark

- The flag [`--incompatible_restrict_named_params`](https://github.com/bazelbuild/bazel/issues/8147)
  is added. [Buildifier](https://github.com/bazelbuild/buildtools) will soon be
  updated to automatically fix this in your code.

- The flag [`--incompatible_no_kwargs_in_build_files`](https://github.com/bazelbuild/bazel/issues/8021)
  is added. It fixes a bug where the `**kwarg` notation was allowed in `BUILD`
  files.

- The flag [`--incompatible_static_name_resolution_in_build_files`](https://github.com/bazelbuild/bazel/issues/8022)
  is added. This fixes a bug where invalid code was silently accepted.

- The flag [`--incompatible_disallow_old_octal_notation`](https://github.com/bazelbuild/bazel/issues/8059)
  is added. It removes a deprecated notation for octal numbers.
  [Buildifier](https://github.com/bazelbuild/buildtools) can update your code
  automatically.

- [`struct.to_proto()`](https://docs.bazel.build/versions/master/skylark/lib/struct.html#to_proto)
  can now convert dictionaries into proto3 text message.

- The [`glob`](https://docs.bazel.build/versions/master/be/functions.html#glob)
  function has a new argument `allow_empty`. When set to `False` (this will be
  the default in the future), the glob fails when it doesn't match anything.
  Please explicitly set `allow_empty = True` to the globs that need it.

### Windows

- Windows, Python: the new `--incompatible_windows_escape_python_args` flag
  (`false` by default) builds `py_binary` and `py_test` targets with correct
  command line argument escaping. Similar to
  https://github.com/bazelbuild/bazel/issues/7486 but for `py_*` rules.

### Other changes

- cquery supports `--output=build`

- Generated Go protobufs now depend on `//net/proto2/go:proto_gendeps` instead
  of `//net/proto2/go:proto`

- Added `--incompatible_remove_binary_profile` to disable the old binary
  profiles. Instead use the JSON profile format:
  https://docs.bazel.build/versions/master/skylark/performance.html#json-profile

- Introducing `--execution_log_binary_file` and `--execution_log_json_file`
  that output a stable sorted execution log. They will offer a stable
  replacement to `--experimental_execution_log_file`.

- New platform_mappings ability to allow gradual flag to platforms/toolchains
  migration. See also https://github.com/bazelbuild/bazel/issues/6426

- Added support for compiling against fully qualified `R` classes from
  `aar_import` dependencies.

- `--tls_enabled` flag is deprecated. Please provide `grpcs` as a scheme in
  the URLs if TLS should be used for a remote connection.

### Community

- Alex Eagle gave a talk at ng-conf:
  [The Bazel Opt-in Preview is Here!](https://www.youtube.com/watch?v=J1lnp-nU4wM)

- The latest release of
  [rules_haskell](https://github.com/tweag/rules_haskell/releases/tag/v0.9)
  includes many new improvements, including an initial Windows support.

- The latest
  [Buildifier release](https://github.com/bazelbuild/buildtools/releases/tag/0.25.1)
  includes Windows binaries, as well as a JSON output for diagnostics.

- Natan Silnitsky gave the talk
  [How to successfully migrate to Bazel from Maven or Gradle](https://www.youtube.com/watch?v=2UOFm-Cc_cU)
  at the Java Eastern Europe Conference.

This release contains contributions from many people at Google, as well as
Benjamin Peterson, Brian Topping, clyang82, Dave Lee, George Gensure,, Greg,
Guro Bokum, Keith Smiley, Max Vorobev, Michael Hackner, Robert Brown, Robert
Sayre, Ryan Beasley, and Yannic.


## Release 0.25.3 (2019-05-23)

```
Baseline: 03662462941953dad23af88693804b8b1d4430b9

Cherry picks:

   + 3f7f255f9a8345b8898e3953e7e53d68106cc63d:
     Windows: fix native test wrapper's arg. escaping
   + afeb8d0b7fef619159fc8fbaaeb8bd41dd2619bd:
     Flip --incompatible_windows_escape_jvm_flags
   + 4299b6549cbc1b3e4494c91ed2f51d49b14c7980:
     Sort DirectoryNode children to ensure validity.
   + 231270c67d5aa771462245531fa9b2ee7d3d0ae8:
     Conditionally use deprecated signature for initWithContentsOfURL
   + 75a3a531b08e727ade4fa3cb0a574bd142727cce:
     Add http_archive entries for testing with various JDK versions.
   + 4a6354a3a5ca23583f8b62e3e439a04ce75b863f:
     Now that ubuntu1804 uses JDK 11, remove explicit
     ubuntu1804_java11 tests.
   + ae102fbde3c1ff87e4f67007a275fb30792a4e8d:
     Fix wrong name of ubuntu1804_javabase9 task.
   + 0020a97fdc20ca099ec6386771b20d3236f9890d:
     Remove @executable_path/Frameworks from rpaths
   + 130f86ded1ce84f959f0b78c065211902faed546:
     Download stderr/stdout to a temporary FileOutErr
   + 2ab38663592a9ccbcc1f6f75ed135ae315f4d9d9:
     Release 0.25.0 (2019-05-01)
   + ed48a4a5fddbd93b057c3aa726e15720d79dcf8f:
     Add implementation to removed methods to address
     https://github.com/bazelbuild/bazel/issues/8226
   + 81aefe7ee01cc73646a53f9c72ed40ead09f9f5a:
     Remove unsupported cpu attribute from cc_toolchains.
   + cccced1e7fc7eaf4ba8f2c9d73dbac72b2686df9:
     Release 0.25.1 (2019-05-07)
   + 0900660d67b53a56a13d1fa16a788e4cecbb1c0e:
     Use package identifier instead of package name
   + 85a5a2bd569a5274950fc7327a044c395248c024:
     Configure @androidsdk//:emulator_x86 and :emulator_arm to point
     to the unified emulator binary
   + 6549ac5bba3eef2b1813b5a75757437383bf38f0:
     Release 0.25.2 (2019-05-10)
   + 0ff19c6d0adf3c0df94fff59ca3bd13cbcf99897:
     Fix StandaloneTestStrategy.appendStderr
```

Incompatible changes:

  - Flip --incompatible_windows_escape_jvm_flags to true. See
    https://github.com/bazelbuild/bazel/issues/7486

This release contains contributions from many people at Google, as well as George Gensure, Keith Smiley, Robert Sayre.

## Release 0.25.2 (2019-05-10)

```
Baseline: 03662462941953dad23af88693804b8b1d4430b9

Cherry picks:

   + 3f7f255f9a8345b8898e3953e7e53d68106cc63d:
     Windows: fix native test wrapper's arg. escaping
   + afeb8d0b7fef619159fc8fbaaeb8bd41dd2619bd:
     Flip --incompatible_windows_escape_jvm_flags
   + 4299b6549cbc1b3e4494c91ed2f51d49b14c7980:
     Sort DirectoryNode children to ensure validity.
   + 231270c67d5aa771462245531fa9b2ee7d3d0ae8:
     Conditionally use deprecated signature for initWithContentsOfURL
   + 75a3a531b08e727ade4fa3cb0a574bd142727cce:
     Add http_archive entries for testing with various JDK versions.
   + 4a6354a3a5ca23583f8b62e3e439a04ce75b863f:
     Now that ubuntu1804 uses JDK 11, remove explicit
     ubuntu1804_java11 tests.
   + ae102fbde3c1ff87e4f67007a275fb30792a4e8d:
     Fix wrong name of ubuntu1804_javabase9 task.
   + 0020a97fdc20ca099ec6386771b20d3236f9890d:
     Remove @executable_path/Frameworks from rpaths
   + 130f86ded1ce84f959f0b78c065211902faed546:
     Download stderr/stdout to a temporary FileOutErr
   + 2ab38663592a9ccbcc1f6f75ed135ae315f4d9d9:
     Release 0.25.0 (2019-05-01)
   + ed48a4a5fddbd93b057c3aa726e15720d79dcf8f:
     Add implementation to removed methods to address
     https://github.com/bazelbuild/bazel/issues/8226
   + 81aefe7ee01cc73646a53f9c72ed40ead09f9f5a:
     Remove unsupported cpu attribute from cc_toolchains.
   + cccced1e7fc7eaf4ba8f2c9d73dbac72b2686df9:
     Release 0.25.1 (2019-05-07)
   + 0900660d67b53a56a13d1fa16a788e4cecbb1c0e:
     Use package identifier instead of package name
   + 85a5a2bd569a5274950fc7327a044c395248c024:
     Configure @androidsdk//:emulator_x86 and :emulator_arm to point
     to the unified emulator binary
```

* Add fix for https://github.com/bazelbuild/bazel/issues/8254
* Add fix for https://github.com/bazelbuild/bazel/issues/8280

## Release 0.25.1 (2019-05-07)

```
Baseline: 03662462941953dad23af88693804b8b1d4430b9

Cherry picks:

   + 3f7f255f9a8345b8898e3953e7e53d68106cc63d:
     Windows: fix native test wrapper's arg. escaping
   + afeb8d0b7fef619159fc8fbaaeb8bd41dd2619bd:
     Flip --incompatible_windows_escape_jvm_flags
   + 4299b6549cbc1b3e4494c91ed2f51d49b14c7980:
     Sort DirectoryNode children to ensure validity.
   + 231270c67d5aa771462245531fa9b2ee7d3d0ae8:
     Conditionally use deprecated signature for initWithContentsOfURL
   + 75a3a531b08e727ade4fa3cb0a574bd142727cce:
     Add http_archive entries for testing with various JDK versions.
   + 4a6354a3a5ca23583f8b62e3e439a04ce75b863f:
     Now that ubuntu1804 uses JDK 11, remove explicit
     ubuntu1804_java11 tests.
   + ae102fbde3c1ff87e4f67007a275fb30792a4e8d:
     Fix wrong name of ubuntu1804_javabase9 task.
   + 0020a97fdc20ca099ec6386771b20d3236f9890d:
     Remove @executable_path/Frameworks from rpaths
   + 130f86ded1ce84f959f0b78c065211902faed546:
     Download stderr/stdout to a temporary FileOutErr
   + 2ab38663592a9ccbcc1f6f75ed135ae315f4d9d9:
     Release 0.25.0 (2019-05-01)
   + ed48a4a5fddbd93b057c3aa726e15720d79dcf8f:
     Add implementation to removed methods to address
     https://github.com/bazelbuild/bazel/issues/8226
   + 81aefe7ee01cc73646a53f9c72ed40ead09f9f5a:
     Remove unsupported cpu attribute from cc_toolchains.
```

Adding a commit which reintroduces the implementation for cc_common.compile and cc_common.link https://github.com/bazelbuild/bazel/issues/8226

## Release 0.25.0 (2019-05-01)

```
Baseline: 03662462941953dad23af88693804b8b1d4430b9

Cherry picks:

   + 3f7f255f9a8345b8898e3953e7e53d68106cc63d:
     Windows: fix native test wrapper's arg. escaping
   + afeb8d0b7fef619159fc8fbaaeb8bd41dd2619bd:
     Flip --incompatible_windows_escape_jvm_flags
   + 4299b6549cbc1b3e4494c91ed2f51d49b14c7980:
     Sort DirectoryNode children to ensure validity.
   + 231270c67d5aa771462245531fa9b2ee7d3d0ae8:
     Conditionally use deprecated signature for initWithContentsOfURL
   + 75a3a531b08e727ade4fa3cb0a574bd142727cce:
     Add http_archive entries for testing with various JDK versions.
   + 4a6354a3a5ca23583f8b62e3e439a04ce75b863f:
     Now that ubuntu1804 uses JDK 11, remove explicit
     ubuntu1804_java11 tests.
   + ae102fbde3c1ff87e4f67007a275fb30792a4e8d:
     Fix wrong name of ubuntu1804_javabase9 task.
   + 0020a97fdc20ca099ec6386771b20d3236f9890d:
     Remove @executable_path/Frameworks from rpaths
   + 130f86ded1ce84f959f0b78c065211902faed546:
     Download stderr/stdout to a temporary FileOutErr
```

Incompatible changes:

  - (Starlark rules) The legacy "py" provider can no longer be passed
    to or produced by native Python rules; use
    [PyInfo](https://docs.bazel.build/versions/master/skylark/lib/PyIn
    fo.html) instead. See
    [#7298](https://github.com/bazelbuild/bazel/issues/7298) for more
    information.
  - (Python rules) The `default_python_version` attribute of the
    `py_binary` and `py_test` rules has been renamed to
    `python_version`. Also, the `--force_python` flag has been
    renamed to `--python_version`. See
    [#7308](https://github.com/bazelbuild/bazel/issues/7308) for more
    information.
  - (Python rules) The python version now changes to whatever version
    is specified in a `py_binary` or `py_test`'s `python_version`
    attribute, instead of being forced to the value set by a command
    line flag. You can temporarily revert this change with
    `--incompatible_allow_python_version_transitions=false`. See
    [#7307](https://github.com/bazelbuild/bazel/issues/7307) for more
    information.
  - --incompatible_disable_third_party_license_checking` is enabled
    by default
  - Introduced --incompatible_use_python_toolchains, which supersedes
    --python_top/--python_path. See #7899 and #7375 for more
    information.
  - Python 3 is now the default Python version (for `py_binary` and
    `py_test` targets that don't specify the `python_version`
    attribute). Targets that are built for Python 3 will no longer
    have their output put in a separate `-py3` directory; instead
    there is now a separate `-py2` directory for Python 2 targets.
    See #7359 and #7593 for more information.
  - objc_library resource attributes are now disabled by default.
    Please migrate them to data instead. See
    https://github.com/bazelbuild/bazel/issues/7594 for more info.
  - Flip --incompatible_windows_escape_jvm_flags to true. See
    https://github.com/bazelbuild/bazel/issues/7486

New features:

  - genrules now support a $(RULEDIR) variable that resolves to the
    directory where the outputs of the rule are put.
  - Added --incompatible_windows_native_test_wrapper flag: enables
    using the Bash-less test wrapper on Windows. (No-op on other
    platforms.)

Important changes:

  - incompatible_use_jdk11_as_host_javabase: makes JDK 11 the default
    --host_javabase for remote jdk
    (https://github.com/bazelbuild/bazel/issues/7219)
  - Makes genquery somepath output deterministic.
  - Tristate attributes of native rules now reject True/False (use
    1/0)
  - Rollback of "Tristate attributes of native rules now reject
    True/False (use 1/0)"
  - Tristate attributes of native rules now reject True/False (use
    1/0)
  - Added -incompatible_do_not_split_linking_cmdline flag. See #7670
  - Tristate attributes of native rules now temporarily accept
    True/False again
  - `--incompatible_disable_legacy_crosstool_fields` has been flipped
    (https://github.com/bazelbuild/bazel/issues/6861)
    `--incompatible_disable_expand_if_all_available_in_flag_set` has
    been flipped (https://github.com/bazelbuild/bazel/issues/7008)
  - `--incompatible_disable_legacy_crosstool_fields` has been flipped
    (https://github.com/bazelbuild/bazel/issues/6861)
    `--incompatible_disable_expand_if_all_available_in_flag_set...
    RELNOTES: None.
  - --incompatible_no_transitive_loads is enabled by default.
  - Makes TreeArtifact deterministic.
  - --incompatible_no_transitive_loads is enabled by default.
  - Android NDK C++ toolchain is now configured in Starlark. This
    should be a backwards compatible change, but in case of bugs
    blame unknown commit.
  - `--incompatible_disable_legacy_crosstool_fields` has been flipped
    (https://github.com/bazelbuild/bazel/issues/6861)
    `--incompatible_disable_expand_if_all_available_in_flag_set` has
    been flipped (https://github.com/bazelbuild/bazel/issues/7008)
  - --incompatible_no_transitive_loads is enabled by default.
  - --incompatible_bzl_disallow_load_after_statement is enabled
  - Added `--incompatible_require_ctx_in_configure_features`, see
    https://github.com/bazelbuild/bazel/issues/7793 for details.
  - Flag --incompatible_merge_genfiles_directory is flipped. This
    removes the directory `bazel-genfiles` in favor of `bazel-bin`.
  - previously deprecated flag --experimental_remote_spawn_cache was
    removed
  - `--incompatible_disallow_load_labels_to_cross_package_boundaries`
    is enabled by default
  - Fix an issue where the Android resource processor did not surface
    errors from aapt2 compile and link actions.
  - --incompatible_no_attr_license is enabled by default
  - `--incompatible_disable_crosstool_file` has been flipped
    (https://github.com/bazelbuild/bazel/issues/7320)
  - A new flag `--incompatible_string_join_requires_strings` is
    introduced. The sequence argument of `string.join` must contain
    only string elements.
  - --incompatible_symlinked_sandbox_expands_tree_artifacts_in_runfile
    s_tree has been flipped
  - Incompatible flag `--incompatible_disable_legacy_cc_provider` has
    been flipped (see https://github.com/bazelbuild/bazel/issues/7036
    for details).
  - Don't drop the analysis cache when the same --define flag is set
    multiple times and the last value is the same (e.g. if the
    current invocation was run with "--define foo=bar" and the
    previous one was run with "--define foo=baz --define foo=bar").
  - The --incompatible_disable_genrule_cc_toolchain_dependency flag
    has been flipped (see
    https://github.com/bazelbuild/bazel/issues/6867 for details).
  - Incompatible change
    `--incompatible_remove_cpu_and_compiler_attributes_from_cc_toolcha
    in` has been flipped (see
    https://github.com/bazelbuild/bazel/issues/7075 for details).
  - --noexperimental_java_coverage is a no-op flag.
  - --experimental_java_coverage/--incompatible_java_coverage flag was
    removed. See #7425.
  - incompatible_use_toolchain_providers_in_java_common: pass
    JavaToolchainInfo and JavaRuntimeInfo providers to java_common
    APIs instead of configured targets
    (https://github.com/bazelbuild/bazel/issues/7186.)
  - --incompatible_remote_symlinks has been flipped. The remote
    caching and execution protocol will now represent symlinks in
    outputs as such. See
    https://github.com/bazelbuild/bazel/issues/7917 for more details.
  - Bazel is now ~20MiB smaller, from unbundling the Android rules'
    runtime dependencies.

This release contains contributions from many people at Google, as well as Andreas Herrmann, Andrew Suffield, Andy Scott, Benjamin Peterson, Ed Baunton, George Gensure, Ian McGinnis, Ity Kaul, Jingwen Chen, John Millikin, Keith Smiley, Marwan Tammam, Mike Fourie, Oscar Bonilla, perwestling, petros, Robert Sayre, Ryan Beasley, silvergasp, Stanimir Mladenov, Travis Cline, Vladimir Chebotarev, ??.

## Release 0.24.1 (2019-04-02)

```
Baseline: 235e76b0e756d05599a6cbe1663ff8e13df84a86

Cherry picks:

   + badd82e4c5cda7b3232481e1c63a5550ac898cd8:
     Automated rollback of commit
     1b4c37c38804559b5c1ade6f9c93501875e231b0.
   + 33e571939085dd158422e1b3503cfc738e0a3165:
     Fix the Python version select() mechanism to handle
     PY3-as-default
   + 56366ee3a73e2c92b2fa36a9840478202b9618ca:
     Set non-empty values for msvc_env_* when VC not installed
   + 22b3fbf4800113df51d603d943bd9eb9517ef904:
     Windows, test wrapper: fix broken integration test
   + f14d447cb56aee563f6e686b8f5b086a3bb55d47:
     Add whitelist file for starlark transitions
   + d99bc478db1f3414b4f6cd3dc14ca70aacf6b375:
     Update BUILD
   + 3529ad7ccf0c26dfb20a9d67b9d96de15f309f8b:
     Rename tools/function_transition_whitelist/BUILD to
     tools/whitelists/function_transition_whitelist/BUILD
   + de0612ad3ef7cc8c44069261befdeb0d15b97c10:
     Update bazel_toolchains to latest release and add toolchain
     config target for BuildKite CI (rbe_ubuntu1604)
   + 3e660ad178926648e8e10e2ee7a1a30b12f9b3d1:
     Automated rollback of commit
     087734009801242b83655efb863b2d5a761ae3dc.
   + 314cf1f9e4b332955c4800b2451db4e926c3e092:
     Pass -undefined dynamic_lookup to dynamic library linking
     actions on Mac
   + fc586a86b614667a21e5a01aea3544ac0338de78:
     Move cc_flags target into @bazel_tools//tools/cpp.
   + ea1703b30f9ac43a0c3262f5729c34658ed8d473:
     C++: Fix crash reported in #7721
   + 803801d1494f06f0ce977a1f2241ef6a4d85df09:
     Pass execution info to xml generating spawn. Fixes #7794
   + f092ec388a4b58788285b928c86f0f81561d4be4:
     Release 0.24.0 (2019-03-26)
   + c56c489119e6587975964c44ceb9e429ad950736:
     Wrap StatusRuntimeExceptions from GrpcRemoteCache
```

Baseline: f092ec388a4b58788285b928c86f0f81561d4be4

Cherrypicks:
+ c56c489119e6587975964c44ceb9e429ad950736: Wrap StatusRuntimeExceptions from GrpcRemoteCache

## Release 0.24.0 (2019-03-26)

```
Baseline: 235e76b0e756d05599a6cbe1663ff8e13df84a86

Cherry picks:

   + badd82e4c5cda7b3232481e1c63a5550ac898cd8:
     Automated rollback of commit
     1b4c37c38804559b5c1ade6f9c93501875e231b0.
   + 33e571939085dd158422e1b3503cfc738e0a3165:
     Fix the Python version select() mechanism to handle
     PY3-as-default
   + 56366ee3a73e2c92b2fa36a9840478202b9618ca:
     Set non-empty values for msvc_env_* when VC not installed
   + 22b3fbf4800113df51d603d943bd9eb9517ef904:
     Windows, test wrapper: fix broken integration test
   + f14d447cb56aee563f6e686b8f5b086a3bb55d47:
     Add whitelist file for starlark transitions
   + d99bc478db1f3414b4f6cd3dc14ca70aacf6b375:
     Update BUILD
   + 3529ad7ccf0c26dfb20a9d67b9d96de15f309f8b:
     Rename tools/function_transition_whitelist/BUILD to
     tools/whitelists/function_transition_whitelist/BUILD
   + de0612ad3ef7cc8c44069261befdeb0d15b97c10:
     Update bazel_toolchains to latest release and add toolchain
     config target for BuildKite CI (rbe_ubuntu1604)
   + 3e660ad178926648e8e10e2ee7a1a30b12f9b3d1:
     Automated rollback of commit
     087734009801242b83655efb863b2d5a761ae3dc.
   + 314cf1f9e4b332955c4800b2451db4e926c3e092:
     Pass -undefined dynamic_lookup to dynamic library linking
     actions on Mac
   + fc586a86b614667a21e5a01aea3544ac0338de78:
     Move cc_flags target into @bazel_tools//tools/cpp.
   + ea1703b30f9ac43a0c3262f5729c34658ed8d473:
     C++: Fix crash reported in #7721
   + 803801d1494f06f0ce977a1f2241ef6a4d85df09:
     Pass execution info to xml generating spawn. Fixes #7794
```

Incompatible changes:

  - Added --incompatible_py3_is_default to test switching the default
    Python version to PY3 for py_binary/py_test targets that do not
    specify a version. See #7359.
  - //tools/cmd_line_differ has been renamed to //tools/aquery_differ
    & can now compare (in addition to command lines) inputs of
    actions given 2 aquery results.
  - java_(mutable_|)proto_library: removed strict_deps attribute.
  - The flag --incompatible_list_based_execution_strategy_selection
    was added and is used to ease the migration to the new style of
    specifying
    execution strategy selection and fallback behavior. The
    documentation for
    this flag is here: https://github.com/bazelbuild/bazel/issues/7480
  - Added --incompatible_py2_outputs_are_suffixed, for switching the
    bazel-bin symlink to point to Python 3 outputs instead of Python
    2 outputs. See
    [#7593](https://github.com/bazelbuild/bazel/issues/7593).

New features:

  - Make actions.args() object chainable.
  - Added --incompatible_windows_style_arg_escaping flag: enables
    correct subprocess argument escaping on Windows. (No-op on other
    platforms.)
  - Added --incompatible_windows_escape_jvm_flags flag: enables
    correct java_binary.jvm_flags and java_test.jvm_flags
    tokenization and escaping on Windows. (No-op on other platforms.)

Important changes:

  - Allow running aquery against the current state of Skyframe
  - Added support for the "navigation" resource directory to Android
    resource processing in Bazel. This is used by the Navigation
    Architecture Component.
  - --incompatible_disable_runtimes_filegroups was flipped
    (https://github.com/bazelbuild/bazel/issues/6942)
  - Incompatible flag `--incompatible_linkopts_in_user_link_flags`
    has been flipped (https://github.com/bazelbuild/bazel/issues/6826)
  - Incompatible flag `--incompatible_dont_emit_static_libgcc` has
    been flipped (https://github.com/bazelbuild/bazel/issues/6825)
  - --incompatible_disallow_filetype is enabled by default.
  - Fixed issue where exceptions and stacktraces were silently
    swallowed in the Android resource processing pipeline.
  - `--incompatible_disable_expand_if_all_available_in_flag_set` has
    been flipped (https://github.com/bazelbuild/bazel/issues/7008)
  - --incompatible_disallow_dict_plus is enabled by default
  - Adds --incompatible_disable_objc_library_resources to disable
    resource attributes in objc_library. Please migrate these
    attributes to `data` instead.
  - --incompatible_disallow_old_style_args_add is enabled by default.
  - Using the `native` module in BUILD files is deprecated. It will
    be forbidden with --incompatible_disallow_native_in_build_file.
  - (Python rules) PyRuntimeInfo is exposed to Starlark, making it
    possible for Starlark rules to depend on or imitate `py_runtime`.
    The `files` attribute of `py_runtime` is no longer mandatory.
  - incompatible_use_toolchain_providers_in_java_common: pass
    JavaToolchainInfo and JavaRuntimeInfo providers to java_common
    APIs instead of configured
    targetshttps://github.com/bazelbuild/bazel/issues/7186.
  - is_using_fission crosstool variable is now exposed in all compile
    actions when fission is active (it used to be exposed only for
    linking actions).
  - incompatible_use_toolchain_providers_in_java_common: pass
    JavaToolchainInfo and JavaRuntimeInfo providers to java_common
    APIs instead of configured
    targetshttps://github.com/bazelbuild/bazel/issues/7186.
  - `py_runtime` gains a `python_version` attribute for specifying
    whether it represents a Python 2 or 3 interpreter.
  - `--incompatible_java_coverage` is enabled by default.
  - Starlark rules can safely declare attributes named "licenses"
  - When using
    --incompatible_list_based_execution_strategy_selection, Bazel
    will use remote execution by default (if you specify
    --remote_executor), otherwise persistent workers (if the action
    supports it), otherwise sandboxed local execution (if the action
    and platform supports it) and at last unsandboxed local
    execution. The flags --spawn_strategy and --strategy continue to
    work as before - this only sets new defaults for the case where
    you don't specify these flags.
  - Set default value of --incompatible_remap_main_repo to true.
  - Set default value of --incompatible_remap_main_repo to true.

This release contains contributions from many people at Google, as well as Andrew Suffield, Brandon Lico, Chris Eason, Clint Harrison, Ed Schouten, Garrett Hopper, George Gensure, Greg, John Millikin, Julie, Keith Smiley, Laurent Le Brun, Ryan Beasley, Shmuel H, Travis Cline, Vladimir Chebotarev.

## Release 0.23.0 (2019-02-26)

```
Baseline: 441fd75d0047f8a998d784c557736ab9075db893

Cherry picks:

   + 6ca7763669728253606578a56a205bca3ea883e9:
     Fix a typo
   + 2310b1c2c8b2f32db238f667747e7d5672480f4a:
     Ignore SIGCHLD in test setup script
   + f9eb1b56706f91063e9d080b850fa56964e77324:
     Complete channel initialization in the event loop
```

Incompatible changes:

  - //src:bazel uses the minimal embedded JDK, if you want to
    avoid the extra steps of minimizing the JDK, use //src:bazel-dev
    instead.
  - //src:bazel uses the minimal embedded JDK, if you want to
    avoid the extra steps of building bazel with the minimized JDK,
    use //src:bazel-dev
    instead.
  - The default value of --host_platform and --platforms will be
      changed to not be dependent on the configuration. This means
    that setting
      --cpu or --host_cpu will not affect the target or host platform.
  - Toolchain resolution for cc rules is now enabled via an
    incompatible flag, --incompatible_enable_cc_toolchain_resolution.
    The previous
    flag, --enabled_toolchain_types, is deprecated and will be
    removed.
  - java_(mutable_|)proto_library: removed strict_deps attribute.
  - Python rules will soon reject the legacy "py" struct provider
    (preview by enabling --incompatible_disallow_legacy_py_provider).
    Upgrade rules to use PyInfo instead. See
    [#7298](https://github.com/bazelbuild/bazel/issues/7298).
  - java_(mutable_|)proto_library: removed strict_deps attribute.
  - Two changes to native Python rules: 1) `default_python_version`
    and `--force_python` are deprecated; use `python_version` and
    `--python_version` respectively instead. You can preview the
    removal of the deprecated names with
    --incompatible_remove_old_python_version_api. See
    [#7308](https://github.com/bazelbuild/bazel/issues/7308). 2) The
    version flag will no longer override the declared version of a
    `py_binary` or `py_test` target. You can preview this new
    behavior with --incompatible_allow_python_version_transitions.
    See [#7307](https://github.com/bazelbuild/bazel/issues/7307).

Important changes:

  - There is a new flag available
    `--experimental_java_common_create_provider_enabled_packages`
    that acts as a whitelist for usages of
    `java_common.create_provider`. The constructor will be deprecated
    in Bazel 0.23.
  - [#7024] Allow chaining of the same function type in aquery.
  - Introduces --local_{ram,cpu}_resources, which will take the place
    of --local_resources.
  - [#6930] Add documentation for the aquery command.
  - Incompatible flag `--incompatible_dont_emit_static_libgcc` has
    been flipped (https://github.com/bazelbuild/bazel/issues/6825)
  - Incompatible flag `--incompatible_linkopts_in_user_link_flags`
    has been flipped (https://github.com/bazelbuild/bazel/issues/6826)
  - Flag --incompatible_range_type is removed.
  - Flag --incompatible_disallow_slash_operator is removed.
  - Flag --incompatible_disallow_conflicting_providers is removed.
  - `--incompatible_disallow_data_transition` is now enabled by
    default
  - Allow inclusion of param files in aquery output
  - [#6985] Add test to verify aquery's behavior for Cpp action
    templates.
  - --incompatible_require_feature_configuration_for_pic was flipped
    (https://github.com/bazelbuild/bazel/issues/7007).
  - Also ignore module-info.class in multi-version Jars
  - objc_framework has been deleted. Please refer to
    apple_dynamic_framework_import and apple_static_framework_import
    rules available in
    [rules_apple](https://github.com/bazelbuild/rules_apple/blob/maste
    r/doc/rules-general.md)
  - --test_sharding_strategy=experimental_heuristic is no more
  - objc_bundle_library has been removed. Please migrate to
    rules_apple's
    [apple_resource_bundle](https://github.com/bazelbuild/rules_apple/
    blob/master/doc/rules-resources.md#apple_resource_bundle).
  - You can now use the attribute `aapt_version` or the flag
    `--android_aapt` to pick the aapt version for android_local_test
    tests
  - In --keep_going mode, Bazel now correctly returns a non-zero exit
    code when encountering a package loading error during target
    pattern parsing of patterns like "//foo:all" and "//foo/...".
  - The default value for --incompatible_strict_action_env has been
    flipped to 'false' again, as we discovered breakages for local
    execution users. We'll need some more time to figure out the best
    way to make this work for local and remote execution. Follow
    https://github.com/bazelbuild/bazel/issues/7026 for more details.
  - Locally-executed spawns tagged "no-cache" no longer upload their
    outputs to the remote cache.
  - Introduces --host_compiler flag to allow setting a compiler for
    host compilation when --host_crosstool_top is specified.
  - --incompatible_expand_directories is enabled by default
  - [aquery] Handle the case of aspect-on-aspect.
  - Fixed a longstanding bug in the http remote cache where the value
    passed to
    --remote_timeout would be interpreted as milliseconds instead of
    seconds.
  - Enable --incompatible_use_jdk10_as_host_javabase by default, see
    https://github.com/bazelbuild/bazel/issues/6661
  - Add --incompatible_use_jdk11_as_host_javabase: makes JDK 11 the
    default --host_javabase for remote jdk
    (https://github.com/bazelbuild/bazel/issues/7219)
  - Highlight TreeArtifact in aquery text output.
  - Locally-executed spawns tagged "no-cache" no longer upload their
    outputs to the remote cache.
  - java_common APIs now accept JavaToolchainInfo and JavaRuntimeInfo
    instead of configured targets for java_toolchain and java_runtime
  - cc_common.create_cc_toolchain_config_info is stable and available
    for production use
  - incompatible_use_toolchain_providers_in_java_common: pass
    JavaToolchainInfo and JavaRuntimeInfo providers to java_common
    APIs instead of configured targets
    (https://github.com/bazelbuild/bazel/issues/7186)
  - --incompatible_strict_argument_ordering is enabled by default.
  - Bazel now supports reading cache hits from a repository cache,
    even if it doesn't have write access to the cache.
  - Adding arm64e to OSX CROSSTOOL.
  - Ignore package-level licenses on config_setting.
  - Add an optional output_source_jar parameter to java_common.compile
  - --incompatible_disable_objc_provider_resources is now enabled by
    default. This disables ObjcProvider's fields related to resource
    processing.
  - Explicitly set https.protocols and exclude TLSv1.3.
  - Bazel now validates that JAVA_HOME points to a valid JDK and
    falls back to auto-detection by looking up the path of `javac`.
  - Upgrade the embedded JDK version to 11.0.2.
  - Added --incompatible_disable_crosstool_file
    (https://github.com/bazelbuild/bazel/issues/7320)
  - --incompatible_disable_objc_provider_resources is now enabled by
    default. This disables ObjcProvider's fields related to resource
    processing.
  - --incompatible_disable_tools_defaults_package has been flipped.
  - For tests that do not generate a test.xml, Bazel now uses a
    separate action to generate one; this results in minor
    differences in the generated test.xml, and makes the generation
    more reliable overall.
  - incompatible_generate_javacommon_source_jar: java_common.compile
    now always generates a source jar, see
    https://github.com/bazelbuild/bazel/issues/5824.
  - New incompatible flag
    --incompatible_disallow_struct_provider_syntax removes the
    ability for rule implementation functions to return struct. Such
    functions should return a list of providers instead. Migration
    tracking: https://github.com/bazelbuild/bazel/issues/7347

This release contains contributions from many people at Google, as well as Benjamin Peterson, Ed Schouten, erenon, George Gensure, Greg Estren, Igal Tabachnik, Ittai Zeidman, Jannis Andrija Schnitzer, John Millikin, Keith Smiley, Kelly Campbell, Max Vorobev, nicolov, Robin Nabel.

## Release 0.22.0 (2019-01-28)

```
Baseline: deb028e3fb30b4e2953df16f35ab1f55a08ea8fa

Cherry picks:

   + a3a5975dca3ad04c19dc7d063fcf490a8cd612fd:
     Fix a race condition in remote cache
   + b8d0e1b05c225a4b943ce498194d069d18093d9a:
     Use a new GitHub token and KMS key for the release process.
   + 3759e3895503aa2bbd6943c5b568b8c050b9448f:
     remote: fix unexpected IO error (not a directory)
   + 4473bb1a9ec4282aa8497b86580d68e82415df4a:
     Fix a race condition in Bazel's Windows process management.
   + 9137fb940886aa516f32ca8a36feccedb545c99b:
     undo flag flip of --incompatible_strict_action_env
   + 12ab12e80ad1c9a3510aa4bbfdf3fddafc0bca00:
     Revert "Enabling Bazel to generate input symlinks as defined by
     RE AP?
   + 6345c747d8cb1819e70c853becadbf8a989decf1:
     Automated rollback of commit
     30536baa4a410d8c0a7adab5cd58cd8a2ac7e46c.
```



The Bazel team is happy to announce a new release of Bazel,
[Bazel 0.22.0](https://github.com/bazelbuild/bazel/releases/tag/0.22.0).

Baseline: deb028e3fb30b4e2953df16f35ab1f55a08ea8fa

### Breaking changes

- [`--incompatible_string_is_not_iterable`](https://github.com/bazelbuild/bazel/issues/5830)

### Upcoming changes

This release is a [migration window for the following changes](https://github.com/bazelbuild/bazel/labels/migration-0.22).

- [`--incompatible_disallow_data_transition`](https://github.com/bazelbuild/bazel/issues/6153)
- [`--incompatible_dont_emit_static_libgcc`](https://github.com/bazelbuild/bazel/issues/6825)
- [`--incompatible_linkopts_in_user_link_flags`](https://github.com/bazelbuild/bazel/issues/6826)
- [`--incompatible_disable_legacy_crosstool_fields`](https://github.com/bazelbuild/bazel/issues/6861)
- [`--incompatible_use_aapt2_by_default`](https://github.com/bazelbuild/bazel/issues/6907)
- [`--incompatible_disable_runtimes_filegroups`](https://github.com/bazelbuild/bazel/issues/6942)
- [`--incompatible_disable_legacy_cc_provider`](https://github.com/bazelbuild/bazel/issues/7036)
- [`--incompatible_require_feature_configuration_for_pic`](https://github.com/bazelbuild/bazel/issues/7007)
- [`--incompatible_disable_expand_if_all_available_in_flag_set`](https://github.com/bazelbuild/bazel/issues/7008)
- [`--incompatible_disable_legacy_proto_provider`](https://github.com/bazelbuild/bazel/issues/7152)
- [`--incompatible_disable_proto_source_root`](https://github.com/bazelbuild/bazel/issues/7153)

### General Changes

- https://docs.bazel.build now supports versioned
  documentation. Use the selector at the top of the navigation bar
  to switch between documentation for different Bazel releases.

- set `projectId` in all `PublishBuildToolEventStreamRequest`

### Android

- mobile-install now works with aapt2. Try it out with `bazel
  mobile-install --android_aapt=aapt2 //my:target`

- Fixed issues with mobile-install v1 when deploying to Android 9 Pie
  devices. https://github.com/bazelbuild/bazel/issues/6814

- Fixed issue where error messages from Android manifest merging
  actions were not fully propagated.

- New incompatible change flag `--incompatible_use_aapt2_by_default`
  for defaulting to aapt2 in Android builds has been added. To build with
  aapt2 today, pass the flag
  `--incompatible_use_aapt2_by_default=true` or
  `--android_aapt=aapt2`, or set the `aapt_version`  to `aapt2` on
  your `android_binary` or `android_local_test` target.

- Fixed mobile-install v1 error when installing an app with native
  libraries onto an Android 9 (Pie) device. See
  https://github.com/bazelbuild/examples/issues/77

- Fixed a mobile-install bug where `arm64-v8a` libraries were not
  deployed correctly on `arm64` devices. This was done by enabling
  incremental native lib deployment by default. A previously
  undocumented `--android_incremental_native_libs` flag is removed,
  and is now the regular behavior. See
  https://github.com/bazelbuild/bazel/issues/2239

### Apple

- The `objc_bundle` rule has been removed. Please migrate to rules_apple's
  [apple_bundle_import](https://github.com/bazelbuild/rules_apple/bl
  ob/master/doc/rules-resources.md#apple_bundle_import).

- The `apple_stub_binary` rule has been deleted.

- The `--xbinary_fdo` option that passes xbinary profiles has been added.

### C++

- `cc_toolchain.(static|dynamic)_runtime_libs` attributes are now optional

### Packaging

- `build_tar.py` in `tools/build_defs/pkg` now supports a JSON manifest
  that can be used to add paths that have symbols that can't be
  specified via the command line

### Query

- Filtering of inputs, outputs, and mnemonic filtering have been added to
  aquery.

- The aquery and cquery query2 tests have been open-sourced.

- The Bazel query how-to recommends ":*" instead of ":all", because "all" might
  be the name of a target.

### Testing

- The `--runs_per_test` has been placed in the TESTING documentation category.

- A a clarifying message has been added to test case summary output when all
  test cases pass but the target fails.

### Contributors

This release contains contributions from many people at Google, as well as
Benjamin Peterson, Dave Lee, George Gensure, Gert van Dijk, Gustavo Storti
Salibi, Keith Smiley, Loo Rong Jie, Lukasz Tekieli, Mikhail Mazurskiy, Thi,
Travis Cline, Vladimir Chebotarev, and Yannic.

## Release 0.21.0 (2018-12-19)

```
Baseline: cb9b2afbba3f8d3a1db8bf68e65d06f1b36902f5

Cherry picks:

   + 12b96466ee0d6ab83f7d4cd24be110bb5021281d:
     Windows, test wrapper: rename the associated flag
   + 7fc967c4d6435de2bb4e34aac00ca2e499f55fca:
     Use a fixed thread pool in ByteStreamBuildEventArtifactUploader
   + 798b9a989aa793655d29504edb5fb85f3143db84:
     Add --build_event_upload_max_threads option
   + dbe05df23ccf4c919379e0294e0701fd3f66739c:
     Update the version of  skylib bundled in the distfile
```

Incompatible changes:

  - The --experimental_stl command line option is removed.
  - aquery defaults to human readable output format.

New features:

  - repository_ctx.download and repository_ctx.download_and_extract
    now return a struct.
  - Android Databinding v2 can be enabled with
    --experimental_android_databinding_v2.

Important changes:

  - The deprecated and unmaintained Docker rules in
    tools/build_defs/docker were removed. Please use
    https://github.com/bazelbuild/rules_docker instead.
  - The new --upload_query_output_using_bep query/cquery/aquery flag
    causes query outputs to be uploaded via BEP.
  - New incompatible flag --incompatible_strict_argument_ordering
  - --strict_android_deps and --strict_java_deps were renamed to
    --experimental_strict_java_deps
  - config_settings that select on "compiler" value instead of values
    = {"compiler" : "x"} should use flag_values =
    {"@bazel_tools//tools/cpp:compiler": "x"}.
  - The new --upload_query_output_using_bep query/cquery/aquery flag
    causes query outputs to be uploaded via BEP.
  - Turn on --incompatible_disable_sysroot_from_configuration
  - We revamped our Android with Bazel tutorial! Check it out
    [here](https://docs.bazel.build/versions/master/tutorial/android-a
    pp.html).
  - --incompatible_disallow_slash_operator is now on by default
  - Enable --experimental_check_desugar_deps by default.  This flag
    rules out several types of invalid Android builds at compile-time.
  - The --max_config_changes_to_show option lists the names of
    options which
    have changed and thus caused the analysis cache to be dropped.
  - The --experimental_strict_action_env option has been renamed to
    --incompatible_strict_action_env and is now on by default. This
    means Bazel will no longer use the client's PATH and
    LD_LIBRARY_PATH environmental variables in the default action
    environment. If the old behavior is desired, pass
    --action_env=PATH and --action_env=LD_LIBRARY_PATH.
    --noincompatible_strict_action_env will also temporarily restore
    the old behavior. However, as --action_env is a more general and
    explicit way to pass client environmental variables into actions,
    --noincompatible_strict_action_env will eventually be deprecated
    and removed. See #6648 for more details.
  - XCRUNWRAPPER_LABEL has been removed. If you used this value
    before, please use @bazel_tools//tools/objc:xcrunwrapper instead.
  - --incompatible_static_name_resolution is no unable by default
  - We will phase out --genrule_strategy in favor of
    --strategy=Genrule=<value> (for genrules) or
    --spawn_strategy=<value> (for all actions).
  - --incompatible_package_name_is_a_function is now enabled by
    default
  - Dynamic execution is now available with
    --experimental_spawn_strategy. Dynamic execution allows a build
    action to run locally and remotely simultaneously, and Bazel
    picks the fastest action. This provides the best of both worlds:
    faster clean builds than pure local builds, and faster
    incremental builds than pure remote builds.
  - --incompatible_package_name_is_a_function is now enabled by
    default
  - New incompatible flag --incompatible_merge_genfiles_directory
  - grpc log now logs updateActionResult
  - CppConfiguration doesn't do package loading anymore. That means:
    * it's no longer needed to have C++ toolchain available when
    building non-C++ projects
    * bazel will not analyze C++ toolchain when not needed -> speedup
    ~2s on bazel startup when C++ rules using hermetic toolchain are
    not loaded
  - --incompatible_package_name_is_a_fu...

This release contains contributions from many people at Google, as well as andy g scott ?, Attila Ol?h, Benjamin Peterson, Clint Harrison, Dave Lee, Ed Schouten, Greg Estren, Gregor Jasny, Jamie Snape, Jerry Marino, Loo Rong Jie, Or Shachar, Sevki Hasirci, William Chargin.

## Release 0.20.0 (2018-11-30)

```
Baseline: 7bf7f031c332dc483257248d1c1f98ad75bbc83b

Cherry picks:

   + fd52341505e725487c6bc6dfbe6b5e081aa037da:
     update bazel-toolchains pin to latest release Part of changes to
     allow bazelci to use 0.19.0 configs. RBE toolchain configs at or
     before 0.17.0 are not compatible with bazel 0.19.0 or above.
   + 241f28d05424db2d11ee245dc856b992258505e3:
     Revert "Toggle --incompatible_disable_late_bound_option_defaults
     flag."
   + f7e5aef145c33968f658eb2260e25630dc41cc67:
     Add cc_toolchain targets for the new entries in the default
     cc_toolchain_suite.
   + d2920e32ec7f3f8551a693d33c17b19f1b802145:
     Revert "WindowsFileSystem: open files with delete-sharing"
```

[Breaking changes in 0.20](https://github.com/bazelbuild/bazel/issues?q=is%3Aissue+label%3Abreaking-change-0.20)

  - [--incompatible_remove_native_http_archive](https://github.com/bazelbuild/bazel/issues/6570).
  - [--incompatible_remove_native_git_repository](https://github.com/bazelbuild/bazel/issues/6569).
  - [--incompatible_disable_cc_toolchain_label_from_crosstool_proto](https://github.com/bazelbuild/bazel/issues/6434).
  - [--incompatible_disable_depset_in_cc_user_flags](https://github.com/bazelbuild/bazel/issues/6384).
  - [--incompatible_disable_cc_configuration_make_variables](https://github.com/bazelbuild/bazel/issues/6381).
  - [--incompatible_disallow_conflicting_providers](https://github.com/bazelbuild/bazel/issues/5902).
  - [--incompatible_range_type](https://github.com/bazelbuild/bazel/issues/5264).

[0.20 is a migration window for the following changes](https://github.com/bazelbuild/bazel/issues?q=is%3Aissue+label%3Amigration-0.20)

  - [--incompatible_use_jdk10_as_host_javabase](https://github.com/bazelbuild/bazel/issues/6661)
  - [--incompatible_use_remotejdk_as_host_javabase](https://github.com/bazelbuild/bazel/issues/6656)
  - [--incompatible_disable_sysroot_from_configuration](https://github.com/bazelbuild/bazel/issues/6565)
  - [--incompatible_provide_cc_toolchain_info_from_cc_toolchain_suite](https://github.com/bazelbuild/bazel/issues/6537)
  - [--incompatible_disable_depset_in_cc_user_flags](https://github.com/bazelbuild/bazel/issues/6383)
  - [--incompatible_package_name_is_a_function](https://github.com/bazelbuild/bazel/issues/5827)

[Breaking changes in the next release (0.21)](https://github.com/bazelbuild/bazel/issues?q=is%3Aissue+label%3Abreaking-change-0.21)

  - [--incompatible_use_jdk10_as_host_javabase](https://github.com/bazelbuild/bazel/issues/6661)
  - [--incompatible_use_remotejdk_as_host_javabase](https://github.com/bazelbuild/bazel/issues/6656)
  - [--incompatible_disable_sysroot_from_configuration](https://github.com/bazelbuild/bazel/issues/6565)
  - [--incompatible_provide_cc_toolchain_info_from_cc_toolchain_suite](https://github.com/bazelbuild/bazel/issues/6537)
  - [--incompatible_disable_depset_in_cc_user_flags](https://github.com/bazelbuild/bazel/issues/6383)
  - [--incompatible_disallow_data_transition](https://github.com/bazelbuild/bazel/issues/6153)
  - [--incompatible_package_name_is_a_function](https://github.com/bazelbuild/bazel/issues/5827)
  - [--incompatible_disallow_slash_operator](https://github.com/bazelbuild/bazel/issues/5823)
  - [--incompatible_static_name_resolution](https://github.com/bazelbuild/bazel/issues/5637)

Incompatible changes:

  - the --experimental_no_dotd_scanning_with_modules command line
    argument is not supported anymore.
  - The --prune_cpp_modules command line option is not supported
    anymore.
  - the --experimental_prune_cpp_input_discovery command line option
    is not supported anymore.

New features:

  - Added support for Android NDK r18.

Important changes:

  - The 'default' parameter of attr.output and attr.output_list is
    removed. This is controlled by
    --incompatible_no_output_attr_default
  - A number of platform-related Starlark APIs which were previously
    marked "experimental" are now disabled by default, and may be
    enabled via --experimental_platforms_api
  - Make legacy-test-support ("legacy_test-<api-level>") from
    android_sdk_repository neverlink. The legacy test support
    libraries shouldn't be built into test binaries. To make them
    available at runtime, developers should declare them via
    uses-library:
    https://developer.android.com/training/testing/set-up-project#andr
    oid-test-base
  - query remote server Capabilities (per REAPI v2)
  - CppRules: All cc_toolchains depended on from
    cc_toolchain_suite.toolchains are now analyzed when not using
    platforms in order to select the right cc_toolchain.
  - removed obsolete --explicit_jre_deps flag.
  - Incompatible flag
    --incompatible_disable_legacy_cpp_toolchain_skylark_api was
    flipped.
  - Improve error messaging when unsupport proguard options are
    specified at the library level.
  - Incompatible flag
    --incompatible_disable_legacy_cpp_toolchain_skylark_api was
    flipped.
  - Incompatible flag
    --incompatible_disable_legacy_cpp_toolchain_skylark_api was
    flipped.
  - The --incompatible_disable_late_bound_option_defaults flag has
    been flipped (#6384)
  - Incompatible flag
    --incompatible_disable_legacy_flags_cc_toolchain_api was flipped
    (#6434)
  - Fixed issue where ctx.resolve_command created conflicting
    intermediate files when resolve_command was called multiple times
    within the same rule invocation with a long command attribute.
  - Incompatible flag
    --incompatible_disable_cc_configuration_make_variables was
    flipped (#6381)
  - If the --javabase flag is unset, it Bazel locates a JDK using
    the JAVA_HOME environment variable and searching the PATH. If no
    JDK is found --javabase will be empty, and builds targeting Java
    will not
    be supported. Previously Bazel would fall back to using the
    embedded
    JDK as a --javabase, but this is no longer default behaviour. A
    JDK should
    be explicitly installed instead to enable Java development
  - Bazel will now shut down when idle for 5 minutes and the system
    is low on RAM (linux only).
  - CROSSTOOL file is now read from the package of cc_toolchain, not
    from
    the package of cc_toolchain_suite. This is not expected to break
    anybody since
    cc_toolchain_suite and cc_toolchain are commonly in the same
    package.
  - All overrides of Starlark's ctx.new_file function are now
    deprecated.
      Try the `--incompatible_new_actions_api` flag to ensure your
    code is forward-compatible.
  - --incompatible_disable_cc_toolchain_label_from_crosstool_proto
    was flipped.
  - Introduce --(no)shutdown_on_low_sys_mem startup flag to toggle
    idle low-memory shutdown, disabled by default.
  - --incompatible_disable_cc_toolchain_label_from_crosstool_proto
    was flipped.
  - --incompatible_disable_cc_toolchain_label_from_crosstool_proto
    was flipped.
  - CppRules: All cc_toolchains depended on from
    cc_toolchain_suite.toolchains are now analyzed when not using
    platforms in order to select the right cc_toolchain.
  - The function `attr.license` is deprecated and will be removed.
      It can be disabled now with `--incompatible_no_attr_license`.
  - `range()` function now returns a lazy value
    (`--incompatible_range_type` is now set by default).
  - The code coverage report now includes the actual paths to header
    files instead of the ugly,
    Bazel generated, virtual includes path.
  - `--incompatible_disallow_conflicting_providers` has been switched
    to true
  - Add new flag `--incompatible_disable_systool_from_configration` to
    disable loading the systool from CppConfiguration.
  - Add new flag `--incompatible_disable_sysroot_from_configuration`
    to
    disable loading the systool from CppConfiguration.
  - Sorting remote Platform properties for remote execution. May
    affect cache keys!
  - Use different server log files per Bazel server process; java.log
    is
    now a symlink to the latest log.

This release contains contributions from many people at Google, as well as a7g4 <a7g4@a7g4.net>, Alan <alan.agius@betssongroup.com>, Asaf Flescher <asafflesch@gmail.com>, Benjamin Peterson <bp@benjamin.pe>, Ed Schouten <ed.schouten@prodrive-technologies.com>, George Gensure <ggensure@uber.com>, George Kalpakas <kalpakas.g@gmail.com>, Greg <gregestren@users.noreply.github.com>, Irina Iancu <iirina@users.noreply.github.com>, Keith Smiley <keithbsmiley@gmail.com>, Loo Rong Jie <loorongjie@gmail.com>, Mark Zeren <mzeren@vmware.com>, Petros Eskinder <petroseskinder@users.noreply.github.com>, rachcatch <rachelcatchpoole@hotmail.com>, Robert Brown <robert.brown@gmail.com>, Robert Gay <robert.gay@redfin.com>, Salty Egg <2281521+zhouhao@users.noreply.github.com>.

## Release 0.19.2 (2018-11-19)

```
Baseline: ac880418885061d1039ad6b3d8c28949782e02d6

Cherry picks:

   + 9bc3b20053a8b99bf2c4a31323a7f96fabb9f1ec:
     Fix the "nojava" platform and enable full presubmit checks for
     the various JDK platforms now that we have enough GCE resources.
   + 54c2572a8cabaf2b29e58abe9f04327314caa6a0:
     Add openjdk_linux_archive java_toolchain for nojava platform.
   + 20bfdc67dc1fc32ffebbda7088ba49ee17e3e182:
     Automated rollback of commit
     19a401c38e30ebc0879925a5caedcbe43de0028f.
   + 914b4ce14624171a97ff8b41f9202058f10d15b2:
     Windows: Fix Precondition check for addDynamicInputLinkOptions
   + 83d406b7da32d1b1f6dd02eae2fe98582a4556fd:
     Windows, test-setup.sh: Setting RUNFILES_MANIFEST_FILE only when
     it exists.
   + e025726006236520f7e91e196b9e7f139e0af5f4:
     Update turbine
   + 5f312dd1678878fb7563eae0cd184f2270346352:
     Fix event id for action_completed BEP events
   + f0c844c77a2406518c4e75c49188390d5e281d3d:
     Release 0.19.0 (2018-10-29)
   + c3fb1db9e4e817e8a911f5b347b30f2674a82f7c:
     Do not use CROSSTOOL to select cc_toolchain
   + 8e280838e8896a6b5eb5421fda435b96b6f8de60:
     Windows Add tests for msys gcc toolchain and mingw gcc toolchain
   + fd52341505e725487c6bc6dfbe6b5e081aa037da:
     update bazel-toolchains pin to latest release Part of changes to
     allow bazelci to use 0.19.0 configs. RBE toolchain configs at or
     before 0.17.0 are not compatible with bazel 0.19.0 or above.
   + eb2af0f699350ad187048bf814a95af23f562c77:
     Release 0.19.1 (2018-11-12)
   + 6bc452874ddff69cbf7f66186238032283f1195f:
     Also update cc_toolchain.toolchain_identifier when
     CC_TOOLCHAIN_NAME is set
   + f7e5aef145c33968f658eb2260e25630dc41cc67:
     Add cc_toolchain targets for the new entries in the default
     cc_toolchain_suite.
   + 683c302129b66a8999f986be5ae7e642707e978c:
     Read the CROSSTOOL from the package of the current cc_toolchain,
     not from --crosstool_top
```

- Fixes regression #6662, by fixing tools/cpp/BUILD
- Fixes regression #6665, by setting the toolchain identifier.
- CROSSTOOL file is now read from the package of cc_toolchain, not from the
  package of cc_toolchain_suite. This is not expected to break anybody since
  cc_toolchain_suite and cc_toolchain are commonly in the same package.

## Release 0.19.1 (2018-11-12)

```
Baseline: ac880418885061d1039ad6b3d8c28949782e02d6

Cherry picks:

   + 9bc3b20053a8b99bf2c4a31323a7f96fabb9f1ec:
     Fix the "nojava" platform and enable full presubmit checks for
     the various JDK platforms now that we have enough GCE resources.
   + 54c2572a8cabaf2b29e58abe9f04327314caa6a0:
     Add openjdk_linux_archive java_toolchain for nojava platform.
   + 20bfdc67dc1fc32ffebbda7088ba49ee17e3e182:
     Automated rollback of commit
     19a401c38e30ebc0879925a5caedcbe43de0028f.
   + 914b4ce14624171a97ff8b41f9202058f10d15b2:
     Windows: Fix Precondition check for addDynamicInputLinkOptions
   + 83d406b7da32d1b1f6dd02eae2fe98582a4556fd:
     Windows, test-setup.sh: Setting RUNFILES_MANIFEST_FILE only when
     it exists.
   + e025726006236520f7e91e196b9e7f139e0af5f4:
     Update turbine
   + 5f312dd1678878fb7563eae0cd184f2270346352:
     Fix event id for action_completed BEP events
   + f0c844c77a2406518c4e75c49188390d5e281d3d:
     Release 0.19.0 (2018-10-29)
   + c3fb1db9e4e817e8a911f5b347b30f2674a82f7c:
     Do not use CROSSTOOL to select cc_toolchain
   + 8e280838e8896a6b5eb5421fda435b96b6f8de60:
     Windows Add tests for msys gcc toolchain and mingw gcc toolchain
   + fd52341505e725487c6bc6dfbe6b5e081aa037da:
     update bazel-toolchains pin to latest release Part of changes to
     allow bazelci to use 0.19.0 configs. RBE toolchain configs at or
     before 0.17.0 are not compatible with bazel 0.19.0 or above.
```

Important changes:
- Fix regression #6610, which prevents using the MINGW compiler on Windows.

## Release 0.19.0 (2018-10-29)

```
Baseline: ac880418885061d1039ad6b3d8c28949782e02d6

Cherry picks:

   + 9bc3b20053a8b99bf2c4a31323a7f96fabb9f1ec:
     Fix the "nojava" platform and enable full presubmit checks for
     the various JDK platforms now that we have enough GCE resources.
   + 54c2572a8cabaf2b29e58abe9f04327314caa6a0:
     Add openjdk_linux_archive java_toolchain for nojava platform.
   + 20bfdc67dc1fc32ffebbda7088ba49ee17e3e182:
     Automated rollback of commit
     19a401c38e30ebc0879925a5caedcbe43de0028f.
   + 914b4ce14624171a97ff8b41f9202058f10d15b2:
     Windows: Fix Precondition check for addDynamicInputLinkOptions
   + 83d406b7da32d1b1f6dd02eae2fe98582a4556fd:
     Windows, test-setup.sh: Setting RUNFILES_MANIFEST_FILE only when
     it exists.
   + e025726006236520f7e91e196b9e7f139e0af5f4:
     Update turbine
   + 5f312dd1678878fb7563eae0cd184f2270346352:
     Fix event id for action_completed BEP events
```

The Bazel team is happy to announce a new version of Bazel, [Bazel 0.19](https://github.com/bazelbuild/bazel/releases/tag/0.19.0).

This document lists the major changes since Bazel 0.18.

General changes
---------------

* The `--incompatible_expand_directories` flag will automatically expand directories in command lines. Design doc: https://docs.google.com/document/d/11agWFiOUiz2htBLj6swPTob5z78TrCxm8DQE4uJLOwM

* The `--loading_phase_threads` flag now defaults to `auto` (not 200, as was previously the case), which at the moment corresponds to the number of CPUs. This is appropriate for most users. However, if your sources reside on a network file system, increasing this value may yield better analysis-time performance when disk caches are cold.

Android
-------

* Fixed missing debug symbols when building native code with `--compilation_mode=dbg` that target Android ARM architectures by adding the `-g` flag.

C++
---

* Added `--incompatible_disable_legacy_flags_cc_toolchain_api` to deprecate legacy `cc_toolchain` Starlark API for legacy CROSSTOOL fields. Tracking issue is #6434. Migration docs are on the bazel website.

* Runfiles in cc_test: the C++ runfiles library (`@bazel_tools//tools/cpp/runfiles`) can now create Runfiles objects for tests. See `//tools/cpp/runfiles/runfiles_src.h` (in the Bazel source tree) for documentation.

* :cc_binary link action no longer hardcodes `-static-libgcc` for toolchains that support embedded runtimes (guarded by `--experimental_dont_emit_static_libgcc` temporarily).

* The flag `--experimental_enable_cc_configuration_make_variables` is removed, use `--incompatible_disable_cc_configuration_make_variables` instead.

Java
----

* If the `--javabase` flag is unset, Bazel locates a JDK using the `JAVA_HOME` environment variable and searching the PATH. If no JDK is found `--javabase` will be empty, and builds targeting Java will not be supported.  Previously Bazel would fall back to using the embedded JDK as a `--javabase`, but this is no longer default behaviour. A JDK should be explicitly installed instead to enable Java development.

Code Coverage
-------------

* LcovMerger was renamed to CoverageOutputGenerator.

* Faster coverage collection for gcc compiled C++ code can now be tested by enabling it with `--experimental_cc_coverage`.

Other Changes
-------------

* Add `--apple_compiler` and `--apple_grte_top options`. These provide the equivalent of --compiler / --grte_top for the toolchain configured in --apple_crosstool_top.

* There is now a `same_pkg_direct_rdeps` query function. See the query documentation for more details.

* Propagating remote errors to the user even if `--verbose_failures=false` is set.

* Add number of configured targets to analysis phase status output.

* Bazel will now check stderr instead of stdout to decide if it is outputting to a terminal.  `--isatty` is deprecated, use `--is_stderr_atty` instead.

Future Changes
--------------

* None of the C++ related incompatible flags mentioned in the 0.18 release were flipped, they will be flipped in the next release (0.20). We have created tracking issues for all the relevant incompatible flags:
    * [`--incompatible_disable_late_bound_option_defaults`](https://docs.bazel.build/versions/master/skylark/backward-compatibility.html#disable-late-bound-option-defaults): #6384
    * [`--incompatible_disable_depset_in_cc_user_flags`](https://docs.bazel.build/versions/master/skylark/backward-compatibility.html#disable-depsets-in-c-toolchain-api-in-user-flags): #6383
    * [`--incompatible_disable_cc_toolchain_label_from_crosstool_proto`](https://docs.bazel.build/versions/master/skylark/backward-compatibility.html#disallow-using-crosstool-to-select-the-cc_toolchain-label): #6382
    * [`--incompatible_disable_cc_configuration_make_variables`](https://github.com/bazelbuild/bazel/issues/6381): #6381
    * [`--incompatible_disable_legacy_cpp_toolchain_skylark_api`](https://docs.bazel.build/versions/master/skylark/backward-compatibility.html#disable-legacy-c-configuration-api): #6380
    * [`incompatible_disable_legacy_flags_cc_toolchain_api`](https://docs.bazel.build/versions/master/skylark/backward-compatibility.html#disable-legacy-c-toolchain-api): #6434

* In the 0.20 release the flags [`--incompatible_remove_native_git_repository`](https://docs.bazel.build/versions/master/skylark/backward-compatibility.html#remove-native-git-repository) and [`--incompatible_remove_native_http_archive`](https://docs.bazel.build/versions/master/skylark/backward-compatibility.html#remove-native-http-archive) will be turned on.

Thank you to our contributors!
------------------------------

This release contains contributions from many people at Google, as well as Andreas Herrmann, Andreas Hippler, Benjamin Peterson, David Ostrovsky, Ed Baunton, George Gensure, Igal Tabachnik, Jason Gavris, Loo Rong Jie, rmalik, and Yannic Bonenberger

Thank you to everyone who contributed to this release!

## Release 0.18.1 (2018-10-31)

```
Baseline: c062b1f1730f3562d5c16a037b374fc07dc8d9a2

Cherry picks:

   + 2834613f93f74e988c51cf27eac0e59c79ff3b8f:
     Include also ext jars in the bootclasspath jar.
   + 2579b791c023a78a577e8cb827890139d6fb7534:
     Fix toolchain_java9 on --host_javabase=<jdk9> after
     7eb9ea150fb889a93908d96896db77d5658e5005
   + faaff7fa440939d4367f284ee268225a6f40b826:
     Release notes: fix markdown
   + b073a18e3fac05e647ddc6b45128a6158b34de2c:
     Fix NestHost length computation Fixes #5987
   + bf6a63d64a010f4c363d218e3ec54dc4dc9d8f34:
     Fixes #6219. Don't rethrow any remote cache failures on either
     download or upload, only warn. Added more tests.
   + c1a7b4c574f956c385de5c531383bcab2e01cadd:
     Fix broken IdlClassTest on Bazel's CI.
   + 71926bc25b3b91fcb44471e2739b89511807f96b:
     Fix the Xcode version detection which got broken by the upgrade
     to Xcode 10.0.
   + 86a8217d12263d598e3a1baf2c6aa91b2e0e2eb5:
     Temporarily restore processing of workspace-wide tools/bazel.rc
     file.
   + 914b4ce14624171a97ff8b41f9202058f10d15b2:
     Windows: Fix Precondition check for addDynamicInputLinkOptions
   + e025726006236520f7e91e196b9e7f139e0af5f4:
     Update turbine
```

Important changes:

  - Fix regression #6219, remote cache failures

## Release 0.18.0 (2018-10-15)

```
Baseline: c062b1f1730f3562d5c16a037b374fc07dc8d9a2

Cherry picks:

   + 2834613f93f74e988c51cf27eac0e59c79ff3b8f:
     Include also ext jars in the bootclasspath jar.
   + 2579b791c023a78a577e8cb827890139d6fb7534:
     Fix toolchain_java9 on --host_javabase=<jdk9> after
     7eb9ea150fb889a93908d96896db77d5658e5005
   + faaff7fa440939d4367f284ee268225a6f40b826:
     Release notes: fix markdown
   + b073a18e3fac05e647ddc6b45128a6158b34de2c:
     Fix NestHost length computation Fixes #5987
   + bf6a63d64a010f4c363d218e3ec54dc4dc9d8f34:
     Fixes #6219. Don't rethrow any remote cache failures on either
     download or upload, only warn. Added more tests.
   + c1a7b4c574f956c385de5c531383bcab2e01cadd:
     Fix broken IdlClassTest on Bazel's CI.
   + 71926bc25b3b91fcb44471e2739b89511807f96b:
     Fix the Xcode version detection which got broken by the upgrade
     to Xcode 10.0.
   + 86a8217d12263d598e3a1baf2c6aa91b2e0e2eb5:
     Temporarily restore processing of workspace-wide tools/bazel.rc
     file.
```

General changes

- New [bazelrc file list](https://docs.bazel.build/versions/master/user-manual.html#where-are-the-bazelrc-files).
  If you need to keep both the old and new lists of .rc files active
  concurrently to support multiple versions of Bazel, you can import the old
  file location into the new list using `try-import`. This imports a file if it
  exists and silently exits if it does not. You can use this method to account
  for a user file that may or may not exist

- [.bazelignore](https://docs.bazel.build/versions/master/user-manual.html#.bazelignore)
  is now fully functional.

- The startup flag `--host_javabase` has been renamed to
  `--server_javabase` to avoid confusion with the build flag
  `--host_javabase`.

Android

- The Android resource processing pipeline now supports persistence
  via worker processes. Enable it with
  `--persistent_android_resource_processor`. We have observed a 50% increase
  in build speed for clean local builds and up to 150% increase in build
  speed for incremental local builds.

C++

- In-memory package //tools/defaults has been removed (controlled by
  `--incompatible_disable_tools_defaults_package` flag). Please see
  [migration instructions](https://docs.bazel.build/versions/master/skylark/backward-compatibility.html#disable-inmemory-tools-defaults-package)
  and migrate soon, the flag will be flipped in Bazel 0.19, and the legacy
  behavior will be removed in Bazel 0.20.

- Late bound option defaults (typical example was the `--compiler` flag, when
  it was not specified, it’s value was computed using the CROSSTOOL) are removed
  (controlled by `--incompatible_disable_late_bound_option_defaults` flag).
  Please see [migration instructions](https://docs.bazel.build/versions/master/skylark/backward-compatibility.html#disable-late-bound-option-defaults)
  and migrate soon, the flag will be flipped in Bazel 0.19, and the legacy
  behavior will be removed in Bazel 0.20.

- Depsets are no longer accepted in `user_compile_flags` and `user_link_flags`
  in the C++ toolchain API (controlled by
  `--incompatible_disable_depset_in_cc_user_flags` flag) affects C++ users.
  Please see [migration instructions](https://docs.bazel.build/versions/master/skylark/backward-compatibility.html#disable-depsets-in-c-toolchain-api-in-user-flags)
  and migrate soon, the flag will be flipped in Bazel 0.19, and the legacy
  behavior will be removed in Bazel 0.20.

- CROSSTOOL is no longer consulted when selecting C++ toolchain (controlled by
  `--incompatible_disable_cc_toolchain_label_from_crosstool_proto` flag).
  Please see [migration instructions](https://docs.bazel.build/versions/master/skylark/backward-compatibility.html#disallow-using-crosstool-to-select-the-cc_toolchain-label)
  and migrate soon, the flag will be flipped in Bazel 0.19, and the legacy behavior will be removed in Bazel 0.20.

- You can now use [`toolchain_identifier` attribute](https://github.com/bazelbuild/bazel/commit/857d4664ce939f240b1d10d8d2baca6c6893cfcb)
  on `cc_toolchain` to pair it with CROSSTOOL toolchain.

- C++ specific Make variables
  are no longer passed from the `CppConfiguration`, but from the C++ toolchain
  (controlled by `--incompatible_disable_cc_configuration_make_variables` flag).
  Please see [migration instructions](https://docs.bazel.build/versions/master/skylark/backward-compatibility.html#disallow-using-c-specific-make-variables-from-the-configuration)
  and migrate soon, the flag will be flipped
  in Bazel 0.19, and the legacy behavior will be removed in Bazel 0.20.

- Skylark api accessing C++
  toolchain in `ctx.fragments.cpp` is removed (controlled by
  `--incompatible_disable_legacy_cpp_toolchain_skylark_api` flag).
  Please migrate soon, the flag will be flipped
  in Bazel 0.19, and the legacy behavior will be removed in Bazel 0.20.

- cc_binary link action no longer hardcodes
  `-static-libgcc` for toolchains that support embedded runtimes
  (guarded by [`--experimental_dont_emit_static_libgcc`](https://source.bazel.build/bazel/+/2f281960b829e964526a9d292d4c3003e4d19f1c)
  temporarily). Proper deprecation using `--incompatible` flags will follow.

Java

- Future versions of Bazel will require a locally installed JDK
  for Java development. Previously Bazel would fall back to using
  the embedded `--server_javabase` if no JDK as available. Pass
  `--incompatible_never_use_embedded_jdk_for_javabase` to disable the
  legacy behaviour.

- `--javacopt=` no longer affects compilations of tools that are
  executed during the build; use `--host_javacopt=` to change javac
  flags in the host configuration.

Objective C

- `objc_library` now supports the module_name attribute.

Skylark

- Adds `--incompatible_expand_directories` to automatically expand
  directories in skylark command lines. Design doc:
  https://docs.google.com/document/d/11agWFiOUiz2htBLj6swPTob5z78TrCxm8DQE4uJLOwM

- Support fileset expansion in ctx.actions.args(). Controlled by
  `--incompatible_expand_directories`.

Windows

- `--windows_exe_launcher` is deprecated, this flag will be removed
  soon. Please make sure you are not using it.

- Bazel now supports the symlink runfiles tree on Windows with
  `--experimental_enable_runfiles` flag. For more details, see
  [this doc](https://docs.google.com/document/d/1hnYmU1BmtCSJOUvvDAK745DSJQCapToJxb3THXYMrmQ).

Other Changes

- A new experimental option `--experimental_ui_deduplicate` has been added. It
  causes the UI to attempt to deduplicate messages from actions to keep the
  console output cleaner.

- Add `--modify_execution_info`, a flag to customize action execution
  info.

- Add ExecutionInfo to aquery output for ExecutionInfoSpecifier
  actions.

- When computing `--instrumentation_filter`, end filter patterns with
  "[/:]" to match non-top-level packages exactly and treat
  top-level targets consistently.

- Added the `bazel info server_log` command, which obtains the main Bazel
  server log file path. This can help debug Bazel issues.

- `aapt shrink` resources now properly respect filter configurations.

## Release 0.17.2 (2018-09-21)

```
Baseline: aa118ca818baf722aede0bc48d0a17584fa45b6e

Cherry picks:
   + 0e0462589528154cb5160411991075a2000b5452:
     Update checker framework dataflow and javacutil versions
   + 3987300d6651cf0e6e91b395696afac6913a7d66:
     Stop using --release in versioned java_toolchains
   + 438b2773b8c019afa46be470b90bcf70ede7f2ef:
     make_deb: Add new empty line in the end of conffiles file
   + 504401791e0a0e7e3263940e9e127f74956e7806:
     Properly mark configuration files in the Debian package.
   + 9ed9d8ac4347408d15c8fce7c9c07e5c8e658b30:
     Add flag
     --incompatible_symlinked_sandbox_expands_tree_artifacts_in_runfil
     es_tree.
   + 22d761ab42dfb1b131f1facbf490ccdb6c17b89c:
     Update protobuf to 3.6.1 -- add new files
   + 27303d79c38f2bfa3b64ee7cd7a6ef03a9a87842:
     Update protobuf to 3.6.1 -- update references
   + ddc97ed6b0367eb443e3e09a28d10e65179616ab:
     Update protobuf to 3.6.1 -- remove 3.6.0 sources
   + ead1002d3803fdfd4ac68b4b4872076b19d511a2:
     Fix protobuf in the WORKSPACE
   + 12dcd35ef7a26d690589b0fbefb1f20090cbfe15:
     Revert "Update to JDK 10 javac"
   + 7eb9ea150fb889a93908d96896db77d5658e5005:
     Automated rollback of
     https://github.com/bazelbuild/bazel/commit/808ec9ff9b5cec14f23a4b
     a106bc5249cacc8c54 and
     https://github.com/bazelbuild/bazel/commit/4c9149d558161e7d3e363f
     b697f5852bc5742a36 and some manual merging.
   + 4566a428c5317d87940aeacfd65f1018340e52b6:
     Fix tests on JDK 9 and 10
   + 1e9f0aa89dad38eeab0bd40e95e689be2ab6e5e5:
     Fix more tests on JDK 9 and 10
   + a572c1cbc8c26f625cab6716137e2d57d05cfdf3:
     Add ubuntu1804_nojava, ubuntu1804_java9, ubuntu1804_java10 to
     postsubmit.
   + 29f1de099e4f6f0f50986aaa4374fc5fb7744ee8:
     Disable Android shell tests on the "nojava" platform.
   + b495eafdc2ab380afe533514b3bcd7d5b30c9935:
     Update bazel_toolchains to latest release.
   + 9323c57607d37f9c949b60e293b573584906da46:
     Windows: fix writing java.log
   + 1aba9ac4b4f68b69f2d91e88cfa8e5dcc7cb98c2:
     Automated rollback of commit
     de22ab0582760dc95f33e217e82a7b822378f625.
   + 2579b791c023a78a577e8cb827890139d6fb7534:
     Fix toolchain_java9 on --host_javabase=<jdk9> after
     7eb9ea150fb889a93908d96896db77d5658e5005
   + 2834613f93f74e988c51cf27eac0e59c79ff3b8f:
     Include also ext jars in the bootclasspath jar.
   + fdb09a260dead1e1169f94584edc837349a4f4a5:
     Release 0.17.1 (2018-09-14)
   + 1d956c707e1c843896ac58a341c335c9c149073d:
     Do not fail the build when gcov is not installed
   + 2e677fb6b8f309b63558eb13294630a91ee0cd33:
     Ignore unrecognized VM options in desugar.sh, such as the JVM 9
     flags to silence warnings.
```

Important changes:

  - In the future, Bazel will expand tree artifacts in runfiles, too,
    which causes the sandbox to link each file individually into the
    sandbox directory, instead of symlinking the entire directory. In
    this release, the behavior is not enabled by default yet. Please
    try it out via
    --incompatible_symlinked_sandbox_expands_tree_artifacts_in_runfile
    s_tree and let us know if it causes issues. If everything looks
    good, this behavior will become the default in a following
    release.

## Release 0.17.1 (2018-09-14)

```
Baseline: aa118ca818baf722aede0bc48d0a17584fa45b6e

Cherry picks:
   + 0e0462589528154cb5160411991075a2000b5452:
     Update checker framework dataflow and javacutil versions
   + 3987300d6651cf0e6e91b395696afac6913a7d66:
     Stop using --release in versioned java_toolchains
   + 438b2773b8c019afa46be470b90bcf70ede7f2ef:
     make_deb: Add new empty line in the end of conffiles file
   + 504401791e0a0e7e3263940e9e127f74956e7806:
     Properly mark configuration files in the Debian package.
   + 9ed9d8ac4347408d15c8fce7c9c07e5c8e658b30:
     Add flag
     --incompatible_symlinked_sandbox_expands_tree_artifacts_in_runfil
     es_tree.
   + 22d761ab42dfb1b131f1facbf490ccdb6c17b89c:
     Update protobuf to 3.6.1 -- add new files
   + 27303d79c38f2bfa3b64ee7cd7a6ef03a9a87842:
     Update protobuf to 3.6.1 -- update references
   + ddc97ed6b0367eb443e3e09a28d10e65179616ab:
     Update protobuf to 3.6.1 -- remove 3.6.0 sources
   + ead1002d3803fdfd4ac68b4b4872076b19d511a2:
     Fix protobuf in the WORKSPACE
   + 12dcd35ef7a26d690589b0fbefb1f20090cbfe15:
     Revert "Update to JDK 10 javac"
   + 7eb9ea150fb889a93908d96896db77d5658e5005:
     Automated rollback of
     https://github.com/bazelbuild/bazel/commit/808ec9ff9b5cec14f23a4b
     a106bc5249cacc8c54 and
     https://github.com/bazelbuild/bazel/commit/4c9149d558161e7d3e363f
     b697f5852bc5742a36 and some manual merging.
   + 4566a428c5317d87940aeacfd65f1018340e52b6:
     Fix tests on JDK 9 and 10
   + 1e9f0aa89dad38eeab0bd40e95e689be2ab6e5e5:
     Fix more tests on JDK 9 and 10
   + a572c1cbc8c26f625cab6716137e2d57d05cfdf3:
     Add ubuntu1804_nojava, ubuntu1804_java9, ubuntu1804_java10 to
     postsubmit.
   + 29f1de099e4f6f0f50986aaa4374fc5fb7744ee8:
     Disable Android shell tests on the "nojava" platform.
   + b495eafdc2ab380afe533514b3bcd7d5b30c9935:
     Update bazel_toolchains to latest release.
   + 9323c57607d37f9c949b60e293b573584906da46:
     Windows: fix writing java.log
   + 1aba9ac4b4f68b69f2d91e88cfa8e5dcc7cb98c2:
     Automated rollback of commit
     de22ab0582760dc95f33e217e82a7b822378f625.
   + 2579b791c023a78a577e8cb827890139d6fb7534:
     Fix toolchain_java9 on --host_javabase=<jdk9> after
     7eb9ea150fb889a93908d96896db77d5658e5005
   + 2834613f93f74e988c51cf27eac0e59c79ff3b8f:
     Include also ext jars in the bootclasspath jar.
```

Incompatible changes:

  - Loading @bazel_tools//tools/build_defs/repo:git_repositories.bzl
    no longer works. Load @bazel_tools//tools/build_defs/repo:git.bzl
    instead.
  - If the same artifact is generated by two distinct but identical
    actions, and a downstream action has both those actions' outputs
    in its inputs, the artifact will now appear twice in the
    downstream action's inputs. If this causes problems in Skylark
    actions, you can use the uniquify=True argument in Args.add_args.
  - If the same artifact is generated by two distinct but identical
    actions, and a downstream action has both those actions' outputs
    in its inputs, the artifact will now appear twice in the
    downstream action's inputs. If this causes problems in Skylark
    actions, you can use the uniquify=True argument in Args.add_args.
  - Labels in C++ rules' linkopts attribute are not expanded anymore
    unless they are wrapped, e.g: $(location //foo:bar)
  - If the same artifact is generated by two distinct but identical
    actions, and a downstream action has both those actions' outputs
    in its inputs, the artifact will now appear twice in the
    downstream action's inputs. If this causes problems in Skylark
    actions, you can use the uniquify=True argument in Args.add_args.
  - New bazelrc file list.
  - Windows: when BAZEL_SH envvar is not defined and Bazel searches
    for a suitable bash.exe, Bazel will no longer look for Git Bash
    and no longer recommend installing it as a Bash implementation.
    See issue #5751.
  - New bazelrc file list.

New features:

  - The aquery command now supports --output=text.
  - Java, runfiles: the Java runfiles library is now in
    @bazel_tools//tools/java/runfiles. The old target
    (@bazel_tools//tools/runfiles:java-runfiles) is deprecated and
    will be removed in Bazel 0.18.0.
  - Java, runfiles: the Java runfiles library is now in
    @bazel_tools//tools/java/runfiles. The old target
    (@bazel_tools//tools/runfiles:java-runfiles) is deprecated and
    will be removed in Bazel 0.19.0 (not 0.18.0, as stated earlier).

Important changes:

  - Allow @ in package names.
  - Remove support for java_runtime_suite; use alias() together with
    select() instead.
  - Python wrapper scripts for MSVC are removed.
  - [JavaInfo] Outputs are merged in java_common.merge().
  - Faster analysis by improved parallelization.
  - --experimental_shortened_obj_file_path is removed.
  - Introduce the --remote_cache_proxy flag,
    which allows for remote http caching to connect
    via a unix domain socket.
  - No longer define G3_VERSION_INFO for c++ linkstamp compiles, as
    it was a duplicate of G3_TARGET_NAME.
  - Added support for Android NDK r17. The default STL is now
    `libc++`, and support for targeting `mips`, `mips64` and `ARMv5`
    (`armeabi`) has been removed.
  - Add aquery command to get analysis time information about the
    action graph.
  - Fixed compatibility with aar_import when using aapt2.  AAPT2 is
    now supported for Android app builds without resource shrinking.
    To use it, pass the `--android_aapt=aapt2` flag or define
    android_binary.aapt_version=aapt2.
  - Code coverage is collected for Java binaries invoked from sh_test.
  - java_common.compile creates the native headers jar accesible via
    JavaInfo.outputs.native_headers.
  - Deleting deprecated no-op flag --show_package_location
  - The JDK shipped with Bazel was updated to JDK10.
  - Rename the startup flag --host_javabase to --server_javabase to
    avoid confusion with the build flag --host_javabase
  - newly added options --experimental_repository_hash_file and
      --experimental_verify_repository_rules allow to verify for
    repositories
      the directory generated against pre-recorded hashes. See
    documentation
      for those options.
  - Removed the gen_jars output group
  - --subcommands can now take a "pretty_print" value
    ("--subcommands=pretty_print") to print the
    arguments of subcommands as a list for easier reading.
  - follow-up to
    https://github.com/bazelbuild/bazel/commit/1ac359743176e659e9c7472
    645e3142f3c44b9e8
  - A rule error is now thrown if a Skylark rule implementation
    function returns multiple providers of the same type.
  - When using Bazel's remote execution feature and Bazel has to
    fallback to local execution for an action, Bazel used
    non-sandboxed
    local execution until now. From this release on, you can use the
    new
    flag --remote_local_fallback_strategy=<strategy> to tell Bazel
    which
    strategy to use in that case.
  - Execution Log Parser can now, when printing it out, filter the
    log by runner type
  - A rule error is now thrown if a Skylark rule implementation
    function returns multiple providers of the same type.
  - Removed the gen_jars output group
  - Removed the gen_jars output group
  - Set --defer_param_files to default to true.
  - Sort attribute lists in proto-form query output to fix
    non-deterministic genquery output.
  - Replace 0/1 with False/True for testonly attribute
  - bazel now supports a .bazelignore file specifying
      directories to be ignored; however, these directories still
      have to be well founded and, in particular, may not contain
      symlink cycles.
  - Add more detailed reporting of the differences between startup
    options.
  - update data binding to 3.2.0
  - For Android incremental dexing actions, Bazel now persists its
    DexBuilder process across individual actions. From our
    benchmarks, this results in a 1.2x speedup for clean local builds.
  - The standard `xcode_VERSION` feature now always uses exactly two
    components in the version, even if you specify `--xcode_version`
    with
    more or fewer than two.
  - A rule error will be thrown if a Skylark rule implementation
    function returns multiple providers of the same type. Try the
    `--incompatible_disallow_conflicting_providers` flag to ensure
    your code is forward-compatible.
  - Removed notion of FULLY_STATIC linking mode from C++ rules.
  - In documentation, we've renamed Skylark into Starlark.
  - Execution Log Parser can now, when printing it out, reorder the
    actions for easier text diffs
  - Linkstamps are no longer recompiled after server restart.
  - Use VanillaJavaBuilder and disable header compilation in
    toolchain_hostjdk8. The default toolchain will soon drop
    compatibility with JDK 8. Using a JDK 8 host_javabase
    will only be supported when using 'VanillaJavaBuilder' (which
    does not support Error Prone,
    Strict Java Deps, or reduced classpaths) and with header
    compilation disabled.
  - In the future, Bazel will expand tree artifacts in runfiles, too,
    which causes the sandbox to link each file individually into the
    sandbox directory, instead of symlinking the entire directory. In
    this release, the behavior is not enabled by default yet. Please
    try it out via
    --incompatible_symlinked_sandbox_expands_tree_artifacts_in_runfile
    s_tree and let us know if it causes issues. If everything looks
    good, this behavior will become the default in a following
    release.

## Release 0.16.1 (2018-08-13)

```
Baseline: 4f64b77a3dd8e4ccdc8077051927985f9578a3a5

Cherry picks:
   + 4c9a0c82d308d5df5c524e2a26644022ff525f3e:
     reduce the size of bazel's embedded jdk
   + d3228b61f633cdc5b3f740b641a0836f1bd79abd:
     remote: limit number of open tcp connections by default. Fixes
     #5491
   + 8ff87c164f48dbabe3b20becd00dde90c50d46f5:
     Fix autodetection of linker flags
   + c4622ac9205d2f1b42dac8c598e83113d39e7f11:
     Fix autodetection of -z linker flags
   + 10219659f58622d99034288cf9f491865f818218:
     blaze_util_posix.cc: fix order of #define
   + ab1f269017171223932e0da9bb539e8a17dd99ed:
     blaze_util_freebsd.cc: include path.h explicitly
   + 68e92b45a37f2142c768a56eb7ecfa484b8b22df:
     openjdk: update macOS openjdk image. Fixes #5532
   + f45c22407e6b00fcba706eb62141cb9036bd38d7:
     Set the start time of binary and JSON profiles to zero correctly.
   + bca1912853086b8e9a28a85a1b144ec0dc9717cc:
     remote: fix race on download error. Fixes #5047
   + 3842bd39e10612c7eef36c6048407e81bcd0a8fb:
     jdk: use parallel old gc and disable compact strings
   + 6bd0bdf5140525cb33dc2db068b210261d9df271:
     Add objc-fully-link to the list of actions that require the
     apple_env feature. This fixes apple_static_library functionality.
   + f330439fb970cfa17c70fc59c1458bb1c31c9522:
     Add the action_names_test_files target to the OSS version of
     tools/buils_defs/cc/BUILD.
   + d215b64362c4ede61c8ba87b5f3f57bce4785d15:
     Fix StackOverflowError on Windows. Fixes #5730
   + 366da4cf27b7f957ef39f89206db77fa2ac289df:
     In java_rules_skylark depend on the javabase through
     //tools/jdk:current_java_runtime
   + 30c601dc13d9e1b40a57434c022c888c7578cc56:
     Don't use @local_jdk for jni headers
   + c56699db5f9173739ba3ac55aa9fa69b6457a99b:
     'DumpPlatformClasspath' now dumps the current JDK's default
     platform classpath
```

This release is a patch release that contains fixes for several serious
regressions that were found after the release of Bazel 0.16.0.

In particular this release resolves the following issues:

 - Bazel crashes with a StackOverflowError on Windows (See #5730)
 - Bazel requires a locally installed JDK and does not fall back
   to the embedded JDK (See #5744)
 - Bazel fails to build for Homebrew on macOS El Capitan (See #5777)
 - A regression in apple_static_library (See #5683)

Please watch our blog for a more detailed release announcement.

## Release 0.16.0 (2018-07-31)

```
Baseline: 4f64b77a3dd8e4ccdc8077051927985f9578a3a5

Cherry picks:
   + 4c9a0c82d308d5df5c524e2a26644022ff525f3e:
     reduce the size of bazel's embedded jdk
   + d3228b61f633cdc5b3f740b641a0836f1bd79abd:
     remote: limit number of open tcp connections by default. Fixes
     #5491
   + 8ff87c164f48dbabe3b20becd00dde90c50d46f5:
     Fix autodetection of linker flags
   + c4622ac9205d2f1b42dac8c598e83113d39e7f11:
     Fix autodetection of -z linker flags
   + 10219659f58622d99034288cf9f491865f818218:
     blaze_util_posix.cc: fix order of #define
   + ab1f269017171223932e0da9bb539e8a17dd99ed:
     blaze_util_freebsd.cc: include path.h explicitly
   + 68e92b45a37f2142c768a56eb7ecfa484b8b22df:
     openjdk: update macOS openjdk image. Fixes #5532
   + f45c22407e6b00fcba706eb62141cb9036bd38d7:
     Set the start time of binary and JSON profiles to zero correctly.
   + bca1912853086b8e9a28a85a1b144ec0dc9717cc:
     remote: fix race on download error. Fixes #5047
   + 3842bd39e10612c7eef36c6048407e81bcd0a8fb:
     jdk: use parallel old gc and disable compact strings
```

Incompatible changes:

  - The $(ANDROID_CPU) Make variable is not available anymore. Use
    $(TARGET_CPU) after an Android configuration transition instead.
  - The $(JAVA_TRANSLATIONS) Make variable is not supported anymore.
  - Skylark structs (using struct()) may no longer have to_json and
    to_proto overridden.
  - The mobile-install --skylark_incremental_res flag is no longer
    available, use the --skylark flag instead.

New features:

  - android_local_test now takes advantage of Robolectric's binary
    resource processing which allows for faster tests.
  - Allow @ in package names.

Important changes:

  - Option --glibc is removed, toolchain selection relies solely on
    --cpu and --compiler options.
  - Build support for enabling cross binary FDO optimization.
  - The --distdir option is no longer experimental. This
      option allows to specify additional directories to look for
      files before trying to fetch them from the network. Files from
      any of the distdirs are only used if a checksum for the file
      is specified and both, the filename and the checksum, match.
  - Java coverage works now with multiple jobs.
  - Flip default value of --experimental_shortened_obj_file_path to
    true, Bazel now generates short object file path by default.
  - New rules for importing Android dependencies:
    `aar_import_external` and `aar_maven_import_external`.
    `aar_import_external` enables specifying external AAR
    dependencies using a list of HTTP URLs for the artifact.
    `aar_maven_import_external` enables specifying external AAR
    dependencies using the artifact coordinate and a list of server
    URLs.
  - The BAZEL_JAVAC_OPTS environment variable allows arguments, e.g.,
    "-J-Xmx2g", may be passed to the javac compiler during bootstrap
    build. This is helpful if your system chooses too small of a max
    heap size for the Java compiler during the bootstrap build.
  - --noexpand_configs_in_place is deprecated.
  - A tool to parse the Bazel execution log.
  - Support for LIPO has been fully removed.
  - Remove support for --discard_actions_after_execution.
  - Add --materialize_param_files flag to write parameter files even
    when actions are executed remotely.
  - Windows default system bazelrc is read from the user's
    ProgramData if present.
  - --[no]allow_undefined_configs no longer exists, passing undefined
    configs is an error.
  - In remote caching we limit the number of open
    TCP connections to 100 by default. The number can be adjusted
    by specifying the --remote_max_connections flag.

## Release 0.15.0 (2018-06-26)

```
Baseline: b93ae42e8e693ccbcc387841a17f58259966fa38

Cherry picks:
   + 4b80f2455e7e49a95f3a4c9102a67a57dad52207:
     Add option to enable Docker sandboxing.
   + 6b1635279e8b33dc1ac505ac81825e38f8797a14:
     Allow disabling the simple blob caches via CLI flag overrides.
   + 4ec0a7524913ab2c4641368e3f8c09b347351a08:
     Use BUILD.bazel instead of BUILD for external projects
```

Incompatible changes:

  - Bazel now always runs binaries in with "bazel run" in
    interactive mode. The "--nodirect_run" command line option is now
    a no-op.
  - "bazel run --noas_test" is not supported anymore.
  - Indentation on the first line of a file was previously ignored.
    This is now fixed.

New features:

  - C++,runfiles: to access data-dependencies (runfiles) in C++
    programs, use the runfiles library built into Bazel. For usage
    info, see
    https://github.com/bazelbuild/bazel/blob/master/tools/cpp/runfiles
    /runfiles.h

Important changes:

  - Bazel now allows almost all 7-bit ASCII characters in labels.
  - Remove vestigial java_plugin.data attribute
  - Bazel supports including select Java 8 APIs into Android apps
    targeting pre-Nougat Android devices with
    --experimental_desugar_java8_libs
  - Flag `--incompatible_disable_glob_tracking` is removed.
  - SkyQuery's rbuildfiles now returns targets corresponding to
    broken packages.
  - Introduce build support for providing cache prefetch hints.
  - Update the skylark DefaultInfo documentation to spell out
    runfiles, data_runfiles and default_runfiles
  - An internal action for symlinking runfiles will use Command
    instead of a Spawns. This should have no functional chages; the
    only user visible consequence should be that the internal action
    is no longer be included in statistics when calculating processes
    count.
  - --batch is deprecated
  - execution strategies line no longer handles differently the case
    where all processes have the same strategy.
  - The --experimental_remote_spawn_cache flag is now enabled by
    default, and remote caching no longer needs --*_strategy=remote
    flags (it will fail if they are specified).
  - android_binary.aapt_version='aapt2' now supports en_XA and ar_XB
  - Added --apple_enable_auto_dsym_dbg flag.
  - non_propagated_deps has been removed from objc_library and
    apple_binary.
  - For Android projects, Bazel now supports building fonts as
    resources. See
    https://developer.android.com/guide/topics/ui/look-and-feel/fonts-in-xml
    for more information on the feature.
  - With --incompatible_no_support_tools_in_action_inputs enabled, Skylark
    action inputs are no longer scanned for tools. Move any such
    inputs to the newly introduced 'tools' attribute.

## Release 0.14.1 (2018-06-08)

```
Baseline: 5c3f5c9be7fa40d4fb3c35756891fab8483ca406

Cherry picks:
   + f96f037f8f77335dc444844abcc31a372a3e1849:
     Windows, Java launcher: Support jar files under different drives
   + ff8162d01409db34893de98bd840a51c5f13e257:
     sh_configure.bzl: FreeBSD is also a known platform
   + 7092ed324137f03fcd34856bdb0595a1bdec3069:
     Remove unneeded exec_compatible_with from local_sh_toolchain
   + 57bc201346e61c62a921c1cbf32ad24f185c10c9:
     Do not autodetect C++ toolchain when
     BAZEL_DO_NOT_DETECT_CPP_TOOLCHAIN=1 is present
   + 35a78c09cf2fbfc3de9c124d2142e3d72aac4348:
     remote: recursively delete incomplete downloaded output
     directory.
   + 3c9cd82b847f3ece8ec04b2029bd5e8ad0eb7502:
     distfile: pack the archives needed later in the build
   + 27487c77387e457df18be3b6833697096d074eab:
     Slightly refactor SpawnAction to improve env handling
   + 1b333a2c37add9d04fe5bc5258ee4f73c93115e2:
     Fix Cpp{Compile,Link}Action environment and cache key computation
   + 3da8929963e9c70dff5d8859d6e988e6e7f4f9d7:
     Make SymlinkTreeAction properly use the configuration's
     environment
   + eca7b81cf8cc51e1fe56e5ed7d4ad5cd1668a17a:
     Add a missing dependency from checker framework dataflow to
     javacutils
   + 10a4de954c2061258d8222961fc3bd39516db49d:
     Release 0.14.0 (2018-06-01)
   + 4b80f2455e7e49a95f3a4c9102a67a57dad52207:
     Add option to enable Docker sandboxing.
   + 6b1635279e8b33dc1ac505ac81825e38f8797a14:
     Allow disabling the simple blob caches via CLI flag overrides.
```

Bug fix for [#5336](https://github.com/bazelbuild/bazel/issues/5336)
Bug fix fot [#5308](https://github.com/bazelbuild/bazel/issues/5308)

## Release 0.14.0 (2018-06-01)

```
Baseline: 5c3f5c9be7fa40d4fb3c35756891fab8483ca406

Cherry picks:
   + f96f037f8f77335dc444844abcc31a372a3e1849:
     Windows, Java launcher: Support jar files under different drives
   + ff8162d01409db34893de98bd840a51c5f13e257:
     sh_configure.bzl: FreeBSD is also a known platform
   + 7092ed324137f03fcd34856bdb0595a1bdec3069:
     Remove unneeded exec_compatible_with from local_sh_toolchain
   + 57bc201346e61c62a921c1cbf32ad24f185c10c9:
     Do not autodetect C++ toolchain when
     BAZEL_DO_NOT_DETECT_CPP_TOOLCHAIN=1 is present
   + 35a78c09cf2fbfc3de9c124d2142e3d72aac4348:
     remote: recursively delete incomplete downloaded output
     directory.
   + 3c9cd82b847f3ece8ec04b2029bd5e8ad0eb7502:
     distfile: pack the archives needed later in the build
   + 27487c77387e457df18be3b6833697096d074eab:
     Slightly refactor SpawnAction to improve env handling
   + 1b333a2c37add9d04fe5bc5258ee4f73c93115e2:
     Fix Cpp{Compile,Link}Action environment and cache key computation
   + 3da8929963e9c70dff5d8859d6e988e6e7f4f9d7:
     Make SymlinkTreeAction properly use the configuration's
     environment
   + eca7b81cf8cc51e1fe56e5ed7d4ad5cd1668a17a:
     Add a missing dependency from checker framework dataflow to
     javacutils
```

Incompatible changes:

  - Add --incompatible_disallow_legacy_javainfo flag.
  - Added flag --incompatible_disallow_old_style_args_add to help
    migrate from args.add() to args.add_all() / args.add_joined()
    where appropriate.

New features:

  - Bash,runfiles: use the new platform-independent library in
    `@bazel_tools//tools/bash/runfiles` to access runfiles
    (data-dependencies). See
    https://github.com/bazelbuild/bazel/blob/master/tools/bash/runfile
    s/runfiles.bash for usage information.
  - TemplateVariableInfo can now be constructed from Skylark.
  - The java_host_runtime_alias rule is now implemented in Java.

Important changes:

  - Flip default value of --experimental_shortened_obj_file_path to
    true, Bazel now generates short object file path by default.
  - Introduce fdo_profile rule that allows architecture-sensitive
    specification of fdo profiles.
  - canonicalize-flags no longer reorders the flags
  - CppRules: optional_compiler_flag was removed from CROSSTOOL, use
    features instead.
  - Labels of the form ////foo are disallowed.
  - The `/` operator is deprecated in favor of `//` (floor integer
    division).
      Try the `--incompatible_disallow_slash_operator` flag to ensure
    your code
      is forward-compatible.
  - Flip default value of --experimental_shortened_obj_file_path to
    true, Bazel now generates short object file path by default.
  - Exposed "mnemonic" and "env" fields on skylark "Action" objects.
  - Removed flag `--incompatible_disallow_toplevel_if_statement`.
  - Remove vestigial 'deps' and 'data' attributes from
    proto_lang_toolchain
  - Args objects (ctx.actions.args()) have new methods add_all() and
    add_joined() for building command lines using depsets.
  - `FileType` is deprecated and will be removed soon.
      Try the `--incompatible_disallow_filetype` flag to ensure your
    code
      is forward-compatible.
  - Introduce absolute_path_profile attribute that allows fdo_profile
    to accept absolute paths.
  - Support two-arg overloads for ctx.actions.args (eg.
    args.add("--foo", val))
  - Introduce 'tools' attribute to ctx.actions.run.
  - Fixed error message for proguard_apply_dictionary.
  - "bazel run" now lets one run interactive binaries. The
    BUILD_WORKSPACE_DIRECTORY and BUILD_WORKING_DIRECTORY environment
    variables indicate the working directory and the workspace root
    of the Bazel invocation. Tests are provided with an approximation
    of the official test environment.
  - repository rules are no longer restricted to return None.
  - Add --high_priority_workers flag.
  - CppRules: Feature configuration can be created from Skylark
  - Adds new-style JavaInfo provider constructor.
  - Make java_common.compile now uses java_toolchain javacopts by
    default; explicitly retrieving them using
    java_common.default_javac_opts is unnecessary.
  - CppRules: C++ command lines and env variables for C++ actions can
    be retrieved from feature configuration.
  - Skylark rule definitions may advertise providers that targets of
    the rule must propagate.
  - Bazel now supports running actions inside Docker containers.
    To use this feature, run "bazel build --spawn_strategy=docker
    --experimental_docker_image=myimage:latest".
  - Remote execution works for Windows binaries with launchers.
  - Fixing start/end lib expansion for linking. There were many cases
    where archive files were still being used with toolchains that
    support start/end lib. This change consolidates the places that
    make that decision so they can be more consistent.
  - Add support for reporting an error if
    android_test.binary_under_test contains incompatible versions of
    deps
  - We replaced the --experimental_local_disk_cache and
    --experimental_local_disk_cache_path flags into a single
    --disk_cache flag. Additionally, Bazel now tries to create the disk cache
    directory if it doesn't exist.
  - Save Blaze memory by not storing LinkerInput objects in
    LinkCommandLine
  - In the JavaInfo created by java_common.create_provider now
    includes both direct and transitive arguments in
    transitive_compile_time_jars and transitive_runtime_jars
  - Allow --worker_max_instances to take MnemonicName=value to
    specify max for each worker.
  - Allow java_toolchain.header_compiler to be an arbitrary executable

## Release 0.13.1 (2018-05-23)

```
Baseline: fdee70e6e39b74bfd9144b1e350d2d8806386e05

Cherry picks:
   + f083e7623cd03e20ed216117c5ea8c8b4ec61948:
     windows: GetOutputRoot() returns GetHomeDir()
   + fa36d2f48965b127e8fd397348d16e991135bfb6:
     Automated rollback of commit
     4465dae23de989f1452e93d0a88ac2a289103dd9.
   + 4abd2babcc50900afd0271bf30dc64055f34e100:
     Add error message on empty public resources
   + 2c957575ff24c183d48ade4345a79ffa5bec3724:
     test-setup: remove leading "./" from test name
   + e6eaf251acb3b7054c8c5ced58a49c054b5f23b1:
     Sort entries by segment when building a parent node to prevent
     unordered directory structures.
```

Important changes:

  - Remote Execution: Fixes a regression that produces directories with unsorted file/directory lists

## Release 0.13.0 (2018-04-30)

```
Baseline: fdee70e6e39b74bfd9144b1e350d2d8806386e05

Cherry picks:
   + f083e7623cd03e20ed216117c5ea8c8b4ec61948:
     windows: GetOutputRoot() returns GetHomeDir()
   + fa36d2f48965b127e8fd397348d16e991135bfb6:
     Automated rollback of commit
     4465dae23de989f1452e93d0a88ac2a289103dd9.
   + 4abd2babcc50900afd0271bf30dc64055f34e100:
     Add error message on empty public resources
   + 2c957575ff24c183d48ade4345a79ffa5bec3724:
     test-setup: remove leading "./" from test name
```

Incompatible changes:

  - Remove //tools/defaults:android_jar. Use
    @bazel_tools//tools/android:android_jar instead.
  - The flag --incompatible_show_all_print_messages is removed.
    Messages generated by `print` statements from any package will be
    displayed as
    DEBUG messages.
  - The --incompatible_disallow_uncalled_set_constructor flag is no
    longer available, the `set` constructor` is completely removed
    from Skylark.
    Use `depset` instead.
  - Variables PACKAGE_NAME and REPOSITORY_NAME are deprecated in
    favor of
      functions `package_name()` and `repository_name()`.

    https://docs.bazel.build/versions/master/skylark/lib/native.html#p
    ackage_name
  - BUILD_TIMESTAMP now contains seconds (and not milliseconds) since
    the epoch.

New features:

  - Strings have a new .elems() method, that provides an iterator on
    the characters of the string.
  - Now you can access three functions in windows_cc_configure.bzl by:
      load("@bazel_tools/tools/cpp:windows_cc_configure.bzl",
    "<function_name>")

Important changes:

  - CppRules: Unified action_configs for static libraries
  - Remove support for blaze dump --vfs. It is no longer meaningful.
  - Enable dependency checking for aar_import targets.
  - internal_bootstrap_hack has been deprecated and removed.
  - Properly handle tree artifacts on the link command line coming
    from a cc_library dependency.
  - Allow C++ features to make proto_library emit smaller C++ code
  - The 'j2objc' configuration fragment is exposed to Skylark.
  - Remove the default content of the global bazelrc.
  - In int() function, do not auto-detect base if input starts with
    '0'.
  - Users can now pass --experimental_shortened_obj_file_path=true to
    have a shorter object file path, the object file paths (and all
    other related paths) will be constructed as following:
    If there's no two or more source files with the same base name:

    <bazel-bin>/<target_package_path>/_objs/<target_name>/<source_base
    _name>.<extension>
    otherwise:

    <bazel-bin>/<target_package_path>/_objs/<target_name>/N/<source_ba
    se_name>.<extension>
      N = the file?s order among the source files with the same
    basename, starts from 0.
  - Move (c/cxx)opts from legacy_compile_flags to user_compile_flags
  - CppRules: Remove optional_*_flag fields from CROSSTOOL, they are
    not
    used, and could be expressed using features.
  - Introduce --incompatible_disable_objc_provider_resources to turn
    off all resource-related fields of the Objc provider.
  - Removed the statement of "What does Bazel support?" as it's
    limiting/misleading. Added supported host OSes to
    "multi-platform" paragraph.
  - android_library AAR output now contains proguard.txt
  - Bazel now displays information about remote cache hits and
    execution strategies used in its UI after every build and test,
    and adds a corresponding line "process stats" to BuildToolLogs in
    BEP.
  - Print correct build result for builds with --aspects flag.
  - android_binary.manifest_merger is no longer supported.

## Release 0.12.0 (2018-04-11)

```
Baseline: b33e5afa313322a7048044c44d854cbb666b988e

Cherry picks:
   + 369409995bd75eeb0683fd24f7585d2a90320796:
     Automated rollback of commit
     c2b332b45e6ea41a14ecbd3c5f30782bcdeec301.
   + dbf779869751cc893ba240402d352c6e70be2978:
     Emit SJD errors even if we don't know the label of a dependency
   + 4c3098cfa6f00f90c7530b6f40d3e93062931c1d:
     Android tools: remove mtime-modifications
   + a1068c44a700ec2cff84cbd12592e9bfea25d754:
     NDK cc_toolchains: include bundled runtime libraries in
     cc_toolchain.all_files
   + b1be5816ec1bf8e1172c1bed4f29b4e6c6bb7202:
     runfiles,Python: remove library from @bazel_tools
   + 0a4622012ff796429220fe57d3217f262cc208a8:
     Fix visibility of def_parser for remote builds
   + 3c5373c50c7c492842f8a468906eda2c0bc90787:
     Remove visibility attribute from
     //third_party/def_parser:def_parser
   + f54d7e5293cc40ce3507a9adef530e46ab817585:
     Enable bulk writes in the HttpBlobStore
   + 04ce86e8ba96630f89a436167b7f3a195c5e50e7:
     remote/http: properly complete user promise
```

Incompatible changes:

  - The order of dict-valued attributes is now the order in the BUILD
    file (or in the Skylark dict they were created from) and not
    lexicographically sorted.

New features:

  - The new "--direct_run" flag on "blaze run" lets one run
    interactive binaries.
  - "blaze run --direct_run" with tests now gives the test an
    approximation of the official test environment.
  - "blaze run --direct_run" now exports the
    BUILD_{WORKSPACE,WORKING}_DIRECTORY variables to tell the binary
    about the cwd of the client and the workspace root.
  - New Android device test rule: android_instrumentation_test.
  - Add option to dump the action graph to a file: 'bazel dump
    --action_graph=/path/to/file'.
  - Pass `tags` from `java_import_external` rule to the generated
    `java_import` rule.
  - blaze query: use --proto:output_rule_attrs to filter for given
    attributes
  - Added Android NDK r15 support, including compatibility with
    Unified Headers.
  - Adds --ltobackendopt and --per_file_ltobackendopt for passing
    options to ThinLTO LTO backend compile actions only.

Important changes:

  - Fix how libraries to link is specified to archiver actions.
  - Fix how libraries_to_link are expanded in the archiver command
    line.
  - stop using --no-locals in android coverage builds
  - apple_binary can now generate dSYM outputs with the
    --apple_generate_dsym=true flag.
  - Fix FDO_STAMP_MACRO to only be set when fdoBuildStamp is not null.
  - Improved clarity of warning message for unsupported NDK revisions.
  - Add lint check for discouraging glob(["**/*.java"])
  - unifly lint glob(["**/*.java"]) message
  - Removed flags `--incompatible_checked_arithmetic`,
    `--incompatible_dict_literal_has_no_duplicates`,
    `--incompatible_disallow_keyword_only_args`, and `
    --incompatible_comprehension_variables_do_not_leak`.
  - Add "proto_source_root" flag to proto_library.
  - Updated default android_cpu value to armeabi-v7a
  - In skylark, print(target) now shows the provider keys of a
    target, as debug information.
  - The native http_archive rule is deprecated. Use the
      Skylark version available via
    load("@bazel_tools//tools/build_defs/repo:http.bzl",
    "http_archive")
      instead.
  - flaky_test_attempts supports the regex@attempts syntax, like
    runs_per_test.
  - Fixed include paths for NDK r13+ llvm-libc++ headers to
    `ndk/sources/cxx-stl/llvm-libc++/include` and
    `ndk/sources/cxx-stl/llvm-libc++abi/include`
  - --config flags now expand in place by default.
  - aar_import now sets java.transitive_exports.
  - repository_cache is no longer experimental and enabled by default.
  - BAZEL_LINKOPTS is now consulted when autoconfiguring c++ toolchain
  - The native git_repository rule is deprecated. Use the
      Skylark version available via
    load("@bazel_tools//tools/build_defs/repo:git.bzl",
    "git_repository")
      instead.
  - Removed flag `--incompatible_load_argument_is_label`.
  - CcToolchain: Introduced action_config for
    "c++-link-transitive-dynamic-library"
  - Use bazel dump --action_graph=/path/to/action.proto
    --action_graph:targets://foo:bar,//foo:foo to filter for certain
    targets in the action graph dump.
  - Added Android NDK r16 support. Use --cxxopt='-std=c++11` compile
    with the C++11 standard, and
    `--android_crosstool_top=@androidndk//:toolchain-libcpp` to use
    the `libc++` STL.
  - Add a --build_event_publish_all_actions flag to allow all actions
    to be published via the BEP.
  - C++: Introduced --experimental_drop_fully_static_linking_mode
  - Removed cc_inc_library, please use cc_library instead
  - CppRules: cc_binary/cc_test now enable 'static_linking_mode' or
    'dynamic_linking_mode'.

## Release 0.11.1 (2018-03-06)

```
Baseline: 00d781ae78a8bd51d3c61b621d79f0bb095aff9e

Cherry picks:
   + ea2d4c475febdbd59ca0e0ba46adc7be759f84e0:
     Update stub_finds_runfiles_test to be a real sh_test.
   + d855d8133f4efb73ebd5e82c54a9afb4c7565d46:
     java,runfiles: fix bugs in runfiles library
   + 56aeb04a064218b845ecc193d530c341c6ec854d:
     Fixing #4585: broken re-execution of orphaned actions.
   + cf3f81aef7c32019d70cbce218a64a03276268f0:
     remote: Add support for HTTP Basic Auth
   + 28bd997c1c8793973f63dcae4c22bbae49e7d8b7:
     Fixing test-setup.sh occasionally missing stdout/stderr, on
     systems where "tail --pid" is supported.
   + 109e4b4dc9e786e3a2d8d7cb245d18320dbe9216:
     Automated rollback of commit
     7e6837cc1d1aa4259f5c27ba3606b277b5f6c3e9.
   + b3d52b1b6d46a0f23cc91125c1d522e9d13433b4:
     Fix incorrect include directories when -no-canonical-prefixes is
     passed to clang
   + 1001141f0674ff4b611814edcb00a5183680ef4a:
     Roll forward of
     https://github.com/bazelbuild/bazel/commit/3904ac33a983fd8faebba1
     b52bcac5a3ff942029
     (https://github.com/bazelbuild/bazel/commit/3904ac33a983fd8faebba
     1b52bcac5a3ff942029). Fix #4625 by running the test process in a
     sub-shell.
   + fc98b44b6181fa4c3efd8613d887970629468d74:
     android,windows: bugfix in aar_resources_extractor
```

Important changes:

  - Fixes regression building Android rules on Windows.

## Release 0.11.0 (2018-02-23)

```
Baseline: 00d781ae78a8bd51d3c61b621d79f0bb095aff9e

Cherry picks:
   + ea2d4c475febdbd59ca0e0ba46adc7be759f84e0:
     Update stub_finds_runfiles_test to be a real sh_test.
   + d855d8133f4efb73ebd5e82c54a9afb4c7565d46:
     java,runfiles: fix bugs in runfiles library
   + 56aeb04a064218b845ecc193d530c341c6ec854d:
     Fixing #4585: broken re-execution of orphaned actions.
   + cf3f81aef7c32019d70cbce218a64a03276268f0:
     remote: Add support for HTTP Basic Auth
   + 28bd997c1c8793973f63dcae4c22bbae49e7d8b7:
     Fixing test-setup.sh occasionally missing stdout/stderr, on
     systems where "tail --pid" is supported.
   + 109e4b4dc9e786e3a2d8d7cb245d18320dbe9216:
     Automated rollback of commit
     7e6837cc1d1aa4259f5c27ba3606b277b5f6c3e9.
   + b3d52b1b6d46a0f23cc91125c1d522e9d13433b4:
     Fix incorrect include directories when -no-canonical-prefixes is
     passed to clang
   + 3904ac33a983fd8faebba1b52bcac5a3ff942029:
     Automated rollback of commit
     28bd997c1c8793973f63dcae4c22bbae49e7d8b7.
   + 1001141f0674ff4b611814edcb00a5183680ef4a:
     Roll forward of
     https://github.com/bazelbuild/bazel/commit/3904ac33a983fd8faebba1
     b52bcac5a3ff942029
     (https://github.com/bazelbuild/bazel/commit/3904ac33a983fd8faebba
     1b52bcac5a3ff942029). Fix #4625 by running the test process in a
     sub-shell.
```

Incompatible changes:

  - ctx.fragments.jvm is not available anymore.

New features:

  - java,runfiles: You can now depend on
    `@bazel_tools//tools/runfiles:java-runfiles` to get a
    platform-independent runfiles library for Java. See JavaDoc of
    https://github.com/bazelbuild/bazel/blob/master/src/tools/runfiles
    /java/com/google/devtools/build/runfiles/Runfiles.java for usage
    information.

Important changes:

  - The --[no]experimental_disable_jvm command line option is not
    supported anymore.
  - Allow expanding TreeArtifacts for libraries_to_link
  - Proguarded Android binaries can be built with incremental dexing.
  - aar_import now supports assets.
  - Crash in OutputJar::Close has been fixed
  - generator_* attributes are nonconfigurable.
  - Introduces --[no]keep_state_after_build
  - Add support for merged object files needed for -flto-unit.
  - Fix how libraries to link is specified to archiver actions.
  - Replace //tools/defaults:android_jar with
    @bazel_tools//tools/android:android_jar.
    //tools/defaults:android_jar will be removed in a future release.
  - java_common.compile supports neverlink
  - Resolved an issue where a failure in the remote cache would not
    trigger local re-execution of an action.

## Release 0.10.1 (2018-02-15)

```
Baseline: 22c2f9a7722e8c8b7fdf8f5d30a40f1c4118e993

Cherry picks:
   + f6ca78808722c8c119affdb33400838ee92d44b6:
     isable_presubmit
   + 65c13dd5a4c1b4b5a072f7680b8f1cf3c5079b52:
     Fix StreamResourceLeak error
   + e5436745e1732f5e43fc55f0deb5b19e23ce8524:
     windows: fix --symlink_prefix=/ throwing exception
   + 22ccdd1ebe1dc495e05d894a3325f6b05e681fb3:
     Fix turbine command lines with empty javacopts
   + 96c654d43eb2906177325cbc2fc2b1e90dbcc792:
     Remove EOL'd Linux flavours, bump CentOS to 6.9.
   + f0bec36864f10370cbbda4caa8beac2e0c5ee45b:
     Automated rollback of commit
     2aeaeba66857c561dd6d63c79a213f1cabc3650d.
   + 860af5be10b6bad68144d9d2d34173e86b40268c:
     Consolidate Error Prone resource handling
   + 2e631c99495f75270d2639542cefb531ec262d67:
     sandbox: properly add `tmpDir` to `writablePaths`
   + 5bfa5844d0d16d71e88002956e88402bfec88ef7:
     actions,temp: respect TMPDIR envvar
   + 6cc2ad8676d1ae0542b351a07a05ddbe5efac165:
     sandbox: add env[TMPDIR] instead of `tmpDir`
   + 40c757f4ab90214f95935672532a495c4551490a:
     Change git clone to pull all history, so all needed commits can
     be accessed.
   + 56aeb04a064218b845ecc193d530c341c6ec854d:
     Fixing #4585: broken re-execution of orphaned actions.
```

Important changes:

  - Resolved an issue where a failure in the remote cache would not
    trigger local re-execution of an action.

## Release 0.10.0 (2018-02-01)

```
Baseline: 22c2f9a7722e8c8b7fdf8f5d30a40f1c4118e993

Cherry picks:
   + f6ca78808722c8c119affdb33400838ee92d44b6:
     isable_presubmit
   + 65c13dd5a4c1b4b5a072f7680b8f1cf3c5079b52:
     Fix StreamResourceLeak error
   + e5436745e1732f5e43fc55f0deb5b19e23ce8524:
     windows: fix --symlink_prefix=/ throwing exception
   + 22ccdd1ebe1dc495e05d894a3325f6b05e681fb3:
     Fix turbine command lines with empty javacopts
   + 96c654d43eb2906177325cbc2fc2b1e90dbcc792:
     Remove EOL'd Linux flavours, bump CentOS to 6.9.
   + f0bec36864f10370cbbda4caa8beac2e0c5ee45b:
     Automated rollback of commit
     2aeaeba66857c561dd6d63c79a213f1cabc3650d.
   + 860af5be10b6bad68144d9d2d34173e86b40268c:
     Consolidate Error Prone resource handling
   + 2e631c99495f75270d2639542cefb531ec262d67:
     sandbox: properly add `tmpDir` to `writablePaths`
   + 5bfa5844d0d16d71e88002956e88402bfec88ef7:
     actions,temp: respect TMPDIR envvar
   + 6cc2ad8676d1ae0542b351a07a05ddbe5efac165:
     sandbox: add env[TMPDIR] instead of `tmpDir`
   + 40c757f4ab90214f95935672532a495c4551490a:
     Change git clone to pull all history, so all needed commits can
     be accessed.
```

Incompatible changes:

  - In order to access the template variables $(JAVA) and
    $(JAVABASE), @bazel_tools//tools/jdk:current_java_runtime needs
    to be added to the toolchains= attribute from now on.
  - The ctx.middle_man function is not supported anymore.
  - The flag --incompatible_list_plus_equals_inplace is removed, its
    default behavior is preserved. += on lists now always mutates the
    left hand
    side.
  - --android_sdk no longer supports filegroup targets.
  - android_* rules no longer support legacy_native_support attribute.

New features:

  - query: Add option --noproto:flatten_selects to turn off
    flattening of selector lists in proto output.
  - New android test rule, android_local_test.

Important changes:

  - The --remote_rest_cache flag now respects --remote_timeout.
  - --experimental_java_coverage is available for testing.
  - The deprecated builtin `set` is no longer allowed even from within
    unexecuted code in bzl files. It's temporarily possible to use
    --incompatible_disallow_uncalled_set_constructor=false if this
    change causes
    incompatibility issues.
  - Linkstamping is now a separate and full-blown CppCompileAction,
    it's
    no longer a part of linking command.
  - Using `+`, `|` or `.union` on depsets is now deprecated. Please
    use the new
      constructor instead (see
    https://docs.bazel.build/versions/master/skylark/depsets.html).
  - config_feature_flag's default_value is optional. It is
    only an error to have a config_feature_flag with no default_value
    if that config_feature_flag has not been set in the configuration
    it is being evaluated in.
  - --[no]keep_incrementality_data is gone, replaced by the
    enum-valued --incremental_state_retention_strategy
  - Linkstamping is now a separate and full-blown CppCompileAction,
    it's
    no longer a part of linking command.
  - Added --checkHashMismatch flag to ZipFilterAction. Valid values
    are IGNORE, WARN and ERROR. --errorOnHashMismatch is deprecated,
    please use this flag instead.
  - Set build jobs equivalent to number of logical processors by
    default. Should improve build times significantly.
  - Added --(no)expand_test_suites flag.
  - Rename --keep_incrementality_data to --track_incremental_state
  - --remote_rest_cache was renamed to --remote_http_cache. Both
    options keep working in this release, but --remote_rest_cache
    will be
    removed in the next release.
  - Aspects-on-aspect see and propagate over aspect attributes.
  - --auth_* flags were renamed to --google_* flags. The old names
    will continue to work for this release but will be removed in the
    next
    release.
  - Remote Caching and Execution support output directories.
  - Remove defunct flags
    --experimental_incremental_dexing_for_lite_proto and
    --experimental_incremental_dexing_error_on_missed_jars that have
    long been enabled by default
  - New version of aapt2 and Resources.proto.
  - Make PIC and non PIC outputs for C++ compilation with Tree
    Artifacts

## Release 0.9.0 (2017-12-19)

```
Baseline: ddd5ac16aeffa6c4693c348f73e7365240b1abc5

Cherry picks:
   + 2cf560f83922e6df9626ba3ee063c1caf6797548:
     Update version of re2
   + a2d2615362c65be98629b39ce39754a325ed1c42:
     Check for null build file returned from getBuildFileForPackage.
   + 68c577afc2fb33b5e66b820bcc9043fed1071456:
     Fix some broken targets and failing tests.
   + 766ba8adc4487f17ebfc081aeba6f34b18b53d6c:
     Automated rollback of commit
     337f19cc54e77c45daa1d5f61bf0a8d3daf8268f.
   + a22d0e9c14e58b29d81f5a83bdcc6e5fce52eafe:
     Fix: uploading artifacts of failed actions to remote cache
     stopped working.
   + 03964c8ccb20d673add76c7f37245e837c3899b6:
     [java_common.compile] Name output source jar relative to the
     output jar name
```

Incompatible changes:

  - The deprecated `set` constructor is removed, along with the
    migration flag --incompatible_disallow_set_constructor. It is
    still temporarily
    allowed to refer to `set` from within unexecuted code.
  - The flag --incompatible_disallow_set_constructor is no longer
    available, the deprecated `set` constructor is not available
    anymore.
  - The path to the JVM executable is not accessible anymore as
    ctx.{fragments,host_fragments}.jvm.java_executable. Use
    JavaRuntimeInfo.java_executable_exec_path instead.
  - --clean_style is no longer an option.

New features:

  - Users can use win_def_file attribute to specify a DEF file for
    exporting symbols when build a shared library on Windows.
  - Add --experimental_android_resource_cycle_shrinking option to
    allow for more aggressive code and resource shrinking.

Important changes:

  - Late-bound attributes are exposed to skylark. This is a new API
    (`configuration_field()`) to depend on certain
    configuration-defined targets from skylark rules.
  - Document interaction between test_suite and target exclusions
  - AAR manifest files will come from the processed resource APK if it
    exists.
    RELNOTES: None for Blaze users.
  - Document interaction between test_suite and target exclusions
  - --keep_incrementality_data flag allows Bazel servers to be run in
    memory-saving non-incremental mode independent of --batch and
    --discard_analysis_cache.
  - Add deps attribute to Skylark maven_aar and maven_jar workspace
    rules.
  - Use --expand_configs_in_place as a startup argument to change the
    order in which --config expansions are interpreted.
  - SOURCE_DATE_EPOCH
    (https://reproducible-builds.org/specs/source-date-epoch/) can
    be used to override the timestamp used for stamped target (when
    using --stamp).
  - Package specifications can now be prefixed with `-` to indicate
    negation
  - transitive_source_jars is now exposed on JavaInfo.
  - Add six to deps of has_services=1 py_proto_librarys.
  - java_tests no complain when use_testrunner is explicitly set to 1
    and main_class is set.
  - transitive_source_jars is now exposed on JavaInfo.
  - Debug messages generated by `print()` are not being filtered out
    by --output_filter anymore, it's recommended not to use them in
    production code.
  - in the Label() function, relative_to_caller_repository is now
    deprecated.
  - java_tests no complain when use_testrunner is explicitly set to 1
    and main_class is set.
  - Bazel's default hash function was changed from MD5 to SHA256.
    In particular, this affects users of remote caching and
    execution, as
    all hashes will be SHA256 by default.
  - Remove redirects for domains be.bazel.build and cr.bazel.build
    from the source for docs.bazel.build (because those subdomains
    don't resolve here; they resolve to bazel.build, which has the
    redirects for them)
  - First argument of 'load' must be a label. Path syntax is removed.
      (label should start with '//' or ':').
  - Document startup option --host_javabase
  - The --host_platform and --platform flags are no longer
    experimental.

## Release 0.8.0 (2017-11-27)

```
Baseline: cff0dc94f6a8e16492adf54c88d0b26abe903d4c

Cherry picks:
   + 8a49b156c4edf710e3e1e0acfde5a8d27cc3a086:
     Fix ImportError on tools.android for junction_lib
   + 275ae45b1228bdd0f912c4fbd634b29ba4180383:
     Automated rollback of commit
     4869c4e17d5b1410070a1570f3244148d8f97b5d.
   + d0bf589f2716b3d139c210930371a684c6e158eb:
     Add a random number to action temp dir
   + 9738f35abddb7ef7a7ef314b5d2a52a3be1b830a:
     CcProtoLibrary: Don't add dynamic librarys to filesToBuild on
     Windows
   + 0d6ff477099fdf6c8c1c7d4e2104f9184afe0a2b:
     Automated rollback of commit
     0ebb3e54fc890946ae6b3d059ecbd50e4b5ec840.
```

Incompatible changes:

  - ctx.fragments.apple.{xcode_version,ios_minimum_os} is not
    supported anymore. The same information is accessible through the
    target @bazel_tools//tools/osx:current_xcode_config: point an
    implicit attribute to it (i.e.
    attr.label(default=Label("@bazel_tools//tools/osx:current_xcode_co
    nfig")) then use
    ctx.attr._xcode_config[apple_common].XcodeVersionConfig].
  - ctx.fragments.apple.minimum_os_for_platform_type is not supported
    anymore. The same information is accessible through the target
    @bazel_tools//tools/osx:current_xcode_config: point an implicit
    attribute to it (i.e.
    attr.label(default=Label("@bazel_tools//tools/osx:current_xcode_co
    nfig")) then use
    ctx.attr._xcode_config[apple_common].XcodeVersionConfig].minimum_o
    s_for_platform_type .
  - ctx.fragments.apple.sdk_version_for_platform is not supported
    anymore. The same information is accessible through the target
    @bazel_tools//tools/osx:current_xcode_config: point an implicit
    attribute to it (i.e.
    attr.label(default=Label("@bazel_tools//tools/osx:current_xcode_co
    nfig")) then use
    ctx.attr._xcode_config[apple_common].XcodeVersionConfig].sdk_versi
    on_for_platform .
  - --javabase=<absolute path> and --host_javabase=<absolute path>
    are not supported anymore. If you need this functionality
    java_runtime_suite(name="suite", default=":runtime")
    java_runtime(name="runtime", java_home=<path to the JDK>) is an
    alternative.
  - The flag --incompatible_descriptive_string_representations is no
    longer available, old style string representations of objects are
    not supported
    anymore.
  - The flag --incompatible_disallow_set_constructor is no longer
    available, the deprecated `set` constructor is not available
    anymore.
  - += on lists now mutates them. `list1 += list2` is now equivalent
    to `list1.extend(list2)` and not equivalent to `list1 = list1 +
    list2` anymore.
  - the target_apple_env and apple_host_system_env methods on
    ctx.fragments.apple are not supported anymore. The same
    information is accessible through apple_common.target_apple_env
    and apple_common.apple_host_system_env . They need the Xcode
    configuration as an argument, which can be obtained by declaring
    an implicit dependency on it (i.e.
    attr.label(default=Label("@bazel_tools//tools/osx:current_xcode_co
    nfig")) and then calling e.g.
    apple_common.apple_host_system_env(ctx.attr._xcode_config[apple_co
    mmon.XcodeVersionConfig]).
  - C++ toolchain identifiers are not in the name of the output
    directory anymore.
  - Selecting on "xcode_version" and
    "{ios,tvos,macos,watchos}_sdk_version" is not supported anymore.
    What was config_setting(values={"$FOO_version": $VALUE}) is now
    config_setting(flag_values={"@bazel_tools//tools/osx:$FOO_version_
    flag": $VALUE}).
  - Selecting on "xcode_version" and
    "{ios,tvos,macos,watchos}_sdk_version" is not supported anymore.
    What was config_setting(values={"$FOO_version": $VALUE}) is now
    config_setting(flag_values={"@bazel_tools//tools/osx:$FOO_version_
    flag": $VALUE}).
  - The flag --incompatible_disallow_set_constructor is no longer
    available, the deprecated `set` constructor is not available
    anymore.
  - Selecting on "xcode_version" and
    "{ios,tvos,macos,watchos}_sdk_version" is not supported anymore.
    What was config_setting(values={"$FOO_version": $VALUE}) is now
    config_setting(flag_values={"@bazel_tools//tools/osx:$FOO_versi...

New features:

  - runfiles, sh: Shell scripts may now depend on
    //src/tools/runfiles:runfiles_sh_lib and source runfiles.sh. The
    script defines the `rlocation` function which returns runfile
    paths on every platform.
  - In addition to $(location), Bazel now also supports $(rootpath)
    to obtain
        the root-relative path (i.e., for runfiles locations), and
    $(execpath) to
        obtain the exec path (i.e., for build-time locations)

Important changes:

  - android_binary now supports custom debug keys via the debug_key
    attribute.
  - Updated Android proguard to 5.3.3. It now works with android-24+.
  - --experimental_use_parallel_android_resource_processing and
    --experimental_android_use_nocompress_extensions_on_apk are
    removed. These features are fully rolled out.
  - Fixes #2574
  - Fixes #3834
  - Enable experimental UI by default.
  - .
    RELNOTES: None.
    RELNOTES: No.
  - Add memory profiler.
  - [Bazel] {java,cc}_proto_library now look for dependencies in
    @com_google_protobuf, instead of in @com_google_protobuf_$LANG
  - Improved merge.sh script in cookbook.
  - Fixing regression to --experimental_remote_spawn_cache
  - Support for linker scripts in NativeDepsHelper (e.g.,
    android_binary)
  - Skylark semantics flags now affect WORKSPACE files and repository
    rules.
  - ctx.outputs.executable is deprecated. Use DefaultInfo(executable
    = ...) instead.
  - Update "mirror.bazel.build" urls to use https.
  - Improve --config logging when --announce_rc is present.
  - Document interaction between test_suite and target exclusions
  - Replace version numbers for Bazel installers with "<version>"
    (because this will change often)
  - Published command lines should have improved lists of effective
    options.
  - --incremental_dexing_binary_types has been removed. All builds
    are supported by incremental dexing (modulo proguard and some
    blacklisted dx flags).
  - Document --host_javabase, --host_java_toolchain

## Release 0.7.0 (2017-10-18)

```
Baseline: 5cc6246d429f7d9119b97ce263b4fd6893222e92

Cherry picks:
   + e79a1107d90380501102990d82cbfaa8f51a1778:
     Windows,bootstrapping: fix build_windows_jni.sh
```

Incompatible changes:

  - The --output=location flag to 'bazel query' cannot be used with
    query expressions that involve the 'buildfiles' or 'loadfiles'
    operators. This also applies to 'genquery' rules.
  - Operators for equality, comparison, 'in' and 'not in' are no
    longer associative,
      e.g.  x < y < z  is now a syntax error. Before, it was parsed
    as:  (x < y) < z.
  - In strings, octal sequences greater than \377 are now forbidden
    (e.g. "\\600").
      Previously, Blaze had the same behavior as Python 2, where
    "\\450" == "\050".
  - Using tabulation for identation is now fobidden in .bzl files
  - `load` is now a language keyword, it cannot be used as an
    identifier
  - lvalues must have define at least one variable (i.e. we forbid
    `[] = f()`).
  - Fixed a bug whereby multiple load() statements could appear on
    the same line
  - -extra_checks:off is no longer supported; use
    -XepDisableAllChecks instead
  - java_common.java_toolchain_attr is removed. Depend on the
    java_toolchain_alias() rule to accomplish the same thing.
  - cc_common.cc_toolchain_attr and java_common.java_runtime_attr are
    not supported anymore and were replaced with the
    cc_toolchain_alias() and java_runtime_alias() rules.
  - Noop flag --deprecated_generate_xcode_project deleted.
  - Objects in Skylark are converted to strings in a more descriptive
    and less harmful way (they don't leak information that shouldn't
    be accessed by Skylark code, e.g. nondeterministic memory addresses
    of objects).
  - `set` is deprecated in BUILD and .bzl files, please use `depset`
    instead. Ordering names have also been changed, please use "default",
    "postorder", "preorder", and "topological" instead of "stable",
    "compile", "naive_link", and "link" correspondingly.
  - Integer overflow (on signed 32 bit numbers) in BUILD/bzl files is
    an error.
  - Keyword-only syntax in a function definition is now forbidden
      e.g. `def foo(a, *, b)` or `def foo(a, *b, c)`
  - --incompatible_comprehension_variables_do_not_leak defaults to
    "true."
      Iteration variable becomes inaccessible after a list/dict
    comprehension.
  - @bazel_tools//tools/build_defs/docker:docker.bzl is no longer
    available, please see https://github.com/bazelbuild/rules_docker.

New features:

  - Zipped LLVM profiles are now supported.
  - LIPO maps to ThinLTO for LLVM builds.
  - Change to handle LLVM FDO zipped profile contents correctly.
  - Do not disable fully dynamic linking with ThinLTO when invoked
    via LIPO options.
  - There is now a 'siblings' query function. See the query
    documentation for more details.
  - Added the print_action command, which outputs the
    actions needed to build a given target in the form of an
    ExtraActionSummary proto in text format.
  - android_binary now supports proguard_apply_dictionary to specify
    a custom dictionary to use for choosing names to obfuscate
    classes and members to.

Important changes:

  - Windows: bazel clean --expunge works
  - First argument of 'load' should be a label. Path syntax is
    deprecated (label should start with '//' or ':').
  - Octal prefix '0' is deprecated in favor of '0o' (use 0o777
    instead of 0777).
  - The extension_safe attribute of apple_binary no longer validates
    transitive dependencies are compiled against extension_safe APIs.
  - Parentheses around the tuple are now mandatory in [a for b in c
    if 1, 2]
  - Adjust the thresholds for --test_verbose_timeout_warnings so that
    it can recommending timeout increases and won't recommend
    timeouts that are too close to the actual timeout.
  - Iterating on a `depset` object is deprecated. If you need an
    iterable, call the `.to_list()` method first.
  - Bazel now uses tools from action_configs in Crosstool by default
    (as oposed to using top level tools).
  - Incremental dexing errors on combination of --multidex=off and
    either --main-dex-list or --minimal-main-dex.
  - When using the dictionary literal syntax, it is now an error to
    have duplicated keys (e.g.  {'ab': 3, 'ab': 5}).
  - New property on android_sdk: aapt2
      Choose the version of aapt on android_binary
  - Add idl_preprocessed attribute to android_library, so that
    preprocessed aidl files can be passed to android_library for
    compiling
  - Bazel's remote_worker backend for remote execution supports
    sandboxing on Linux now. Check
    https://github.com/bazelbuild/bazel/blob/master/src/tools/remote_w
    orker/README.md for details.
  - Allows flags that expand to take values.
  - Make querying attributes formed by selector lists of list types
    more efficient by no longer listing every possible combination of
    attribute value but by more compactly storing the possible values
    of the list.
  - writing build events to a file is no longer experimental
  - set --rewrite_calls_to_long_compare to false by default.
  - ObjC and C++ coverage feature is unified under name 'coverage'
  - Enable --incremental_dexing for Android builds by default. Note
    that some dexopts are incompatible with incremental dexing,
    including --force-jumbo.
  - Evaluation will soon use checked arithmetics and throw an error
    instead of overflow/underflow.
  - Implicit iteration in the CROSSTOOL has been removed, use
    explicit 'iterate_over' message.
  - Add option for Android specific grte_top
  - Crosstool patches are only applied if the toolchain doesn't define
    'no_legacy_features' feature.
  - 'platform_type' is now a mandatory attribute on apple_binary and
    apple_static_library rules.
    If this change breaks your build, feel free to add platform_type
    = 'ios' to any apple_binary and apple_static_library
    targets in your project, as this was the previous default
    behavior.
  - Remove apple_watch2_extension build rule. Users should be using
    the skylark watchos_application and watchos_extension rules.
    https://github.com/bazelbuild/rules_apple has details.
  - Check stderr to detect if connected to a terminal.  Deprecate
    --isatty.
  - Commands that shut down the server (like "shutdown") now ensure
    that the server process has terminated before the client process
    terminates.
  - Remove apple_watch1_extension and apple_watch_extension_binary
    rules. Users should be using the skylark watchos_application and
    watchos_extension rules.
    https://github.com/bazelbuild/rules_apple has details.
  - Windows: Wrapper-less CROSSTOOL becomes default now.
    set USE_MSVC_WRAPPER=1 if you still want to use wrapper script.
  - Ignore --glibc in the Android transition.
  - Remove --experimental_android_use_singlejar_for_multidex.
  - nocopts now also filter copts
  - 'strip' action is now configured via feature configuration
  - The Build Event Service (BES) client now properly supports
    Google Applicaton Default Credentials.
  - Flags from action_config get added first to the command line
    first, before the flags from features.
  - update dexing tools to Android SDK 26.0.1
  - Bazel Android support now requires build-tools 26.0.1 or later.
  - `bazel info output_path` no longer relies on the root directory
    filename being equal to the workspace name.
  - The `print` function now prints debug messages instead of
    warnings.
  - speedup of incremental dexing tools
  - --announce_rc now controls whether bazelrc startup options are
    printed to stderr.
  - Removing a few unused objc_provider keys.
  - Improved logging when workers have to be restarted due to its
    files having changed.
  - Top-level `if` statements are now forbidden.
  - Java protos are compiled to Java 7 bytecode.
  - All Android builds now use the desugar tool to support some Java
    8 features by default. To disable, use the --nodesugar_for_android flag.
  - Skylark-related options may now appear as "common" command
    options in the .bazelrc
  - Python is now required to build bazel.
  - New --build_runfile_manifests flag controls production of
    runfiles manifests.
  - Enable debug info for Java builds
  - Allow java_lite_proto_library in the deps of android rules.
  - .so files in APKs will be memory-page aligned when
    android_binary.nocompress_extensions contains ".so" and
    --experimental_android_use_nocompress_extensions_on_apk is
    specified.
  - Skylark providers can specify allowed fields and their
    documentation.
  - Support ctx.actions.args() for more efficient Skylark command
    line construction.
  - The remote HTTP/1.1 caching client (--remote_rest_cache) now
    distinquishes between action cache and CAS. The request URL for
    the action cache is prefixed with 'ac' and the URL for the CAS
    is prefixed with 'cas'.
  - `JavaInfo` is a preferred alias to `java_common.provider`.
  - J2ObjC version updated to 2.0.3.
  - A new Java coverage implementation is available. Makes possible
    coverage for Skylark JVM rules.
  - Make proguard_apply_dictionary also apply to class and package
    obfuscation, not just class members.
  - android_binary.nocompress_extensions now applies to all files in
    the APK, not just resources and assets.
  - The apple_genrule rule that is distributed with Bazel has been
    deleted. Users who wish to use genrules with Xcode's
    DEVELOPER_DIR set should use the rules in
    https://github.com/bazelbuild/rules_apple instead.
  - The swift_library rule that is distributed with Bazel has been
    deleted. Users who wish to compile Swift should use the rules in
    https://github.com/bazelbuild/rules_apple instead.
  - The Build Event Protocol's File.uri field is now properly
    encoded according to RFC2396.
  - Deprecated: Using the android_library.deps attribute to
    implicitly export targets to dependent rules. If your code is
    using this feature, Bazel will raise a warning. To fix, please
    use android_library.exports to explicitly specify exported
    targets. Run with
    --experimental_allow_android_library_deps_without_srcs=false to
    ensure forward compatibility when this feature is removed in a
    future release.
  - java_common.create_provider is now supported with creating ijars
    by default. This introduces incompatibilities for existing users.
    Please set use_ijar=False if you don't want to use ijars.
  - Tests can now write files to TEST_UNDECLARED_OUTPUTS_DIR and
    TEST_UNDECLARED_OUTPUTS_ANNOTATIONS_DIR and these will be
    reflected under bazel-testlogs.
  - remove unused --host_incremental_dexing flag
  - Stop using --undefined dynamic_lookup in Apple links. Enables
    unresolved symbol errors.
  - All test output files included for cached, uncached, and multiple
    attempt tests.
  - Android rules no longer restrict the manifest file to be named
    "AndroidManifest.xml".
  - Boolean flag values will now get normalized to 1 or 0 in
    canonicalize-flags output.
  - added experimental --use_new_category_enum to the help command to
    output options grouped by the new type of category.
  - Expose output jars and jdeps in java_common.provider, when
    available.
  - android_library targets are no longer allowed to use deps to
    export targets implicitly; please use android_library.exports
    instead.
  - New depset API
  - apple_binary and apple_static_library no longer support
    compilation attributes such as 'srcs'. If this breaks any
    existing targets, you may migrate all such attributes to a new
    objc_library target and depend on that objc_library target via
    the 'deps' attribute of apple_binary or apple_static_library.

## Release 0.6.1 (2017-10-05)

```
Baseline: 87cc92e5df35d02a7c9bc50b229c513563dc1689

Cherry picks:
   + a615d288b008c36c659fdc17965207bb62d95d8d:
     Rollback context.actions.args() functionality.
   + 7b091c1397a82258e26ab5336df6c8dae1d97384:
     Add a global failure when a test is interrupted/cancelled.
   + 95b0467e3eb42a8ce8d1179c0c7e1aab040e8120:
     Cleanups for Skylark tracebacks
   + cc9c2f07127a832a88f27f5d72e5508000b53429:
     Remove the status xml attribute from AntXmlResultWriter
   + 471c0e1678d0471961f1dc467666991e4cce3846:
     Release 0.6.0 (2017-09-28)
   + 8bdd409f4900d4574667fed83d86b494debef467:
     Only compute hostname once per server lifetime
   + 0bc9b3e14f305706d72180371f73a98d6bfcdf35:
     Fix bug in NetUtil caching.
```

Important changes:
 - Only compute hostname once per server lifetime

## Release 0.6.0 (2017-09-28)

```
Baseline: 87cc92e5df35d02a7c9bc50b229c513563dc1689

Cherry picks:
   + a615d288b008c36c659fdc17965207bb62d95d8d:
     Rollback context.actions.args() functionality.
   + 7b091c1397a82258e26ab5336df6c8dae1d97384:
     Add a global failure when a test is interrupted/cancelled.
   + 95b0467e3eb42a8ce8d1179c0c7e1aab040e8120:
     Cleanups for Skylark tracebacks
   + cc9c2f07127a832a88f27f5d72e5508000b53429:
     Remove the status xml attribute from AntXmlResultWriter
```

Incompatible changes:

  - Noop flag --deprecated_generate_xcode_project deleted.
  - Objects in Skylark are converted to strings in a more descriptive
    and less harmful way (they don't leak information that shouldn't
    be accessed by Skylark code, e.g. nondeterministic memory addresses
    of objects).
  - `set` is deprecated in BUILD and .bzl files, please use `depset`
    instead. Ordering names have also been changed, please use
    "default", "postorder", "preorder", and "topological" instead of
    "stable", "compile", "naive_link", and "link" correspondingly.
  - Integer overflow (on signed 32 bit numbers) in BUILD/bzl files is
    an error.
  - Keyword-only syntax in a function definition is now forbidden
      e.g. `def foo(a, *, b)` or `def foo(a, *b, c)`
  - --incompatible_comprehension_variables_do_not_leak defaults to
    "true."
      Iteration variable becomes inaccessible after a list/dict
    comprehension.

New features:

  - There is now a 'siblings' query function. See the query
    documentation for more details.
  - Added the print_action command, which outputs the
    actions needed to build a given target in the form of an
    ExtraActionSummary proto in text format.
  - android_binary now supports proguard_apply_dictionary to specify
    a custom dictionary to use for choosing names to obfuscate
    classes and members to.

Important changes:

  - 'strip' action is now configured via feature configuration
  - Flags from action_config get added first to the command line
    first,
    before the flags from features.
  - `bazel info output_path` no longer relies on the root directory
    filename being equal to the workspace name.
  - The `print` function now prints debug messages instead of
    warnings.
  - speedup of incremental dexing tools
  - --announce_rc now controls whether bazelrc startup options are
    printed to stderr.
  - Removing a few unused objc_provider keys.
  - Improved logging when workers have to be restarted due to its
    files having changed.
  - Top-level `if` statements are now forbidden.
  - Java protos are compiled to Java 7 bytecode.
  - All Android builds now use the desugar tool to support some Java
    8 features by default. To disable, use the
    --nodesugar_for_android flag.
  - Skylark-related options may now appear as "common" command
    options in the .bazelrc
  - Python is now required to build bazel.
  - When the lvalue of an augmented assignment is a list, we now
    throw an error
      before evaluating the code (e.g. `a, b += 2, 3`).
  - New --build_runfile_manifests flag controls production of
    runfiles manifests.
  - Enable debug info for Java builds
  - Allow java_lite_proto_library in the deps of android rules.
  - .so files in APKs will be memory-page aligned when
    android_binary.nocompress_extensions contains ".so" and
    --experimental_android_use_nocompress_extensions_on_apk is
    specified.
  - Skylark providers can specify allowed fields and their
    documentation.
  - Support ctx.actions.args() for more efficient Skylark command
    line construction.
  - The remote HTTP/1.1 caching client (--remote_rest_cache) now
    distinquishes between action cache and CAS. The request URL for
    the action cache is prefixed with 'ac' and the URL for the CAS
    is prefixed with 'cas'.
  - `JavaInfo` is a preferred alias to `java_common.provider`.
  - J2ObjC version updated to 2.0.3.
  - A new Java coverage implementation is available. Makes possible
    coverage for Skylark JVM rules.
  - Make proguard_apply_dictionary also apply to class and package
    obfuscation, not just class members.
  - When using the dictionary literal syntax, it is now an error to
    have duplicated keys (e.g.  {'ab': 3, 'ab': 5}).
  - android_binary.nocompress_extensions now applies to all files in
    the APK, not just resources and assets.
  - The apple_genrule rule that is distributed with Bazel has been
    deleted. Users who wish to use genrules with Xcode's
    DEVELOPER_DIR set should use the rules in
    https://github.com/bazelbuild/rules_apple instead.
  - The swift_library rule that is distributed with Bazel has been
    deleted. Users who wish to compile Swift should use the rules in
    https://github.com/bazelbuild/rules_apple instead.

## Release 0.5.4 (2017-08-25)

```
Baseline: 6563b2d42d29196432d5fcafa0144b8371fbb028

Cherry picks:
   + d4fa181f8607c35230b7efa1ce94188b51508962:
     Use getExecPathString when getting bash_main_file
   + 837e1b3d4859140d29aaa6bbab8fbb008e6d701e:
     Windows, sh_bin. launcher: export runfiles envvars
   + fe9ba893c0ebec19228086356af5fa8d81f2809b:
     grpc: Consolidate gRPC code from BES and Remote Execution. Fixes
     #3460, #3486
   + e8d4366cd374fba92f1425de0d475411c8defda4:
     Automated rollback of commit
     496d3ded0bce12b7371a93e1183ba30e6aa88032.
   + 242a43449dd44a22857f6ce95f7cc6a7e134d298:
     bes,remote: update default auth scope.
   + 793b409eeae2b42be7fed58251afa87b5733ca4d:
     Windows, sh_bin. launcher: fix manifest path
   + 7e4fbbe4ab3915a57b2187408c3909e5cd6c6013:
     Add --windows_exe_launcher option
   + 91fb38e92ace6cf14ce5da6527d71320b4e3f3d2:
     remote_worker: Serialize fork() calls. Fixes #3356
   + b79a9fcd40f448d3aebb2b93a2ebe80d09b38408:
     Quote python_path and launcher in
     python_stub_template_windows.txt
   + 4a2e17f85fc8450aa084b201c5f24b30010c5987:
     Add build_windows_jni.sh back
   + ce61d638197251f71ed90db74843b55d9c2e9ae5:
     don't use methods and classes removed in upstream dx RELNOTES:
     update dexing tools to Android SDK 26.0.1
   + 5393a4996d701fa192964a35cbb75e558a0599c0:
     Make Bazel enforce requirement on build-tools 26.0.1 or later.
   + 5fac03570f80856c063c6019f5beb3bdc1672dee:
     Fix --verbose_failures w/ sandboxing to print the full command
     line
   + f7bd1acf1f96bb7e3e19edb9483d9e07eb5af070:
     Only patch in C++ compile features when they are not already
     defined in crosstool
   + d7f5c120417bc2d2344dfb285322355f225d9153:
     Bump python-gflags to 3.1.0, take two
   + 3cb136d5451e9d8af58f9a99990cad0592df101a:
     Add python to bazel's dockerfiles
```

New features:

  - Do not disable fully dynamic linking with ThinLTO when invoked
    via LIPO options.

Important changes:

  - Ignore --glibc in the Android transition.
  - Remove --experimental_android_use_singlejar_for_multidex.
  - nocopts now also filter copts
  - The Build Event Service (BES) client now properly supports
    Google Applicaton Default Credentials.
  - update dexing tools to Android SDK 26.0.1
  - Bazel Android support now requires build-tools 26.0.1 or later.
  - Fix a bug in the remote_worker that would at times make it crash on Linux. See #3356
  - The java_proto_library rule now supports generated sources. See #2265

## Release 0.5.3 (2017-07-27)

```
Baseline: 88518522a18df5788736be6151fc67992efe2aad

Cherry picks:
   + 820a46af10808396873c36d0f331e533118cf0c6:
     Automated rollback of commit
     6d6e87297fe8818e4c374fdfabfbcf538bca898a.
   + ccfb2df69ecf4746f5a15e1295af995c3a45aa94:
     Allow py_binary to be the executable of a Skylark action or any
     SpawnAction on Windows.
   + 06534911696838e720c8681f6f568c69d28da65e:
     Fix string representation for the Root class
   + cd159bcee72a7f377621b45409807231a636f9e2:
     sandbox: Allow UNIX sockets on macOS even when block-network is
     used.
   + ad73cba3caa2e08ad61ea9ca63f9111cde1f48d1:
     Fix python_stub_template.txt to be compatible with Python 2.4.
   + 9a63aff8bb771af8917903fbbc9df3b708e2c0ed:
     Create Windows ZIP release artifact using Bazel
   + 5e576637b5705aff0a7bf56b5077463dffcd712f:
     Automated rollback of commit
     820a46af10808396873c36d0f331e533118cf0c6.
   + b6e29ca217b02c3ba499b85479a3830f59c9b9b6:
     Use the correct function to generate the release notes
   + 0f3481ba6364f24ef76b839bdde06ae7883c9bd9:
     Include <cinttypes> instead of <stdint.h>
```

Incompatible changes:

  - The --output=location flag to 'bazel query' cannot be used with
    query expressions that involve the 'buildfiles' or 'loadfiles'
    operators. This also applies to 'genquery' rules.
  - Operators for equality, comparison, 'in' and 'not in' are no
    longer associative, e.g.  x < y < z  is now a syntax error.
    Before, it was parsed as:  (x < y) < z.
  - In strings, octal sequences greater than \377 are now forbidden
    (e.g. "\\600"). Previously, Blaze had the same behavior as Python 2,
    where "\\450" == "\050".
  - Using tabulation for identation is now fobidden in .bzl files
  - `load` is now a language keyword, it cannot be used as an
    identifier
  - lvalues must have define at least one variable (i.e. we forbid
    `[] = f()`).
  - Fixed a bug whereby multiple load() statements could appear on
    the same line
  - -extra_checks:off is no longer supported; use
    -XepDisableAllChecks instead
  - java_common.java_toolchain_attr is removed. Depend on the
    java_toolchain_alias() rule to accomplish the same thing.
  - cc_common.cc_toolchain_attr and java_common.java_runtime_attr are
    not supported anymore and were replaced with the
    cc_toolchain_alias() and java_runtime_alias() rules.

New features:

  - Zipped LLVM profiles are now supported.
  - LIPO maps to ThinLTO for LLVM builds.
  - Change to handle LLVM FDO zipped profile contents correctly.

Important changes:

  - Windows: bazel clean --expunge works
  - First argument of 'load' should be a label. Path syntax is
    deprecated (label should start with '//' or ':').
  - Octal prefix '0' is deprecated in favor of '0o' (use 0o777
    instead of 0777).
  - The extension_safe attribute of apple_binary no longer validates
    transitive dependencies are compiled against extension_safe APIs.
  - Parentheses around the tuple are now mandatory in [a for b in c
    if 1, 2]
  - Adjust the thresholds for --test_verbose_timeout_warnings so that
    it can recommending timeout increases and won't recommend
    timeouts that are too close to the actual timeout.
  - Iterating on a `depset` object is deprecated. If you need an
    iterable, call the `.to_list()` method first.
  - Bazel now uses tools from action_configs in Crosstool by default
    (as oposed to using top level tools).
  - Incremental dexing errors on combination of --multidex=off and
    either --main-dex-list or --minimal-main-dex.
  - When using the dictionary literal syntax, it is now an error to
    have duplicated keys (e.g.  {'ab': 3, 'ab': 5}).
  - New property on android_sdk: aapt2
      Choose the version of aapt on android_binary
  - Add idl_preprocessed attribute to android_library, so that
    preprocessed aidl files can be passed to android_library for
    compiling
  - Bazel's remote_worker backend for remote execution supports
    sandboxing on Linux now. Check
    https://github.com/bazelbuild/bazel/blob/master/src/tools/remote_w
    orker/README.md for details.
  - Allows flags that expand to take values.
  - Make querying attributes formed by selector lists of list types
    more efficient by no longer listing every possible combination of
    attribute value but by more compactly storing the possible values
    of the list.
  - Writing build events to a file is no longer experimental
  - set --rewrite_calls_to_long_compare to false by default.
  - ObjC and C++ coverage feature is unified under name 'coverage'
  - Enable --incremental_dexing for Android builds by default. Note
    that some dexopts are incompatible with incremental dexing,
    including --force-jumbo.
  - Evaluation will soon use checked arithmetics and throw an error
    instead of overflow/underflow.
  - Implicit iteration in the CROSSTOOL has been removed, use
    explicit 'iterate_over' message.
  - Add option for Android specific grte_top
  - Crosstool patches are only applied if the toolchain doesn't define
    'no_legacy_features' feature.
  - 'platform_type' is now a mandatory attribute on apple_binary and
    apple_static_library rules.
    If this change breaks your build, feel free to add platform_type
    = 'ios' to any apple_binary and apple_static_library
    targets in your project, as this was the previous default
    behavior.
  - Remove apple_watch2_extension build rule. Users should be using
    the skylark watchos_application and watchos_extension rules.
    https://github.com/bazelbuild/rules_apple has details.
  - Check stderr to detect if connected to a terminal.  Deprecate
    --isatty.
  - Commands that shut down the server (like "shutdown") now ensure
    that the server process has terminated before the client process
    terminates.
  - Remove apple_watch1_extension and apple_watch_extension_binary
    rules. Users should be using the skylark watchos_application and
    watchos_extension rules.
    https://github.com/bazelbuild/rules_apple has details.
  - Windows: Wrapper-less CROSSTOOL becomes default now.
    set USE_MSVC_WRAPPER=1 if you still want to use wrapper script.

## Release 0.5.2 (2017-06-27)

```
Baseline: e78ad83ded6e9c6d639793827e27b6570e6e9f65

Cherry picks:
   + 68028317c1d3d831a24f90e2b25d1410ce045c54:
     experimental UI: move stopUpdateThread() out of synchronized,
     again
   + 019935dfbb61e61d08d1351b0365fb4e2d0df305:
     Fix bug in URI computation in RemoteModule
   + e9424cf9b9d72b98594966d5ac0f15bb018ec639:
     Automated rollback of commit
     7dec00574aa91327693f6ba7e90bff5bc834253e.
   + 9eea05d068a06ab642dd9d86d46ee5fa2e36b02e:
     Switching to Watcher API instead of wait_for_completion, in
     preparation for deprecating the wait_for_completion field.
   + 89659810e3048782dfb5e308e39aa8a0727e464e:
     Set correct execroot for info
   + 716b527266f47f59a2b7fb2e5fc52cb45e1691b1:
     Only create a single per-build instance of the remote cache /
     executor
   + 1d82d199f82409f217a42bcefebb96f723f91caa:
     protobuf: Update protobuf jars to be binary compatible with Java
     6. Fixes #3198
   + 524b90d9e5acc4fa568f215c9415eaa902e979f8:
     Change CAS URI to use the "bytestream" scheme instead of being
     scheme-less
   + 4929ad79865f8c13ef3b33c827040f4a037e4afe:
     Automated g4 rollback of commit
     923d7df521f67d031b288180560848bd35e20976.
   + 68b9a7e2dc17e32b194238d287e79bee1ba035b9:
     Automated g4 rollback of commit
     da56606563ee9df438db93392f681bf2abb4ac97.
   + 2ba693ffbe824136a0ca5f47d34710612f6302c3:
     Automated rollback of commit
     ce7c4deda60a307bba5f0c9421738e2a375cf44e.
```

Incompatible changes:

  - Blaze no longer generates xcode projects. Use tulsi.bazel.build
    instead.

Important changes:

  - Keyword-only syntax in a function definition is deprecated
      (e.g. `def foo(a, *, b)` or `def foo(a, *b, c)`) and will be
    removed in the future.
  - Attempting to build an Android target without setting up
    android_sdk_repository will now produce a helpful error message.
  - Adds a sha256 attribute to git_repository and new_git_repository.
    This can only be used if the remote is a public GitHub
    repository. It forces
    Bazel to download the repository as a tarball, which will often
    be faster and
    more robust than cloning it.
  - Sandboxing is now enabled by default on FreeBSD (via
    processwrapper-sandbox).
  - android_test may use manifest placeholders with 'manifest_merger
    = "android"'.
  - load() statements should be called at the top of .bzl files,
    before any
      other statement. This convention will be enforced in the future.
  - Effectively remove sysroot from CppConfiguration and allow it to
    use select statements.
  - proto_library.strict_proto_deps no longer exists.
  - Flag --explicit_jre_deps is now a noop.
  - The 'legacy' Android manifest merger is deprecated. Please
    upgrade to the 'android' manifest merger, which is the same
    merger used by Gradle.
    https://developer.android.com/studio/build/manifest-merge.html
  - Using $(CC_FLAGS) in a GenRule adds a dependency to the c++
    toolchain
  - add one-version enforcement to android_local_test
  - Skylark support (apple_common.dotted_version(string)) for
    building DottedVersion objects to interface with native apple
    rules
  - CC_FLAGS can be defined using 'cc-flags-make-variable' action_config in
    CROSSTOOL
  - ios_framework native rule has been removed. This rule had been
    essentially broken for several months now; users should be using
    the skylark ios framework rule.
    https://github.com/bazelbuild/rules_apple has details.
  - Clean command no longer uses boolean values for --async,
    --expunge, and --expunge_async options.
  - Partially fixes external J2ObjC support.
  - '--aspects' can occur more than once on the command line.
  - --no_ prefix no longer recognized.
  - Use action_config in crosstool for static library archiving,
    remove ar_flag.
  - Added a new flag --sandbox_writable_path, which asks the sandbox
    to
    make an existing directory writable when running actions.
  - bazel test now also computes a default instrumentation filter if
    --collect_code_coverage is enabled
  - n/na
  - In .bzl files, top-level `if` statements are deprecated and will
    be forbidden
      in the future. Move them in a function body instead (or use a
    conditional
      expression instead: `x if condition else y`).
  - ios_device and ios_test are deprecated. Please use the new testing
    rules in https://github.com/bazelbuild/rules_apple instead.
  - bazel query --output package now displays packages from external
    repository with the format "@reponame//package". Packages in the
    main repository continue to have the format "package".
  - ctx.expand_make_variables is deprecated.
  - Bazel posts links to the CAS to the BEP if remote caching /
    execution is enabled
  - `bazel info execution_root` returns the corrrect directory name
    for the execution root.

## Release 0.5.1 (2017-06-06)

```
Baseline: f3ae88ee043846e7acdffd645137075a4e72c573

Cherry picks:
   + c58ba098526b748f9c73e6229cafd74748205aa1:
     Release to GCS: put the final release in its own directory
   + 0acead4ea3631240659836ce6ecd6d7f67fd352b:
     Update protobuf to latest master at a64497c and apply
     @laszlocsomor's latest changes from
     https://github.com/google/protobuf/pull/2969 on top of it.
   + d0242ce4a87929f2528f4602d0fb09d1ccfcea94:
     Make symlinks consistent
   + d953ca8b87a46decbce385cebb446ae0dd390881:
     Clean VanillaJavaBuilder output directories
   + 755669fb5de1f4e762f27c19776cac9f410fcb94:
     Pass all the environment variable to Bazel during bootstrapping
   + 6f041661ca159903691fcb443d86dc7b6454253d:
     Do not mark the JDK7 installer -without-jdk-installer
   + 720561113bfa702acfc2ca24ce3cc3fd7ee9c115:
     Fix #2958: Installer should not overwrite bazelrc
   + 511c35b46cead500d4e76706e0a709e50995ceba:
     Bootstrap: move the fail function to the top
   + 8470be1122825aae8ad0903dd1e1e2a90cce47d2:
     Clean up javac and Error Prone targets
   + 4a404de2c6c38735167e17ab41be45ef6fc4713a:
     Update javac version to 9-dev-r4023-2
   + 36ce4b433e19498a78c34540d5a166d4e0006b22:
     Update javac version to 9-dev-r4023-2
   + 38949b8526bdb3e6db22f3846aac87162c28c33f:
     Migrate off versioned javac and Error Prone targets
   + 1a57d298f8aa6ea8136d93223902104f2479cd2a:
     Re-enabling passing -sourcepath via javacopts.
   + eb565f408e03125e92d42b00756e519795be6593:
     Make make sure that msys build actually builds msys version
   + 39f328cf392056618d1a3ead4835a138b189a06d:
     Fix typo. Also do not override host_cpu for msvc.
   + 624802893f4fe72118f00a78452605d41a2e1c6f:
     Select correct JDK for windows_msys
   + c4f271d1a68366b6fa5ff38ea7d951b6a22af044:
     Automated g4 rollback of commit
     3e5edafa2a04a71cd3596e929e83222da725f3f9.
   + 926180997a0f296a5a009326aead887279ce0a90:
     Remove process-tools.cc which I forgot to delete during the last
     rollback.
   + baca6e4cb023649920871b74810927d304729e59:
     Fix #2982: Bazel installer should not check for installed JDK if
     using a bundled JDK.
   + 866ecc8c3d5e0b899e3f0c9c6b2265f16daae842:
     Disable msys path conversion on Windows.
   + cc21998c299b4d1f97df37b961552ff8168da17f:
     Rollforward #2 of: Basic open-source crosstool to support
     targeting apple platform types.
   + 0f0ccc4fc8229c1860a9c9b58089d6cfb2ee971f:
     Escape % in strings that will appear in Crosstool
   + 3b08f774e7938928e3a240a47a0a7554cdc8d50b:
     Adding feature for linking C Run-Time library on Windows
   + 3566474202d1978acfdcb7e5ff73ee03ea6f3df9:
     Do not use sed -E in bootstrap/compile.sh
   + c3cf7d917afd02d71de3800cd46ad8d14f1ddf55:
     Reverts non-xcode-available darwin crosstool generation.
```

Important changes:

  - Fixes regression in 0.5.0 requiring Xcode to build C++ on OSX.

## Release 0.5.0 (2017-05-26)

```
Baseline: f3ae88ee043846e7acdffd645137075a4e72c573

Cherry picks:
   + c58ba098526b748f9c73e6229cafd74748205aa1:
     Release to GCS: put the final release in its own directory
   + 0acead4ea3631240659836ce6ecd6d7f67fd352b:
     Update protobuf to latest master at a64497c and apply
     @laszlocsomor's latest changes from
     https://github.com/google/protobuf/pull/2969 on top of it.
   + d0242ce4a87929f2528f4602d0fb09d1ccfcea94:
     Make symlinks consistent
   + d953ca8b87a46decbce385cebb446ae0dd390881:
     Clean VanillaJavaBuilder output directories
   + 755669fb5de1f4e762f27c19776cac9f410fcb94:
     Pass all the environment variable to Bazel during bootstrapping
   + 6f041661ca159903691fcb443d86dc7b6454253d:
     Do not mark the JDK7 installer -without-jdk-installer
   + 720561113bfa702acfc2ca24ce3cc3fd7ee9c115:
     Fix #2958: Installer should not overwrite bazelrc
   + 511c35b46cead500d4e76706e0a709e50995ceba:
     Bootstrap: move the fail function to the top
   + 8470be1122825aae8ad0903dd1e1e2a90cce47d2:
     Clean up javac and Error Prone targets
   + 4a404de2c6c38735167e17ab41be45ef6fc4713a:
     Update javac version to 9-dev-r4023-2
   + 36ce4b433e19498a78c34540d5a166d4e0006b22:
     Update javac version to 9-dev-r4023-2
   + 38949b8526bdb3e6db22f3846aac87162c28c33f:
     Migrate off versioned javac and Error Prone targets
   + 1a57d298f8aa6ea8136d93223902104f2479cd2a:
     Re-enabling passing -sourcepath via javacopts.
   + eb565f408e03125e92d42b00756e519795be6593:
     Make make sure that msys build actually builds msys version
   + 39f328cf392056618d1a3ead4835a138b189a06d:
     Fix typo. Also do not override host_cpu for msvc.
   + 624802893f4fe72118f00a78452605d41a2e1c6f:
     Select correct JDK for windows_msys
   + c4f271d1a68366b6fa5ff38ea7d951b6a22af044:
     Automated g4 rollback of commit
     3e5edafa2a04a71cd3596e929e83222da725f3f9.
   + 926180997a0f296a5a009326aead887279ce0a90:
     Remove process-tools.cc which I forgot to delete during the last
     rollback.
   + baca6e4cb023649920871b74810927d304729e59:
     Fix #2982: Bazel installer should not check for installed JDK if
     using a bundled JDK.
   + 866ecc8c3d5e0b899e3f0c9c6b2265f16daae842:
     Disable msys path conversion on Windows.
   + cc21998c299b4d1f97df37b961552ff8168da17f:
     Rollforward #2 of: Basic open-source crosstool to support
     targeting apple platform types.
   + 0f0ccc4fc8229c1860a9c9b58089d6cfb2ee971f:
     Escape % in strings that will appear in Crosstool
   + 3b08f774e7938928e3a240a47a0a7554cdc8d50b:
     Adding feature for linking C Run-Time library on Windows
```

Incompatible changes:

  - Bazel's Linux sandbox no longer mounts an empty tmpfs on /tmp,
    instead the existing /tmp is mounted read-write. If you prefer
    to have a tmpfs on /tmp for sandboxed actions for increased
    hermeticity, please use the flag --sandbox_tmpfs_path=/tmp.
  - Converting artifacts to strings and printing them now return
    "File" instead of "Artifact" to be consistent with the type name.
  - The return type of depset.to_list() is now a list rather than a
    frozen list. (Modifying the list has no effect on the depset.)
  - Bazel now prints logs in single lines to java.log
  - --use_dash, --dash_url and --dash_secret are removed.
  - Remote repositories must define any remote repositories they
    themselves use (e.g., if @x//:foo depends on @y//:bar, @y must be
    defined
    in @x's WORKSPACE file).
  - Remote repositories must define any remote repositories they
    themselves use (e.g., if @x//:foo depends on @y//:bar, @y must be
    defined
    in @x's WORKSPACE file).
  - objc_xcodeproj has been removed, use tulsi.bazel.build instead.

New features:

  - If grte_top is a label, it can now follow non-configurable
    redirects.
  - Optional coverage_files attribute to cc_toolchain
  - "query --output=build" now includes select()s
  - Raw LLVM profiles are now supported.

Important changes:

  - Automatically generate Proguard mapping when resource shrinking
    and Proguard are enabled.
  - New rules in Bazel: proto_library, java_lite_proto_library,
    java_proto_library and cc_proto_library
  - Activate the "dead_strip" feature if objc binary stripping is
    enabled.
  - More stable naming scheme for lambda classes in desugared android
    code
  - Convert --use_action_cache to a regular option
  - Per-architecture dSYM binaries are now propagated by
    apple_binary's AppleDebugOutputsProvider.
  - Avoid factory methods when desugaring stateless lambdas for
    Android
  - desugar calls to Objects.requireNonNull(Object o) with
    o.getClass() for android
  - Add an --copy_bridges_from_classpath argument to android
    desugaring tool
  - Change how desugar finds desugared classes to have it working on
    Windows
  - Evaluation of commands on TargetsBelowDirectory patterns
    (e.g. //foo/...) matching packages that fail to load now report
    more
    detailed error messages in keep_going mode.
  - Allow to have several inputs and outputs
  - Repository context's execute() function can print stdout/stderr
    while running. To enable, pass quiet=False.
  - Bazel can now be built with a bundled version of the OpenJDK.
    This makes it possible to use Bazel on systems without a JDK, or
    where
    the installed JDK is too old.
  - The --jobs flag now defaults to "auto", which causes Bazel to
    use a reasonable degree of parallelism based on the local
    machine's
    capacity.
  - Bazel benchmark (perf.bazel.build) supports Java and Cpp targets.
  - no factory methods generated for lambda expressions on android
  - The Linux sandbox no longer changes the user to 'nobody' by
    default, instead the current user is used as is. The old behavior
    can be
    restored via the --sandbox_fake_username flag.
  - /tmp and /dev/shm are now writable by default inside the
    Linux sandbox.
  - Bazel can now use the process-wrapper + symlink tree based
    sandbox implementation in FreeBSD.
  - turn on --experimental_incremental_dexing_error_on_missed_jars by
    default.
  - All android_binarys are now signed with both Apk Signature V1 and
    V2. See https://source.android.com/security/apksigning/v2.html
    for more details.
  - Windows MSVC wrappers: Not filtering warning messages anymore,
    use --copt=-w and --host_copt=-w to suppress them.
  - A downloader bug was fixed that prevented RFC 7233 Range
    connection resumes from working with certain HTTP servers
  - Introduces experimental android_device rule for configuring and
    launching Android emulators.
  - For boolean flags, setting them to false using --no_<flag_name>
    is deprecated. Use --no<flag_name> without the underscore, or
    --<flag_name>=false instead.
  - Add --experimental_android_compress_java_resources flag to store
    java
    resources as compressed inside the APK.
  - Removed --experimental_use_jack_for_dexing and libname.jack
    output of
    android_library.
  - blaze canonicalize-flags now takes a --show_warnings flag
  - Changing --invocation_policy will no longer force a server
    restart.
  - Bazel now supports Android NDK14.
  - android_binary multidex should now work without additional flags.
  - Use action_config in crosstool for static library archiving,
    remove ar_flag.
  - new option for bazel canonicalize-flags, --canonicalize_policy
  - Use action_config in crosstool for static library archiving,
    remove ar_flag.
  - android_library exports_manifest now defaults to True.
  - Fix select condition intersections.
  - Adds a --override_repository option that takes a repository
    name and path. This forces Bazel to use the directory at that path
    for the repository. Example usage:
    `--override_repository=foo=/home/user/gitroot/foo`.
  - fix idempotency issue with desugaring lambdas in interface
    initializers for android
  - --experimental_android_use_singlejar_for_multidex is now a no-op
    and will eventually be removed.
  - Every local_repository now requires a WORKSPACE file.
  - Remove jack and jill attributes of the android_sdk rule.
  - Add Skylark stubs needed to remove sysroot from CppConfiguration.
  - Desugar try-with-resources so that this language feature is
    available
    to deveces with API level under 19.
  - The flag --worker_max_retries was removed. The
    WorkerSpawnStrategy no longer retries execution of failed Spawns,
    the reason being that this just masks compiler bugs and isn't
    done for any other execution strategy either.
  - Bazel will no longer gracefully restart workers that crashed /
    quit, instead this triggers a build failure.
  - All java resources are now compressed in android_binary APKs by
    default.
  - All java resources are now compressed in android_binary APKs by
    default.
  - android_ndk_repository now creates a cc_library
    (@androidndk//:cpufeatures) for the cpufeatures library that is
    bundled in the Android NDK. See
    https://developer.android.com/ndk/guides/cpu-features.html for
    more details.
  - 'output_groups' and 'instrumented_files' cannot be specified in
    DefaultInfo.
  - You can increase the CPU reservation for tests by adding a
    "cpu:<n>" (e.g. "cpu:4" for four cores) tag to their rule in a
    BUILD file. This can be used if tests would otherwise overwhelm
    your system if there's too much parallelism.
  - Deprecate use_singlejar_for_proguard_libraryjars and force
    behavior to always on.

## Release 0.4.5 (2017-03-16)

```
Baseline: 2e689c29d5fc8a747216563235e905b1b62d63b0

Cherry picks:
   + a28b54033227d930672ec7f2714de52e5e0a67eb:
     Fix Cpp action caching
   + 6d1d424b4c0da724e20e14235de8012f05c470f8:
     Fix paths of binaries in .deb packages.
   + 0785cbb672357d950e0c045770c4567df9fbdc43:
     Update to guava 21.0 and Error Prone version 2.0.18-20160224
   + 30490512eb0e48a3774cc4e4ef78680e77dd4e47:
     Update to latest javac and Error Prone
   + 867d16eab3bfabae070567ecd878c291978ff338:
     Allow ' ', '(', ')' and '$' in labels
   + 7b295d34f3a4f42c13aafc1cc8afba3cb4aa2985:
     Pass through -sourcepath to the JavaBuilder
   + 14e4755ce554cdfc685fc9cc2bfb5b699a3b48f4:
     PathFragment comparisons are now platform-aware
   + ed7795234ca7ccd2567007f2c502f853cd947e50:
     Flag to import external repositories in python import path
   + 81ae08bbc13f5f4a04f18caae339ca77ae2699c1:
     Suppress error for non-exhaustive switches
   + e8d1177eef9a9798d2b971630b8cea59471eec33:
     Correctly returns null if an environment variables is missing
   + 869d52f145c077e3499b88df752cebc60af51d66:
     Fix NPE in Android{S,N}dkRepositoryFunction.
   + d72bc57b60b26245e64f5ccafe023a5ede81cc7f:
     Select the good guava jars for JDK7 build
   + 92ecbaeaf6fa11dff161254df38d743d48be8c61:
     Windows: Assist JNI builds with a target for jni_md.h.
   + 36958806f2cd38dc51e64cd7bcc557bd143bbdb6:
     Add java_common.create_provider to allow creating a
     java_common.provider
   + 8c00f398d7be863c4f502bde3f5d282b1e18f504:
     Improve handling of unknown NDK revisions in
     android_ndk_repository.
   + b6ea0d33d3ab72922c8fb3ec1ff0e437af09584d:
     Add the appropriate cxx_builtin_include_directory entries for
     clang to the Android NDK crosstool created by
     android_ndk_repository.
```

Incompatible changes:

  - Depsets (former sets) are converted to strings as "depset(...)"
    instead of
    "set(...)".
  - Using --symlink_prefix is now applied to the output
    symlink (e.g. bazel-out) and the exec root symlink (e.g.
    bazel-workspace).
  - Bazel now uses the test's PATH for commands specified as
        --run_under; this can affect users who explicitly set PATH to
    a more
        restrictive value than the default, which is to forward the
    local PATH
  - It's not allowed anymore to compare objects of different types
    (i.e. a string to an integer) and objects for which comparison
    rules are not
    defined (i.e. a dict to another dict) using order operators.

New features:

  - environ parameter to the repository_rule function let
    defines a list of environment variables for which a change of
    value
    will trigger a repository refetching.

Important changes:

  - android_ndk_repository now supports Android NDK R13.
  - Android resource shrinking is now available for android_binary
    rules. To enable, set the attribute 'shrink_resources = 1'. See
    https://bazel.build/versions/master/docs/be/android.html#android_b
    inary.shrink_resources.
  - resolve_command/action's input_manifest return/parameter is now
    list
  - For increased compatibility with environments where UTS
    namespaces are not available, the Linux sandbox no longer hides
    the hostname of the local machine by default. Use
    --sandbox_fake_hostname to re-enable this feature.
  - proto_library: alias libraries produce empty files for descriptor
    sets.
  - Adds pkg_rpm rule for generating RPM packages.
  - Allow CROSSTOOL files to have linker flags specific to static
    shared libraries.
  - Make it mandatory for Java test suites in bazel codebase, to
    contain at least one test.
  - Support for Java 8 lambdas, method references, type annotations
    and repeated annotations in Android builds with
    --experimental_desugar_for_android.
  - Removed .xcodeproj automatic output from objc rules. It can still
    be generated by requesting it explicitly on the command line.
  - Flips --explicit_jre_deps flag on by default.
  - Activate the "dbg", "fastbuild", and "opt" features in the objc
    CROSSTOOL.
  - Remove support for configuring JDKs with filegroups; use
    java_runtime and java_runtime_suite instead
  - android_ndk_repository api_level attribute is now optional. If not
    specified, the highest api level in the ndk/platforms directory
    is used.

## Release 0.4.4 (2017-02-01)

```
Baseline: 4bf8cc30a

Cherry picks:
   + ef1c6fd33: msvc_tools.py.tpl: Change default runtime library to
              static
```

Incompatible changes:

  - Only targets with public visibility can be bound to something in
    //external: .
  - The deprecated -x startup option has been removed.
  - docker_build: change the repository names embedded by
    docker_build. You can revert to the old behavior by setting
    legacy_repository_naming=True.
  - The string methods strip(), lstrip(), and rstrip() now
    by default remove the same whitespace characters as Python 3
    does, and accept
    None as an argument.
  - Deprecated globals HOST_CFG and DATA_CFG are removed. Use strings
    "host" and "data" instead.
  - repository_ctx environment is now affected by --action_env flag
    (value from the
    client environment will be replaced by value given on the command
    line through --action_env).
  - All executable labels must also have a cfg parameter specified.
  - Removed the cmd_helper.template function.
      The function was equivalent to:
        def template(items, template):
          return [template.format(path = i.path, short_path =
    i.short_path)
                    for i in items]
  - Tuples that end with a trailing comma must now be inside parens,
      e.g. (1,) instead of 1,
  - The traversal orders for depsets have been renamed. The old names
    are deprecated and will be removed in the future. New names:
    "stable" -> "default", "compile" -> "postorder", "link" ->
    "topological", "naive_link" -> "preorder".

New features:

  - Skylark: you can now multiply a list by an integer to get the
    concatenation of N copies of this list, e.g. [a,b] * 3 =
    [a,b,a,b,a,b]
  - Allow Android aidl tool to add a jar to the program's classpath,
    such as if needed to support generated sources.
  - Add transitive proguard_specs when android_sdk.aidl_lib is
    specified
  - Windows: "/dev/null" is now a supported path, e.g.
    --bazelrc=/dev/null now works

Important changes:

  - Bazel Android builds use the apksigner tool from the Android SDK
    build-tools. Bazel Android builds now require build-tools version
    24.0.3 or
    later.
  - Android SDK external bindings for support libraries, e.g.
    //external:android/appcompat_v4, are removed because the support
    library JARs that they referenced no longer ship with the Android
    SDK.
  - aar_import rule is now documented.
  - An IE bug was fixed in repository_ctx.download_and_extract
  - Update "-I" to "-isystem" in documentation to reflect current
    behavior.
  - android_sdk_repository build_tools_version is now optional. The
    highest installed build-tools will be used if none is specified.
  - New flag --sandbox_add_mount_pair to specify customized
    source:target path pairs to bind mount inside the sandbox.
  - expose proto_library descriptor set to skylark via
    <dep>.proto.descriptor_set
  - The `set` constructor is deprecated in favor of `depset`
  - Autodetect gold linker in cc_configure.bzl
  - Remove build flag --experimental_j2objc_annotation_processing. It
    is on by default now.
  - Set clang's -mwatchos-version-min correctly using the value of
    --watchos_minimum_os, not --watchos_sdk_version.
  - singlejar can now create jar files larger than 4GB.
  - android_sdk_repository and android_ndk_repository now read
    $ANDROID_HOME and $ANDROID_NDK_HOME if the path attribute is not
    set.
  - Removed broken api levels 3, 4 and 5 from Android NDK 12.
  - Default --android_dynamic_mode to off.
  - android_sdk_repository no longer requires api_level. If one is
    not specified, the highest android platform installed will be
    used. Furthermore, android_sdk's are created for all android
    platforms installed and can be specified with the --android_sdk
    flag.
  - To iterate over or test for membership in a set, prefer using the
    new to_list() method. E.g., "for x in myset.to_list():", or
    "print(x in myset.to_list())". Iteration/membership-test on the
    raw set itself is deprecated.
  - Remove support for --javawarn; use e.g. --javacopt=-Xlint:all
    instead

## Release 0.4.3 (2016-12-22)

```
Baseline: c645a45

Cherry picks:
   + af878d0: Add coverage support for java test. (series 4/4 of
              open-sourcing coverage command for java test)
   + 09b92a8: Rollback of commit
              67b4d5250edcefa7220e928e529b1f385e2dc464.
   + b11dd48: Fix bad bug with the parallel implementation of
              BinaryOperatorExpression. Turns out that
              ForkJoinTask#adapt(Callable) returns a ForkJoinTask
              whose Future#get on error throws a ExecutionException
              wrapping a RuntimeException wrapping the thrown checked
              exception from the callable. This is documented
              behavior [1] that I incorrectly didn't know about.
   + 9012bf1: Fix scripts/packages/convert_changelog to read the
              changelog correctly
   + 55c97bc: Release script: if master branch does not exist, fall
              back on origin/master
   + 4fb378c: Debian repository: override section and priority fields
   + acbcbc2: Fix release notes in emails
   + 4975760: Fix PathFragment to not use Java8-only static hashCode
              methods.
   + 05fd076: Disable sandboxing for XibCompile actions.
```

Incompatible changes:

  - Skylark maven_jar and maven_aar settings attribute is now a label
    so it can be checked into your workspace.
  - --{no}experimental_use_rclass_generator is now a nop.

New features:

  - Coverage support (*experimental*) for pure Java target.
    Use `bazel coverage //my:target` to generate coverage information
    from a `java_test`.

Important changes:

  - Enable fallback URLs in Skylark http rules.
  - cc_proto_library generates C++ code from proto_library rules.
  - cc_library now supports the strip_prefix and strip_include_prefix
    attributes for control of include paths.
  - Skylark dicts internally don't rely on keys order anymore and
    accept any hashable values (i.e. structs with immutable values)
    as keys. Iteration order of dictionaries is no longer specified.

## Release 0.4.2 (2016-12-02)

```
Baseline: 6331a94

Cherry picks:
   + 7b835d9: Do not patch WORKSPACE in the release process
```

Incompatible changes:

  - Callback functions in Skylark no longer support the cfg
    parameter. This is a cleanup and only affects the signatures of
    callbacks, since the parameter hasn't been set since September
    2016.

Important changes:

  - Alias proto_library's produce a descriptor set that contains all
    srcs of its dependencies.
  - proto_library supports strict proto deps.
  - Top level @androidsdk support library targets have been replaced
    by @androidsdk//<group id>:<artifact id>-<version> for Android
    SDK Support and Google Play Services libraries.

## Release 0.4.1 (2016-11-21)

```
Baseline: 9a796de

Cherry picks:
   + 88bfe85: Description redacted. -- MOS_MIGRATED_REVID=139219934
   + b09ea94: Rollback of commit
              a3f5f576cd35798140ba3e81d03d919dd4ecb847.
```

New features:

  - android_library now has a "exported_plugins" attribute just like
    java_library
  - Use --strict_system_includes to apply hdrs_check=strict also to
        cc_library.includes, even if sandboxing is disabled.
  - Bazel on Windows: java_binary can now be the executable of
    Skylark rule actions (ctx.action's executable argument)
  - Packages are defined in BUILD.bazel as well as BUILD files.

Important changes:

  - getattr()'s 3-arg form no longer raises an error when the
    retrieved field is a built-in method.
  - --apk_signing_method default changed to v1. Android APKs are now
    signed with the new ApkSignerTool by default.
  - New rule: proto_lang_toolchain(), to support LANG_proto_library
    rules on multiple platforms.
  - Fix for Android clang++ std::stack segfault on 32bit x86. See
    https://code.google.com/p/android/issues/detail?id=220159
  - Default android_manifest_merger is now "android" which uses the
    official Android manifest merger.
    http://tools.android.com/tech-docs/new-build-system/user-guide/man
    ifest-merger
  - Do not propagate aspect to its own attributes when using '*'.
  - Comparing sets (`if set1 < set2:`) is not allowed anymore in
    Skylark because it didn't work correctly anyway.
  - When --experimental_extra_action_top_level_only, Bazel reports
    extra-actions for actions registered by Aspects injected by a
    top-level rule (approximately).
  - Blacklists for proto_lang_toolchain() no longer have to be
    proto_library's.
  - Extra actions now contain aspect-related information.
  - Fix slicing bug where "abc"[:-4:-1] would give wrong answer

## Release 0.4.0 (2016-10-26)

```
Baseline: 088bbc6

Cherry picks:
   + b01160c: Stamp Windows release.
   + 2d6736e: Add --no-tty for gpg signing
   + 9b1dfb8: Remove .sig file before gpg signing
   + 81aede1: Reimplement whole archive on Windows
```

Incompatible changes:

  - Skylark: updating list/dicts while they are being looped over is not
    allowed. Use an explicit copy if needed ("for x in list(mylist):").
  - Bazel now uses the --cpu flag to look up Jvms; it falls back
    to "default" if it can't find a Jvm matching the CPU value.
  - --command_port=-1 to use AF_UNIX for client/server communications
    is not supported anymore.
  - Sandboxed actions can access the network by default, unless their
    target has a "block-network" tag.

New features:

  - Files now have an "extension" property in Skylark.

Important changes:

  - Added a new flag --sandbox_tmpfs_path, which asks the sandbox to
    mount an empty, writable directory at a specified path when
    running actions. (Supported on Linux only for now.)
  - Update protoc-3.0.0-mingw.exe to a working (statically linked)
    binary
  - apple_static_library rule to create multi-architecture static
    archive files from Objc/C++/Swift dependencies on apple platforms
  - JS: Add support for localization with closure managed rules.
  - Create a flag --android_dynamic_mode to turn off dynamic mode
    during the Android split transition.
  - Darwin sandboxing is default.
  - Remove flag --experimental_zip_tree_artifact from j2objc Java
    annotation processing support.
  - A few functions are added to BUILD files for consistency (hash,
    dir,
      hasattr, getattr) with .bzl files, although they are not very
    useful.
  - --watchfs is now a command option; the startup option of the same
        name is deprecated. I.e., use bazel build --watchfs, not
    blaze --watchfs
        build.

## Release 0.3.2 (2016-10-07)

```
Baseline: 023a7bd

Cherry picks:
   + bebbbe5: Fix dependency on libtool's helper script
              make_hashed_objlist.py.
   + 8a0d45f: Add the version information to the bazel.exe file
   + 2bc0939: Allow new_ rules to overwrited BUILD files in
              downloaded repos
   + c5545fd: Rollback of commit
              96d46280bc5a4803ba2242a4ad16939f85a3b212.
   + eb87208: Make cc_configure on Windows more robust
   + c30432c: Fix cc_configure on Windows
   + 95b16a8: sandbox: Replace the error-prone lazy cleanup of
              sandbox directories by a simple synchronous cleanup.
   + e898023: Fix #1849: Sandboxing on OS X should be turned off by
              default for 0.3.2.
   + ffdc05d: Add action_config and feature for linking on Windows
```

Incompatible changes:

  - If you maintain a rule that uses persistent workers, you'll have
    to specify execution_requirements={"supports-workers": 1} in the
    ctx.action that intends to run a tool with workers. The
    WorkerSpawnStrategy will alert you with a warning message if you
    forget to make this change and fallback to non-worker based
    execution.
  - It is now an error to include a precompiled library (.a, .lo, .so)
    in a cc_library which would generate a library with the same name
    (e.g., libfoo.so in cc_library foo) if that library also contains
    other linkable
    sources.
  - The main repository's execution root is under the main
    repository's workspace name, not the source directory's basename.
    This shouldn't
    have any effect on most builds, but it's possible it could break
    someone doing
    weird things with paths in actions.
  - Blaze doesn't support Unix domain sockets for communication
    between its client and server anymore. Therefore, the
    --command_port command line argument doesn't accept -1 as a valid
    value anymore.
  - Skylark: It is an error to shadow a global variable with a local
    variable after the global has already been accessed in the
    function.
  - bin_dir and genfiles_dir are now properties of ctx, not
    configuration. That is, to access the bin or genfiles directory
    from a
    Skylark rule, use ctx.bin_dir or ctx.genfiles_dir (not
    ctx.configuration.{bin,genfiles}_dir).  At the moment, you can
    access
    {bin,genfiles}_dir from either, but the ctx.configuration version
    will
    stop working in a future release.
  - filegroup-based C++ toolchains are not supported anymore.
    --*_crosstool_top options must always point to a
    cc_toolchain_suite rule (or an alias of one).
  - repository_ctx.{download,download_and_extract,execute} API now use
                   named parameters for optional parameters and no
    longer uses argument
                   type to distinguished between arguments
    (executable attribute name
                   must be specified when preceding optional
    arguments are missing).

New features:

  - print and fail are now available in BUILD files.

Important changes:

  - Added @bazel_tools//tools/build_defs/repo/git.bzl as a Skylark
    rule for Git repositories.
  - Added @bazel_tools//tools/build_defs/repo/maven_rules.bzl as a
    Skylark rule for Maven repositories.
  - Add global hash() function for strings (only)
  - Improve Android split transition handling.
  - Removes exports_manifest attribute from android_binary rule.
  - java_proto_library: control strict-deps through a rule-level and
    a package-level attribute.
  - Persistent workers are now used by default for Java compilation
    in Bazel, which should speed up your Java builds by ~4x. You can
    switch back to the old behavior via --strategy=Javac=standalone.
    Check out http://www.bazel.io/blog/2015/12/10/java-workers.html
    for more details.
  - objc_* rules can now depend on any target that returns an "objc"
    provider.
  - Adds support for NDK12 to `android_ndk_repository` rule in Bazel.
  - Test targets can disable the JUnit4 test security manager via a
    property.
  - Disable the Android split transition if --android_cpu and
    fat_apk_cpu are both empty.
  - New sandboxing implementation for Linux in which all actions run
    in a separate execroot that contains input files as symlinks back
    to the originals in the workspace. The running action now has
    read-write access to its execroot and /tmp only and can no longer
    write in arbitrary other places in the file system.
  - Add worker support to single jar.
  - Invoke source jar action as a worker.
  - Sandboxed builds allow network access for builds by default.
    Tests will still be run without networking, unless
    "requires-network" is specified as a tag.
  - Add path.realpath() method for Skylark repositories.
  - On Mac devices, detect locally installed versions of xcode to:
     1. Use a sensible default if xcode is required but
    --xcode_version is unspecified.
     2. Use sensible default iOS SDK version for the targeted version
    of xcode if ios_sdk_version is unspecified.
  - Emacs' [C-x `], a.k.a. next-error, works again in emacsen >= 25.1
  - swift_library can be used to build watchOS apps.
  - Exposes the is_device field on Apple platform objects and adds
    the apple_common.platform_type(name) method to retrieve a
    platform_type value that can be passed to methods like the Apple
    fragment's multi_arch_platform.
  - Move Skylark git_repository rules to git.bzl
  - Add support for aspects to attr.label() attributes
  - Global varaiables HOST_CFG and DATA_CFG are deprecated in favor
    of strings "host"
    and "data.
    Argument `cfg = "host"` or `cfg = "data"` is mandatory if
    `executable = True` is provided for a label.
  - The deprecation attribute of all rules now causes warnings
    to be printed when other targets depend on a target with that
    attribute set.
  - Change default of --[no]instrument_test_targets to false, change
    default --instrumentation_filter (which previously tried to
    exclude test targets by heuristic) to only exclude targets in
    javatests.
  - Remove deprecated absolute paths in blaze IDE artifacts
  - When using android_binary.manifest_merger="android" the merger
    produces a summary log next to the merged manifest artifact.
  - Allow different default mallocs per configuration.

## Release 0.3.1 (2016-07-29)

```
Baseline: 792a9d6

Cherry picks:
   + 25e5995: Rollback of commit
              a2770334ea3f3111026eb3e1368586921468710c.
   + 2479405: Fix NPE with unset maven_jar sha1
   + 3cf2126: Rewrite the extra action info files if the data within
              them changes.
   + 5a9c6b4: JavaBuilder: Reintroduce the -extra_checks flag.
```

Incompatible changes:

  - Removed predefined Python variable "generic_cpu".
  - Skylark rules: if you set "outputs" or an attribute to a
    function, this function must now list its required attributes as
    parameters (instead of an attribute map).
  - The host_platform and target_platform entries are not written to
    the master log anymore.
  - Bazel requires Hazelcast 3.6 or higher now for remote execution
    support, because we upgraded our client library and the protocol
    it uses is incompatible with older versions.

New features:

  - LIPO context (--lipo_context) can now also be a cc_test (in
    addition to cc_binary)

Important changes:

  - If --android_crosstool_top is set, native code compiled for
    android will always use --android_compiler and not --compiler in
    choosing the crosstool toolchain, and will use --android_cpu if
    --fat_apk_cpu is not set.
  - Add --instrument_test_targets option.
  - apple_binary supports a new platform_type attribute, which, if
    set to "watchos", will build dependencies for Apple's watchOS2.
  - objc_binary now supports late-loaded dynamic frameworks.
  - Native Swift rules no longer pull in module maps unconditionally.
    Use --experimental_objc_enable_module_maps for that.
  - Merged manifests are guaranteed to have the application element
    as the last child of the manifest element as required by Android
    N.
  - The Android manifest merger is now available as an option for
    android_binary rules. The merger will honor tools annotations in
    AndroidManifest.xml and will perform placeholder substitutions
    using the values specified in android_binary.manifest_values. The
    merger may be selected by setting the manifest_merger attribute
    on android_binary.
  - The progress message would not clear packages that need to be
    loaded twice.
  - Remove warning for high value of --jobs.
  - Use the correct build configuration for shared native deps during
    Android split transitions.
  - When building ObjectiveC++, pass the flag -std=gnu++11.
  - use xcrun simctl instead of iossim to launch the app for "blaze
    run".
  - Glob arguments 'exclude' and 'exclude_directories' must be named
  - Bazel no longer regards an empty file as changed if its mtime has
    changed.

## Release 0.3.0 (2016-06-10)

```
Baseline: a9301fa

Cherry picks:
   + ff30a73: Turn --legacy_external_runfiles back on by default
   + aeee3b8: Fix delete[] warning on fsevents.cc
```

Incompatible changes:

  - The --cwarn command line option is not supported anymore. Use
    --copt instead.

New features:

  - On OSX, --watchfs now uses FsEvents to be notified of changes
    from the filesystem (previously, this flag had no effect on OS X).
  - add support for the '-=', '*=', '/=', and'%=' operators to
    skylark.  Notably, we do not support '|=' because the semantics
    of skylark sets are sufficiently different from python sets.

Important changes:

  - Use singular form when appropriate in blaze's test result summary
    message.
  - Added supported for Android NDK revision 11
  - --objc_generate_debug_symbols is now deprecated.
  - swift_library now generates an Objective-C header for its @objc
    interfaces.
  - new_objc_provider can now set the USES_SWIFT flag.
  - objc_framework now supports dynamic frameworks.
  - Symlinks in zip files are now unzipped correctly by http_archive,
    download_and_extract, etc.
  - swift_library is now able to import framework rules such as
    objc_framework.
  - Adds "jre_deps" attribute to j2objc_library.
  - Release apple_binary rule, for creating multi-architecture
    ("fat") objc/cc binaries and libraries, targeting ios platforms.
  - Aspects documentation added.
  - The --ues_isystem_for_includes command line option is not
    supported anymore.
  - global function 'provider' is removed from .bzl files. Providers
    can only be accessed through fields in a 'target' object.

## Release 0.2.3 (2016-05-10)

```
Baseline: 5a2dd7a
```

Incompatible changes:

  - All repositories are now directly under the x.runfiles directory
    in the runfiles tree (previously, external repositories were at
    x.runfiles/main-repo/external/other-repo. This simplifies
    handling remote repository runfiles considerably, but will break
    existing references to external repository runfiles.
    Furthermore, if a Bazel project does not provide a workspace name
    in the WORKSPACE file, Bazel will now default to using __main__
    as the workspace name (instead of "", as previously). The
    repository's runfiles will appear under x.runfiles/__main__/.
  - Bazel does not embed protocol buffer-related rules anymore.
  - It is now an error for a cc rule's includes attribute to point to
    the workspace root.
  - Bazel warns if a cc rule's includes attribute points out of
    third_party.
  - Removed cc_* attributes: abi / abi_deps. Use select() instead.

New features:

  - select({"//some:condition": None }) is now possible (this "unsets"
    the attribute).

Important changes:

  - java_import now allows its 'jars' attribute to be empty.
  - adds crunch_png attribute to android_binary
  - Replace --java_langtools, --javabuilder_top, --singlejar_top,
    --genclass_top, and --ijar_top with
    java_toolchain.{javac,javabuilder,singlejar,genclass,ijar}
  - External repository correctness fix: adding a new file/directory
    as a child of a new_local_repository is now noticed.
  - iOS apps are signed with get-task-allow=1 unless building with -c
    opt.
  - Generate debug symbols (-g) is enabled for all dbg builds of
    objc_ rules.
  - Bazel's workspace name is now io_bazel. If you are using Bazel's
    source as an external repository, then you may want to update the
    name you're referring to it as or you'll begin seeing warnings
    about name mismatches in your code.
  - Fixes integer overflow in J2ObjC sources to be Java-compatible.
  - A FlagPolicy specified via the --invocation_policy flag will now
    match the current command if any of its commands matches any of
    the commands the current command inherits from, as opposed to
    just the current command.
  - The key for the map to cc_toolchain_suite.toolchains is now a
    string of the form "cpu|compiler" (previously, it was just "cpu").
  - Fix interaction between LIPO builds and C++ header modules.
  - Ctrl-C will now interrupt a download, instead of waiting for it to
    finish.
  - Proxy settings can now be specified in http_proxy and https_proxy
    environment variables (not just HTTP_PROXY and HTTPS_PROXY).
  - Skylark targets can now read include directories from
    ObjcProvider.
  - Expose parameterized aspects to Skylark.
  - Support alwayslink cc_library dependencies in objc binaries.
  - Import cc_library dependencies in generated Xcode project.

## Release 0.2.2b (2016-04-22)

```
Baseline: 759bbfe

Cherry picks:
   + 1250fda: Rollback of commit
              351475627b9e94e5afdf472cbf465f49c433a25e.
   + ba8700e: Correctly set up build variables for the correct pic
              mode for fake_binary rules.
   + 386f242: Automated [] rollback of commit
              525fa71b0d6f096e9bfb180f688a4418c4974eb4.
   + 97e5ab0: Fix cc_configure include path for Frameworks on OS X.
   + a20352e: cc_configure: always add -B/usr/bin to the list of gcc
              option
   + 0b26f44: cc_configure: Add piii to the list of supported
              cpu_value
   + 3e4e416: cc_configure: uses which on the CC environment variable
   + aa3dbd3: cc_configure.bzl: strip end of line when looking for
              the cpu
   + 810d60a: cc_configure: Add -B to compiler flag too
```

Patch release, only includes fixes to C++ auto-configuration.

## Release 0.2.1 (2016-03-21)

```
Baseline: 19b5675
```

Incompatible changes:

  - Skylark rules that are available from their own repository will
    now issue a warning when accessed through @bazel_tools.
  - Set --legacy_bazel_java_test to off by default. java_test will
    now have a slightly different behaviour, correctly emitting XML
    file but, as a downside, it needs correct declaration of the
    test suite (see https://github.com/bazelbuild/bazel/issues/1017).
  - Labels in .bzl files in remote repositories will be resolved
    relative to their repository (instead of the repository the
    Skylark rule is used in).
  - Renamed proto_java_library to java_proto_library.  The former
    is now deprecated and will print out a warning when used.
  - android_sdk now compiles android_jack on the fly from
    android_jar, which means android_jar must be a jar and
    android_jack is now deprecated. The Jack tools (jack, jill,
    resource_extractor) must be specified.
  - Any project that depended on the objc_options rule will be
    broken. Can be fixed by adding attrs (infoplists,copts) directly
    to rules depending on the options.
  - .aidl files correctly require import statements for types
    defined in the same package and the same android_library.

New features:

  - Experimental Windows support is available.
  - Experimental support for writing remote repository rules in
    Skylark is available.
  - iOS ipa_post_processor attribute allows for user-defined IPA
    edits.
  - Adds a to_json method to Skylark structs, providing conversion to
    JSON format.
  - Native python rule can depend on skylark rule as long as skylark
    rule provides 'py' provider.
  - When using both --verbose_failures and --sandbox_debug, Bazel
    prints instructions how to spawn a debugging shell inside the
    sandbox.
  - add flag --sandbox_add_path, which takes a list of additional
    paths as argument and mount these paths to sandbox.

Important changes:

  - @androidsdk//:org_apache_http_legacy added for the legacy Apache
    classes for android sdk version 23 and above.
  - Genrules correctly work when used with bazel run.
  - When namespace-sandbox is run with the -D (debug) flag and
    inside a terminal, it spawns a shell inside the sandbox to aid in
    debugging when the sandboxed command fails.
  - Added --artifact to workspace generator for generating workspace
    and build file rules from artifact coodrinates.
  - Specifying --experimental_android_resource_shrinking on the
    command line will enable a resource shrinking pass on
    android_binary targets that already use Proguard.
  - J2ObjC updated to 1.0.1 release.
  - Added "root_symlinks" and "symlinks" parameters to Skylark
    runfiles() method.
  - You can no longer use objc_binary targets for the xctest_app
    attribute of an ios_test rule.
  - Enable overriding jsonnet binaries and stdlib for Jsonnet rules.
  - mount target of /etc/resolv.conf if it is a symlink.
  - Tests that failed to build because execution was halted no longer
    print their status.
  - Bazel warns if a cc rule's includes attribute contains up-level
    references that escape its package.
  - Add repository_ctx.download and repository_ctx.download_and_extract
    function.

## Release 0.2.0 (2016-02-18)

```
Baseline: 9e100ac
```

Incompatible changes:

  - ObjC compile actions for J2ObjC-translated code now only has
    access to headers from the java deps of the associated original
    java rule.
    These compile actions no longer takes the compiler options
    specified in "copts" attribute on objc_binary/ios_test rules.
    J2ObjC dead code removal (enabled through flag
    "--j2objc_dead_code_removal") now happens *after* ObjC
    compilation.
  - maven_jar no longer supports separate artifact_id, group_id, and
    verison fields. This information should be provided in the
    artifact field,
    instead.

New features:

  - Better support for toolchains that don't have a dynamic linker.
  - build_file_content attribute added to new_git_repository,
    new_http_archive, and new_local_repository.
  - Add support for .tar.bz2 archives to http_archive rules.

Important changes:

  - The --skyframe flag is no longer available for the build command.
  - The --artifacts flag was removed from the dump command.
  - The sha256 attribute is now optional (although recommended!) for
    remote repository rules.
  - Add instrumented file provider support to Skylark rules.
  - Add imports attribute to native Python rules.
  - Allow overriding -gsplit-dwarf from copts.
  - Improved sandbox performance on XFS filesystems.

## Release 0.1.5 (2016-02-05)

```
Baseline: 3a95f35
   + 8378cd8: Rollback of commit
              a9b84575a32476a5faf991da22b44661d75c19b6.
```

Incompatible changes:

  - Set stamping to false by default (i.e., --nostamp)
  - Removed --objc_dump_syms_binary.
  - Removes --objc_gcov_binary flag.
  - Remove JAVAC "Make" variable
  - The startup flag --blaze_cpu is removed,

New features:

  - A new java test runner that support XML output and test filtering
    is supported. It can be used by specifying --nolegacy_bazel_java_test
    or by specifying the test_class attribute on a java_test.
  - Skylark aspects can now specify configuration fragment
    dependencies with fragments and host_fragments like rules can.

Important changes:

  - Support for downloading remote resources through proxies by
    setting HTTP_PROXY (or HTTPS_PROXY).
  - Timestamps within Android apks are removed to make apks
    deterministic.
  - Support aggregation over existing rules in Skylark extensions
    through native.rules and native.rule.
  - A tools/bazel script in the workspace will be executed
    as an opportunity to use a fixed version of Bazel (not
    implemented for the homebrew recipe yet).
  - --noimplicit_deps and --nohost_deps work correctly for Aspect
    attributes.
  - JDK-related targets are now available via @local_jdk (instead of
    @local-jdk).
  - j2objc tools can now be accessed via @bazel_j2objc, not
    @bazel-j2objc.
  - Repository rules must use names that are valid workspace names.
  - [rust] Update to Rust 1.6
  - Add support for .tar.xz archives to http_archive rules.
  - Make C++ modules compatible with tools using
    --compilation_prerequisites_only
  - [d] Update to DMD 2.070.0

## Release 0.1.4 (2016-01-15)

```
Baseline: e933d5e
   + 3d796fe: Rollback of commit
              ac6ed79e1a3fa6b0ca91657b28e2a35f7e49758c.
   + 7a02e5d: Fix installer under OS X
   + 848740c: Fix bazel version for debian package
   + 7751d43: Add a method for getting the root of a rule workspace
              to the Label method
```

Important changes:

  - add loadfiles() query operator, to find skylark files loaded by
    targets.
  - Added ability to declare and use aspects in Skylark.
  - Skylark load statements may now reference .bzl files via build
    labels, in addition to paths. In particular, such labels can be
    used to reference Skylark files in external repositories; e.g.,
    load("@my_external_repo//some_pkg:some_file.bzl", ...).
    Path-based loads are now deprecated and may be disabled in the
    future. Caveats: Skylark files currently do not respect package
    visibility; i.e., all Skylark files are effectively public. Also,
    loads may not reference the special //external package.
  - Relative paths can now be used for 'path' with
    new_local_repository and local_repository.

## Release 0.1.3 (2016-01-07)

```
Baseline: 23ad8f6
   + de2183d: Only depend on the WORKSPACE file for external files
              that are under the external/ directory, i.e. were
              created by Bazel.
   + f8f855c: Rollback of commit
              12bad3af0eade9c4b79d76f9e1c950ad2e3214c2.
   + f627562: Stop parsing the WORKSPACE file when a parse error is
              detected
   + 763f139: Add -fno-canonical-system-headers to CROSSTOOL files so
              that gcc doesn't resolve symlinks in .d files, which
              would confuse Blaze.
   + b95995b: Use openjdk7 as dependency for debian package of jdk7
              flavor
```

New features:

  - Skylark macros are now enabled in WORKSPACE file.
  - .bazelrc allows workspace-relative imports as "import
    %workspace%/path/to/rcfile"
  - Evaluate the query expression in a file by passing
    --query_file=<file> to query

Important changes:

  - Remove obsolete --objc_per_proto_includes flag.
  - iOS apps and extensions now have launch_storyboard
  - Passing multiple JVM options via a single --host_jvm_args flag is
    now deprecated. Pass each JVM option behind its own
    --host_jvm_args flag.
  - Resources defined locally on an android_library rule will respect
    the neverlink attribute.
  - Update Rust to 1.4
  - Fix resource handling for exported android_library rules
  - Files in external repositories are now treated as mutable, which
    will make the correctness guarantees of using external
    repositories stronger (existent), but may cause performance
    penalties.

## Release 0.1.2 (2015-11-20)

```
Baseline: ee0ade3
   + 1e66ccd: RELNOTES: Symlink dirents of directories containing a
              file named
              "DONT_FOLLOW_SYMLINKS_WHEN_TRAVERSING_THIS_DIRECTORY_VIA
              _A_RECURSIVE_TARGET_PATTERN" will *not* be traversed
              for transitive target patterns. The motivation here is
              to allow directories that intentionally contain wonky
              symlinks (e.g. foo/bar -> foo) to opt out of being
              consumed by Blaze. For example, given
   + f5773fc: Set the ijar MAX_BUFFER_SIZE to 256 MB
```

New features:

  - java_library now supports the proguard_specs attribute for
    passing Proguard configuration up to Android (not Java) binaries.
  - http_file can specify "executable" to make the downloaded file
    runnable.
  - Debian and tar packaging is now supported
    (see tools/build_defs/pkg/README.md).
  - cpxx_builtin_include_directory specifications allow more
    flexibility.
  - accept %crosstool_top% in cxx_builtin_include_directory
  - android_binary now supports proguard_apply_mapping to re-use a
    previously generated proguard mapping.

Important changes:

  - remove webstatusserver (--use_webstatusserver).
  - Add support for objc textual headers, which will not be compiled
    when modules are enabled.
  - actoolzip, momczip and swiftstdlibtoolzip have all been made into
    bash scripts and have been renamed to actoolwrapper, momcwrapper
    and swiftstdlibtoolwrapper respectively. The old versions will be
    deleted in a later change.
  - [rust] Add rust_bench_test and rust_doc_test rules and improve
    usability of rust_test tule.
  - Java rules now support a resource_strip_prefix attribute that
    allows the removal of path prefixes from Java resources.
  - [docker_build] incremental loading is default now.
    Specify explicitly //package:target.tar (with the .tar extension)
    to obtain the full image.
  - --ios_signing_cert_name allows specifying a cert for iOS app
    signing
  - Go rules for Bazel.
  - [jsonnet] Update to Jsonnet 0.8.1.
  - [jsonnet] Add vars and code_vars attributes to jsonnet_to_json to
    allow passing external variables to Jsonnet via --var and
    --code_var.
  - Adds --override_workspace_root blaze flag to hand-set
    workspace_root and mainGroup in xcodeproj.
  - Allow dots in package names.
  - When used as a forwarding rule (i.e., has no sources),
    android_library
    will also forward any exported_plugins in its dependencies.
  - Add support for Windows-created zip files with non-posix
    permissions.
  - [jsonnet] Add jsonnet_to_json_test rule for testing Jsonnet code.
  - C++ compile actions run in a sandbox now on systems that support
    sandboxed execution.
  - The names of the clang compilers in the Android NDK crosstool no
    longer reference gcc.
  - 420 dpi is now a valid density for andoid_binary.densities.
  - Bazel does strict validation of include files now to ensure
    correct incremental builds. If you see compilation errors when
    building C++ code, please make sure that you explicitly declare
    all header files in the srcs or hdrs attribute of your cc_*
    targets and that your cc_* targets have correct "deps" on
    cc_library's that they use.
  - [jsonnet] Fix jsonnet_to_json rule to read code_vars from
    code_vars instead of vars.
  - Tests, genrules, and Skylark actions without the
    "requires-network" tag will no longer be able to access the
    network.
  - C++ libraries no longer need includes = ["."] (or similar copts)
    to include paths relative to a remote repository's root.
  - Support exports attribute for android_library
  - Symlink dirents of directories containing a file named
    "DONT_FOLLOW_SYMLINKS_WHEN_TRAVERSING_THIS_DIRECTORY_VIA_A_RECURSI
    VE_TARGET_PATTERN" will *not* be traversed for transitive target
    patterns. The motivation here is to allow directories that
    intentionally contain wonky symlinks (e.g. foo/bar -> foo) to opt
    out of being consumed by Blaze.

## Release 0.1.1 (2015-10-05)

```
Baseline: 22616ae
   + 1ef338f: Rollback of "Propagates cc_library linkopts attribute
              to dependent objc_libraries.": breaks certain
              objc_binary build targets.
   + 5fb1073: Reintroduce an inconsistency check (albeit, in a weaker
              form) removed by a previous change that was trying to
              optimize away a filesystem call.
   + 6d00468b2eb976866cfb814d562e0d53a580a46f: Add IdlClass to the embedded default android tools
              repository and rearrange BuildJar's JarHelper so that
              it too can be embedded.
   + a5199039934a2e399a7201adc0d74e2f2d2b0ff3: Fixes Android integration tests by wiring up idlclass
              rules in integration environment.
```

Incompatible changes:

  - Bazel requires JDK 8 to run.
  - Attribute "copts" is removed from j2objc_library.

New features:

  - a cc_binary rule may list '.s' and '.asm' files in the srcs
  - Support for build with libsass.
  - labels in "linkopts" may match any label in either "deps" or
    "srcs" to be considered valid.
  - Maven servers that require username & password authentication are
    now supported (see maven_server documentation).

Important changes:

  - Support empty plist files
  - The <compatible-screens> section of the AndroidManifest.xml will
    not be overwritten if it already contains a <screen> tag for each
    of the densities specified on the android_binary rule.
  - Add Jsonnet rules to Bazel
  - Remove deprecated xcode_options flag.
  - Workspace names are now restricted to being in their base
    directory
    (that is, the names cannot contain up-level references or /./).
  - j2objc_library on Bazel now transpiles transitive proto_library
    dependencies. (Note that java_* rules in Bazel do not yet support
    protos; currently they ignore proto dependencies.)
  - new_http_archive can specify a root directory.
  - Adds support for dylibs on devices for Xcode 7.
  - [d] d_docs rules now depend on a d_binary, a d_library or
    d_source_library.
  - [docker] docker_build now set the permission to 0555 to files
    added to the layer, use `mode = "0644"` to use the legacy behavior.
  - android_binary now has a main_dex_proguard_specs attribute to
    specify which classes should be in the main dex.
  - [rust] Add rust_docs rule for generating rustdoc.
## Release 0.1.0 (2015-09-08)

```
Baseline: a0881e8
   + 87374e6: Make android_binary use a constant, hard-coded,
              checked-in debug key.
   + 2984f1c: Adds some safety checks in the Bazel installer
   + 4e21d90: Remove BUILD.glob and incorporate the necessary
              filegroups into the android_{ndk,sdk}_repository rules
              themselves.
   + 1ee813e: Fix Groovy rules to work with sandboxing
   + 8741978: Add initial D rules to Bazel.
   + 2c2e70d: Fix the installer and fixing the package shiped into
              binary version of Bazel.
```

Initial release.































