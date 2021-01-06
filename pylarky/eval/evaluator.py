import pkg_resources
import tempfile
from subprocess import STDOUT, check_output, CalledProcessError

RUNNER_EXECUTABLE = pkg_resources.resource_filename('pylarky', 'larky-runner')
LOG_PARAM = '-l'
INPUT_PARAM = '-i'
OUTPUT_PARAM = '-o'
SCRIPT_PARAM = '-s'


class Evaluator:

    def __init__(self, script_data: str):
        self.script_data = script_data

    def evaluate(self, input_data: str) -> str:
        with tempfile.NamedTemporaryFile(mode='w+') as output_file, \
                tempfile.NamedTemporaryFile(mode='w+') as input_file, \
                tempfile.NamedTemporaryFile(mode="w+") as script_file:
            script_file.write(self.script_data)
            input_file.write(input_data)
            script_file.flush()
            input_file.flush()
            self.__evaluate(script_file.name, input_file.name, output_file.name)
            return output_file.read()

    def __evaluate(self, script_path, input_path, output_path):
        try:
            with tempfile.NamedTemporaryFile(mode='w+') as log_file:
                check_output([RUNNER_EXECUTABLE,
                              INPUT_PARAM, input_path,
                              OUTPUT_PARAM, output_path,
                              SCRIPT_PARAM, script_path,
                              LOG_PARAM, log_file.name], stderr=STDOUT)
        except CalledProcessError as e:
            raise FailedEvaluation(f'Starlark evaluation failed. \nOutput: {e.output}') from e


class FailedEvaluation(Exception):
    pass

