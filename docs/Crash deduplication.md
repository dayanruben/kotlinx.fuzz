# Crash deduplication in `kotlinx.fuzz`

Crash deduplication is the process of identifying and grouping identical or similar crash reports to reduce redundancy and improve debugging efficiency.

## Crash deduplication in Jazzer

`kotlinx.fuzz` currently uses [Jazzer](https://github.com/CodeIntelligenceTesting/jazzer) as its main fuzz engine. Jazzer already integrates some crash deduplication techniques, however, only the most basic ones.
Mainly, Jazzer can deduplicate only crashes that have exactly similar stack traces. Unfortunately, that does not reduce the number of crash reports significantly and Jazzer still requires a large amount of manual work to sort the results.

## `kotlinx.fuzz` improvements

In `kotlinx.fuzz` we use [LibCASR](https://docs.rs/libcasr/latest/libcasr/#libcasr), a Rust library that provides API for parsing stacktraces, collecting crash reports, triaging crashes (deduplication and clustering), and estimating severity of crashes. We implemented our custom [casr-adapter](https://github.com/plan-research/casr-adapter) library that provides JVM bindings for necessary APIs of LibCASR. This allows `kotlinx.fuzz` significantly reduce the amount of crashes found during the fuzzing campaign and also ensures that only unique crashes are shown to the user.
