import tempfile
import subprocess

RUNNER_EXECUATBLE = 'larky-runner'
INPUT_PARAM = '-input'
OUTPUT_PARAM = '-output'


def evaluate(script, input_data: str) -> str:
    output_file = tempfile.NamedTemporaryFile(mode='w+')
    input_file = tempfile.NamedTemporaryFile(mode='w+')
    input_file.write(input_data)
    input_file.flush()

    __evaluate(script, input_file.name, output_file.name)
    output = open(output_file.name, mode='r').read()

    input_file.close()
    output_file.close()

    return output


def __evaluate(script, input_path, output_path):
    try:
        subprocess.call([RUNNER_EXECUATBLE,
                         INPUT_PARAM, input_path,
                         OUTPUT_PARAM, output_path,
                         script])
    except Exception as e:
        raise FailedEvaluation(f'Starlark evaluation failed. Error: {e}')


class FailedEvaluation(Exception):
    pass
