FROM ubuntu:mantic

WORKDIR /build/
ENV PYENV_ROOT="/root/.pyenv"
ENV PATH="/root/.poetry/bin:$PYENV_ROOT/shims:$PYENV_ROOT/bin:$PATH"

# java
RUN apt-get update -qq \
    && JVM_ARCH=$([ `lscpu | grep -o "aarch64"` ] && echo "arm64" || echo "amd64") \
    && apt-get install -y curl openjdk-21-jdk maven \
    && update-alternatives --set java /usr/lib/jvm/java-21-openjdk-${JVM_ARCH}/bin/java \
    && update-alternatives --set javac /usr/lib/jvm/java-21-openjdk-${JVM_ARCH}/bin/javac

# ghr
RUN apt-get install golang git -y \
    && go install github.com/tcnksm/ghr@latest

# graalvm
RUN GRAALVM_ARCH=$([ `lscpu | grep -o "aarch64"` ] && echo "aarch64" || echo "x64") \
    && curl https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.2/graalvm-community-jdk-21.0.2_linux-${GRAALVM_ARCH}_bin.tar.gz  -O -J -L \
    && tar xfz graalvm-community-jdk-21.0.2_linux-${GRAALVM_ARCH}_bin.tar.gz \
    && mv  graalvm-community-openjdk-21.0.2+13.1 .graalvm \
    && rm graalvm-community-jdk-21.0.2_linux-${GRAALVM_ARCH}_bin.tar.gz \
    && /build/.graalvm/bin/gu install native-image

# python
RUN git clone https://github.com/pyenv/pyenv.git ~/.pyenv \
    && apt-get install -y gcc libbz2-dev libsqlite3-dev libssl-dev make zlib1g-dev libffi-dev \
    && ~/.pyenv/bin/pyenv install 3.10.5 \
    && ~/.pyenv/bin/pyenv local 3.10.5 \
    && curl -sSL https://install.python-poetry.org | python3 -

WORKDIR /src
