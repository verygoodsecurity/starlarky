"""Checks the runner command line Evaluator builds; the runner itself is mocked."""
import os

import pytest

from pylarky.eval import evaluator as evaluator_module
from pylarky.eval.evaluator import Evaluator, FailedEvaluation
from pylarky.eval.http_evaluator import HttpEvaluator

WASM = b"\x00asm\x01\x00\x00\x00\xff"


class FakePopen:
    """Records each call, with the bytes of every --module file while it exists."""

    calls = []

    def __init__(self, args, stdout=None, stderr=None):
        self.args = list(args)
        self.returncode = 0
        modules = {}
        for i, arg in enumerate(self.args):
            if arg == "--module":
                name, path = self.args[i + 1].split("=", 1)
                with open(path, "rb") as f:
                    modules[name] = (path, f.read())
        FakePopen.calls.append((self.args, modules))
        self.stdout = []

    def wait(self):
        return self.returncode


@pytest.fixture
def popen(monkeypatch):
    FakePopen.calls = []
    monkeypatch.setattr(evaluator_module.subprocess, "Popen", FakePopen)
    return FakePopen


def test_no_modules_keeps_the_command_line(popen):
    Evaluator("1").evaluate("")

    (args, modules), = popen.calls
    assert args[0] == evaluator_module.RUNNER_EXECUTABLE
    assert [args[i] for i in (1, 2, 4, 6, 8)] == ["-d", "-i", "-o", "-s", "-l"]
    assert len(args) == 10
    assert "--module" not in args
    assert modules == {}


def test_modules_are_passed_as_name_equals_path(popen):
    Evaluator(
        "1", modules={"echo.wasm": WASM, "lib/helper.star": "x = 'é'\n"}
    ).evaluate("")

    (args, modules), = popen.calls
    assert args[10:12] == ["--module", args[11]]
    assert args[12] == "--module"
    assert len(args) == 14
    assert [a.split("=", 1)[0] for a in (args[11], args[13])] == [
        "echo.wasm",
        "lib/helper.star",
    ]
    assert modules["echo.wasm"][1] == WASM
    assert modules["lib/helper.star"][1] == "x = 'é'\n".encode("utf-8")
    # The module files are temporary.
    for path, _ in modules.values():
        assert not os.path.exists(path)


def test_http_evaluator_passes_modules(popen):
    evaluator = HttpEvaluator("1", modules={"echo.wasm": bytearray(WASM)})
    assert evaluator.modules == {"echo.wasm": WASM}


@pytest.mark.parametrize("name", ["", "a=b.wasm", 3])
def test_bad_module_name(name):
    with pytest.raises(ValueError):
        Evaluator("1", modules={name: WASM})


def test_bad_module_data():
    with pytest.raises(TypeError):
        Evaluator("1", modules={"echo.wasm": 3})


def test_runner_failure_raises(popen, monkeypatch):
    monkeypatch.setattr(FakePopen, "wait", lambda self: 2)
    with pytest.raises(FailedEvaluation):
        Evaluator("1", modules={"echo.wasm": WASM}).evaluate("")
