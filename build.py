import shutil
from pathlib import Path


def build(setup_kwargs):
    runner_path = Path('runlarky') / 'target' / 'larky-runner'
    assert runner_path.exists()
    shutil.copy(runner_path, Path('pylarky'))
    return setup_kwargs
