# 

Python dependencies: `pandas`, `shutil`

## `run-experiment` 

Complete scripts to perform an experiment. Run with `-h` to see it's arguments.

What is does:

1. runs `run-targets`
2. copies `$PROJECT_DIR/src/test/resources`
3. runs `parse-logs`
4. runs `compute-timestamps`
5. runs `overall-stats`
6. runs `jacoco merge`
7. runs `jacoco report`


# `run-targets` 

Allows to run multiple targets (incl. in parallel) and collecting jazzer logs.

# `jazzer-plot.py` 

Parses jazzer log and draws some charts

