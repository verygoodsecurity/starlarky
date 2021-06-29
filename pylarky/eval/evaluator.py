import pkg_resources
import tempfile
import sys
import subprocess

RUNNER_EXECUTABLE = pkg_resources.resource_filename("pylarky", "larky-runner")
LOG_PARAM = "-l"
INPUT_PARAM = "-i"
OUTPUT_PARAM = "-o"
SCRIPT_PARAM = "-s"


class Evaluator:
    def __init__(self, script_data: str):
        self.script_data = script_data

    def evaluate(self, input_data: str) -> str:
        with tempfile.NamedTemporaryFile(mode="w+") as output_file:
            with tempfile.NamedTemporaryFile(
                mode="w+"
            ) as input_file, tempfile.NamedTemporaryFile(mode="w+") as script_file:
                script_file.write(self.script_data)
                input_file.write(input_data)
                script_file.flush()
                input_file.flush()
                self.__evaluate(script_file.name, input_file.name, output_file.name)
            output_file.flush()
            return output_file.read()

    def __evaluate(self, script_path, input_path, output_path):
        try:
            with tempfile.NamedTemporaryFile(mode="w+") as log_file:
                proc = subprocess.Popen(
                    [
                        RUNNER_EXECUTABLE,
                        "-d",
                        INPUT_PARAM,
                        input_path,
                        OUTPUT_PARAM,
                        output_path,
                        SCRIPT_PARAM,
                        script_path,
                        LOG_PARAM,
                        log_file.name,
                    ],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                )
                output = []
                for line in proc.stdout:
                    line = line.decode(sys.stdout.encoding)
                    sys.stdout.write(line)
                    log_file.write(line)
                    output.append(line)
                log_file.flush()
                if proc.wait() != 0:
                    raise subprocess.CalledProcessError(
                        proc.returncode, proc.args, output="".join(output)
                    )
        except subprocess.CalledProcessError as e:
            raise FailedEvaluation(
                f"Starlark evaluation failed. \nOutput: {e.output}"
            ) from e


class FailedEvaluation(Exception):
    pass