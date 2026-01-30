FROM maven:3.8.5-openjdk-17

RUN microdnf install -y \
    gcc \
    gcc-c++ \
    make \
    glibc-devel \
    zlib-devel \
    libstdc++-static && \
    microdnf clean all

WORKDIR /app