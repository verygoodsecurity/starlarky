import tempfile
import subprocess

RUNNER_EXECUATBLE = 'larky-runner'
INPUT_PARAM = '-input'
OUTPUT_PARAM = '-output'


def evaluate(script, input_data: str) -> str:
    output_file = tempfile.NamedTemporaryFile(mode='w+')
    input_path = __prepare_input_file(input_data)

    __evaluate(script, input_path, output_file.name)

    return open(output_file.name, mode='r').read()


def __evaluate(script, input_path, output_path):
    code = subprocess.call([RUNNER_EXECUATBLE,
                            INPUT_PARAM, input_path,
                            OUTPUT_PARAM, output_path,
                            script])
    if code != 0:
        raise FailedEvaluation(f'Evaluation returned non-zero return code. Code: {code}')


def __prepare_input_file(input_data: str) -> str:
    input_file = tempfile.NamedTemporaryFile(mode='w+')
    input_file.write(input_data)
    input_file.flush()

    return input_file.name


class FailedEvaluation(Exception):
    pass
