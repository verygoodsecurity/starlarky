FROM ubuntu:kinetic

WORKDIR /build/
ENV PYENV_ROOT="/root/.pyenv"
ENV PATH="/root/.poetry/bin:$PYENV_ROOT/shims:$PYENV_ROOT/bin:$PATH"

RUN apt-get update -qq \
    && JVM_ARCH=$([ `lscpu | grep -o "aarch64"` ] && echo "arm64" || echo "amd64") \
    && GRAALVM_ARCH=$([ `lscpu | grep -o "aarch64"` ] && echo "aarch64" || echo "amd64") \
    # java
    && apt-get install -y curl openjdk-11-jdk maven \
    && update-alternatives --set java /usr/lib/jvm/java-11-openjdk-${JVM_ARCH}/bin/java \
    && update-alternatives --set javac /usr/lib/jvm/java-11-openjdk-${JVM_ARCH}/bin/javac \
    # ghr
    && apt-get install golang git -y \
    && go install github.com/tcnksm/ghr@latest \
    # graalvm
    && curl https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-21.1.0/graalvm-ce-java11-linux-${GRAALVM_ARCH}-21.1.0.tar.gz -O -J -L \
    && tar xfz graalvm-ce-java11-linux-${GRAALVM_ARCH}-21.1.0.tar.gz \
    && mv graalvm-ce-java11-21.1.0 .graalvm \
    && rm graalvm-ce-java11-linux-${GRAALVM_ARCH}-21.1.0.tar.gz \
    && /build/.graalvm/bin/gu install native-image \
    # python
    && git clone https://github.com/pyenv/pyenv.git ~/.pyenv \
    && apt-get install -y gcc libbz2-dev libsqlite3-dev libssl-dev make zlib1g-dev libffi-dev \
    && ~/.pyenv/bin/pyenv install 3.8.6 \
    && ~/.pyenv/bin/pyenv local 3.8.6 \
    && curl -sSL https://install.python-poetry.org | python3 -

WORKDIR /src
