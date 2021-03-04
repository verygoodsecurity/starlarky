FROM ubuntu:bionic

WORKDIR /build/

# java
RUN apt-get update
RUN apt-get install -y openjdk-11-jdk maven
RUN update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java
RUN update-alternatives --set javac /usr/lib/jvm/java-11-openjdk-amd64/bin/javac

# ghr
RUN apt-get install golang git -y
RUN go get -u github.com/tcnksm/ghr

# graal
RUN apt-get install -y curl
RUN curl https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.2.0/graalvm-ce-java11-linux-amd64-20.2.0.tar.gz -O -J -L
RUN tar xfz graalvm-ce-java11-linux-amd64-20.2.0.tar.gz
RUN mv graalvm-ce-java11-20.2.0 .graalvm
RUN rm graalvm-ce-java11-linux-amd64-20.2.0.tar.gz
RUN /build/.graalvm/bin/gu install native-image

# python
RUN git clone https://github.com/pyenv/pyenv.git ~/.pyenv
RUN apt-get install -y gcc libbz2-dev libsqlite3-dev libssl-dev make zlib1g-dev libffi-dev

ENV PYENV_ROOT="/root/.pyenv"
ENV PATH="$PYENV_ROOT/shims:$PYENV_ROOT/bin:$PATH"
RUN ~/.pyenv/bin/pyenv install 3.8.6
RUN ~/.pyenv/bin/pyenv local 3.8.6
RUN curl -sSL https://raw.githubusercontent.com/python-poetry/poetry/master/get-poetry.py | python -

ENV PYENV_ROOT /root/.pyenv/
ENV PATH /root/.poetry/bin:$PATH

WORKDIR /src
