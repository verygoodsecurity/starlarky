import re
import sys
import traceback
from contextlib import contextmanager
from functools import partial
from pathlib import Path
from tempfile import TemporaryDirectory


remove_load_statement = partial(
    re.compile(r'^load\([^)]+\)$', flags=re.MULTILINE).sub,
    '',
)


@contextmanager
def pythonize_larky_module(modname: str):
    try:
        mod_file = f'{modname}.star'
        src_mod_path = None
        for path in sys.path:
            src_mod_path = Path(path) / mod_file
            if src_mod_path.exists():
                break

        if not src_mod_path or not src_mod_path.exists():
            raise Exception(f'Unable to find star-module {modname}')

        tmpdir = TemporaryDirectory(prefix='larky_autodoc_')
        fake_import_path = tmpdir.name
        dst_mod_path = Path(fake_import_path) / f'{modname}.py'
        with open(src_mod_path) as src_mod_file, open(dst_mod_path, 'w') as dst_mod_file:
            mod_content = remove_load_statement(src_mod_file.read())
            dst_mod_file.write(mod_content)

        sys.path.insert(0, fake_import_path)

        try:
            yield
        finally:
            sys.path.remove(fake_import_path)
            tmpdir.cleanup()

    except BaseException as exc:
        # We need to wrap it in ImportError to be compartible with autodoc
        raise ImportError(exc, traceback.format_exc()) from exc
