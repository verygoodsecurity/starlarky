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


@pytest.mark.parametrize(
    "name", ["", "a=b.wasm", 3, "bad\0name.wasm", "x" * 256, "é" * 128]
)
def test_bad_module_name(name):
    with pytest.raises(ValueError):
        Evaluator("1", modules={name: WASM})


def test_longest_module_name():
    assert "x" * 255 in Evaluator("1", modules={"x" * 255: WASM}).modules


def test_module_size_limits():
    limit = evaluator_module.MAX_MODULE_BYTES
    assert len(Evaluator("1", modules={"a.wasm": bytes(limit)}).modules["a.wasm"]) == limit
    with pytest.raises(ValueError, match="the limit is"):
        Evaluator("1", modules={"a.wasm": bytes(limit + 1)})
    # str is measured as the UTF-8 it is written as: two bytes per 'é'.
    with pytest.raises(ValueError, match="the limit is"):
        Evaluator("1", modules={"a.star": "é" * (limit // 2 + 1)})
    files = evaluator_module.MAX_MODULES_TOTAL_BYTES // limit + 1
    with pytest.raises(ValueError, match="total more than"):
        Evaluator("1", modules={f"m{i}.wasm": bytes(limit) for i in range(files)})


def test_module_count_limit():
    many = {f"m{i}.wasm": WASM for i in range(evaluator_module.MAX_MODULES + 1)}
    with pytest.raises(ValueError, match="at most"):
        Evaluator("1", modules=many)
    many.popitem()
    assert len(Evaluator("1", modules=many).modules) == evaluator_module.MAX_MODULES


def test_runner_that_cannot_start_raises_failed_evaluation(monkeypatch):
    def cannot_start(*args, **kwargs):
        raise OSError(7, "Argument list too long")

    monkeypatch.setattr(evaluator_module.subprocess, "Popen", cannot_start)
    with pytest.raises(FailedEvaluation, match="Could not run"):
        Evaluator("1", modules={"echo.wasm": WASM}).evaluate("")


def test_bad_module_data():
    with pytest.raises(TypeError):
        Evaluator("1", modules={"echo.wasm": 3})


def test_runner_failure_raises(popen, monkeypatch):
    monkeypatch.setattr(FakePopen, "wait", lambda self: 2)
    with pytest.raises(FailedEvaluation):
        Evaluator("1", modules={"echo.wasm": WASM}).evaluate("")


@pytest.mark.parametrize("name", ["--script", "--debug", "--module", "-s"])
def test_option_like_module_names_fail_before_starting_the_runner(name, popen):
    with pytest.raises(ValueError, match="must not start"):
        Evaluator("1", modules={name: WASM}).evaluate("")
    assert popen.calls == []


@pytest.mark.parametrize("name", ["../outside.wasm", "/absolute.wasm", "a b;$(touch x).wasm"])
def test_module_names_do_not_choose_the_temporary_file_path(name, tmp_path):
    args = evaluator_module._write_modules({name: WASM}, str(tmp_path))
    assert args == ["--module", f"{name}={tmp_path / 'module-0'}"]
    assert list(tmp_path.iterdir()) == [tmp_path / "module-0"]
    assert (tmp_path / "module-0").read_bytes() == WASM


def test_explicit_wasm_policy_reaches_the_runner(popen):
    Evaluator("1", enable_wasm=True, timeout_ms=1000).evaluate("")
    (args, _), = popen.calls
    assert args[10:] == ["--enable-wasm", "--timeout-ms", "1000"]


def test_timeout_without_wasm_and_http_forwarding(popen):
    Evaluator("1", timeout_ms=1000).evaluate("")
    assert popen.calls[0][0][10:] == ["--timeout-ms", "1000"]
    evaluator = HttpEvaluator("1", enable_wasm=True, timeout_ms=1000)
    Evaluator.evaluate(evaluator, "")
    assert popen.calls[1][0][10:] == ["--enable-wasm", "--timeout-ms", "1000"]


@pytest.mark.parametrize("options", [
    {"enable_wasm": True}, {"enable_wasm": "true"},
    {"timeout_ms": 0}, {"timeout_ms": -1}, {"timeout_ms": True},
    {"timeout_ms": 1.5}, {"timeout_ms": "1000"}, {"timeout_ms": 2**63 - 1},
])
def test_invalid_execution_policy_fails_before_runner(options, popen):
    with pytest.raises((ValueError, TypeError)):
        Evaluator("1", **options).evaluate("")
    assert popen.calls == []
