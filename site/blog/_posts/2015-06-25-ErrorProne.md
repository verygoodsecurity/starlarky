---
layout: posts
title: Checking your Java errors with Error Prone.
---

We recently open-sourced our support for [Error Prone](https://errorprone.info).
[Error Prone](https://errorprone.info) checks for common mistakes in Java code
that will not be caught by the compiler.

We turned [Error Prone](https://errorprone.info) on by default but you can easily
turn it off by using the Javac option `-XepDisableAllChecks`. To do so, simply
specify `--javacopt='XepDisableAllChecks` to the list of Bazel's options. You
can also tune the checks error-prone will perform by using the [`-Xep:`
flags](https://errorprone.info/docs/flags).

See the [documentation of Error Prone](https://errorprone.info/docs/installation) for more
on Error Prone.
