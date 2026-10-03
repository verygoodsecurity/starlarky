import os
import pkg_resources
import tempfile
import sys
import subprocess
from typing import Mapping, Optional, Union

RUNNER_EXECUTABLE = pkg_resources.resource_filename("pylarky", "larky-runner")
LOG_PARAM = "-l"
INPUT_PARAM = "-i"
OUTPUT_PARAM = "-o"
SCRIPT_PARAM = "-s"
MODULE_PARAM = "--module"

ModuleData = Union[bytes, bytearray, str]


class Evaluator:
    def __init__(
        self, script_data: str, modules: Optional[Mapping[str, ModuleData]] = None
    ):
        """
        :param script_data: the Starlark script.
        :param modules: files shipped with the evaluation, by name, e.g.
            ``{"echo.wasm": b"\\0asm..."}``. The script reads them by name
            (``wasm.module("echo.wasm")``), or load()s a ``.star`` one. str
            values are written as UTF-8.
        """
        self.script_data = script_data
        self.modules = _check_modules(modules)

    def evaluate(self, input_data: str) -> str:
        with tempfile.NamedTemporaryFile(mode="w+") as output_file:
            with tempfile.NamedTemporaryFile(
                mode="w+"
            ) as input_file, tempfile.NamedTemporaryFile(
                mode="w+"
            ) as script_file, tempfile.TemporaryDirectory() as module_dir:
                script_file.write(self.script_data)
                input_file.write(input_data)
                script_file.flush()
                input_file.flush()
                module_args = _write_modules(self.modules, module_dir)
                self.__evaluate(
                    script_file.name, input_file.name, output_file.name, module_args
                )
            output_file.flush()
            return output_file.read()

    def __evaluate(self, script_path, input_path, output_path, module_args=()):
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
                        *module_args,
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


def _check_modules(modules):
    if modules is None:
        return {}
    checked = {}
    for name, data in modules.items():
        if not isinstance(name, str) or not name:
            raise ValueError(f"module name must be a non-empty str; got {name!r}")
        if "=" in name:
            # The runner splits --module NAME=PATH at the first '='.
            raise ValueError(f"module name must not contain '='; got {name!r}")
        if isinstance(data, str):
            data = data.encode("utf-8")
        elif isinstance(data, (bytes, bytearray)):
            data = bytes(data)
        else:
            raise TypeError(
                f"module {name!r} must be bytes or str; got {type(data).__name__}"
            )
        checked[name] = data
    return checked


def _write_modules(modules, directory):
    """Writes each module to a file in directory; returns the runner's --module arguments."""
    args = []
    for i, (name, data) in enumerate(modules.items()):
        # The file name does not matter to the runner; NAME may contain '/'.
        path = os.path.join(directory, f"module-{i}")
        with open(path, "wb") as f:
            f.write(data)
        args += [MODULE_PARAM, f"{name}={path}"]
    return args
