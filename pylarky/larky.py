import subprocess


def evaluate(script, input_file, output_file):
    print(subprocess.call(['larky-runner',
                           '-input', input_file,
                           '-output', output_file,
                           script]))
