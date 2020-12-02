import tempfile
from subprocess import STDOUT, check_output, CalledProcessError

RUNNER_EXECUATBLE = 'larky-runner'
LOG_PARAM = '-l'
INPUT_PARAM = '-i'
OUTPUT_PARAM = '-o'
SCRIPT_PARAM = '-s'


class Evaluator:

    def __init__(self, script: str, input_data: str):
        self.script = script
        self.input_data = input_data

    def evaluate(self) -> str:
        with tempfile.NamedTemporaryFile(mode='w+') as output_file, tempfile.NamedTemporaryFile(mode='w+') as input_file:
            input_file.write(self.input_data)
            input_file.flush()
            self.__evaluate(self.script, input_file.name, output_file.name)
            return output_file.read()

    def __evaluate(self, script, input_path, output_path):
        try:
            check_output([RUNNER_EXECUATBLE,
                          INPUT_PARAM, input_path,
                          OUTPUT_PARAM, output_path,
                          SCRIPT_PARAM, script], stderr=STDOUT)
        except CalledProcessError as e:
            raise FailedEvaluation(f'Starlark evaluation failed. \nOutput: {e.output}') from e


class FailedEvaluation(Exception):
    pass

