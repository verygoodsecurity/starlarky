FROM alpine:3.12.4 as build-statics
RUN apk add --update npm

ADD ./larky_editor /larky_editor
WORKDIR /larky_editor
RUN npm install .


FROM python:3.9-buster
WORKDIR /app
COPY --from=build-statics /larky_editor ./larky_editor
ADD requirements.txt /app
ADD server.py /app
RUN python -m pip install --upgrade pip
RUN pip3 install --use-deprecated=legacy-resolver pylarky==
RUN pip3 install -r requirements.txt

CMD ["python", "server.py"] 