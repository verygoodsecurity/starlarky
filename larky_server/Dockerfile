FROM python:3.6

# Create app directory
WORKDIR /app

COPY ./ ./larky_server
COPY ./artifacts/larky-runner /bin
COPY ./artifacts/pylarky ./pylarky
ENV PYTHONPATH=$PYTHONPATH:/app
ENV no_proxy=*
ENV GRPC_VERBOSITY=debug

RUN pip install -r larky_server/requirements.txt

EXPOSE 50051
CMD python larky_server/server.py
