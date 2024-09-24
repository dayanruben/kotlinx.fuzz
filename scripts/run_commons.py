import logging
import os
import subprocess
from os import path


def get_test_command(test_name):
    command = [
        "./gradlew",  "--no-daemon",
        ":test", "--tests", test_name
    ]
    return command

def run_test(test_name, home_dir, logs_dir, jacoco_exec_dir):
    subprocess.run("pwd")
    logging.debug("Running test" + test_name)

    command = get_test_command(test_name)
    logging.debug("command: " + str(command))
    my_env = os.environ.copy()
    my_env["JAZZER_FUZZ"] = "1"
    my_env["JAZZER_COVERAGE_DUMP"] = str(path.join(jacoco_exec_dir, test_name + ".exec"))

    # timestamp = datetime.datetime.now().strftime("%Y-%m-%d--%H-%M-%S")
    stdout_file = open(path.join(logs_dir, test_name), "w")
    stderr_file = open(path.join(logs_dir, test_name) + ".err", "w")
    subprocess.run(command, env=my_env, stderr=stderr_file, stdout=stdout_file, cwd=home_dir)
    stdout_file.close()
    stderr_file.close()
