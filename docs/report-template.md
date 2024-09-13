# Template for creating the kotlinx.fuzz library report

## General info

* Library name
* Link to library sources
* Version used for testing
* ???

## Overall impressions with the library

* How easy/hard is it to fuzz?
* How easy/hard is it to write harnesses? Oracles?
* How hard it is to find bugs/interpret them?
* What makes it good or bad target for fuzzing?

## Fuzzing report

* Link to the kotlinx.fuzz repo branch with the harnesses that were used for fuzzing
* What parts of the library did we fuzz? How did we pripritize them? With links, if possible
* Description of all the experiments conducted
    * How long
    * What setup
    * ...
* ???


### Bug reports

All the bugs/interesting examples that were found during fuzzing. Each bug report should contain:
* Reproduction paackage (test harness, how long to run it, ...)
* Bug description: exception/error message, expected behaviour, actual behaviour
* Relevant links
* Severity/priority approximation (?)

When possible, we should try to order the bugs by their severity/priority