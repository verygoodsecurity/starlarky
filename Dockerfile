FROM ubuntu:18.04

WORKDIR /build/
ENV PYENV_ROOT="/root/.pyenv"
ENV PATH="/root/.poetry/bin:$PYENV_ROOT/shims:$PYENV_ROOT/bin:$PATH"

# java
RUN apt-get update -qq \
    && apt-get install -y curl openjdk-11-jdk maven \
    && update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java \
    && update-alternatives --set javac /usr/lib/jvm/java-11-openjdk-amd64/bin/javac \
    # ghr
    && apt-get install golang git -y \
    && go get -u github.com/tcnksm/ghr \
    # graalvm
    && curl https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-21.1.0/graalvm-ce-java11-linux-amd64-21.1.0.tar.gz -O -J -L \
    && tar xfz graalvm-ce-java11-linux-amd64-21.1.0.tar.gz \
    && mv graalvm-ce-java11-21.1.0 .graalvm \
    && rm graalvm-ce-java11-linux-amd64-21.1.0.tar.gz \
    && /build/.graalvm/bin/gu install native-image \
    # python
    && git clone https://github.com/pyenv/pyenv.git ~/.pyenv \
    && apt-get install -y gcc libbz2-dev libsqlite3-dev libssl-dev make zlib1g-dev libffi-dev \
    && ~/.pyenv/bin/pyenv install 3.8.6 \
    && ~/.pyenv/bin/pyenv local 3.8.6 \
    && curl -sSL https://raw.githubusercontent.com/python-poetry/poetry/master/get-poetry.py | python -

WORKDIR /src
