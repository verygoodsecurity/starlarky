import os
import tempfile
from subprocess import STDOUT, check_output, CalledProcessError

LARKY_RUNNER_EXECUTABLE = os.environ.get('LARKY_RUNNER_EXECUTABLE', 'larky-runner')
LARKY_INPUT_PARAM = os.environ.get('LARKY_INPUT_PARAM', '-input')
LARKY_OUTPUT_PARAM = os.environ.get('LARKY_OUTPUT_PARAM', '-output')


def evaluate(script, input_data: str) -> str:
    with tempfile.NamedTemporaryFile(mode='w+') as output_file, tempfile.NamedTemporaryFile(mode='w+') as input_file:
        input_file.write(input_data)
        input_file.flush()
        __evaluate(script, input_file.name, output_file.name)
        return output_file.read()


def __evaluate(script, input_path, output_path):
    try:
        check_output([LARKY_RUNNER_EXECUTABLE,
                      LARKY_INPUT_PARAM, input_path,
                      LARKY_OUTPUT_PARAM, output_path,
                      script], stderr=STDOUT)
    except CalledProcessError as e:
        raise FailedEvaluation(f'Starlark evaluation failed. \nOutput: {e.output}') from e


class FailedEvaluation(Exception):
    pass
