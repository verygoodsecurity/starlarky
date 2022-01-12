from typing import Dict

import grpc
from pylarky.eval import larky_pb2, larky_pb2_grpc
from pylarky.model.http_message import HttpMessage


class Evaluator:
    def __init__(self, script_data: str, grpc_host: str = "localhost", grpc_port: int = 8080):
        self.script_data = script_data
        self.grpc_host = grpc_host
        self.grpc_port = grpc_port

    def evaluate(self, http_message, context: Dict[str, str] = None) -> HttpMessage:
        if context is None:
            context = {}
        channel = grpc.insecure_channel(f'{self.grpc_host}:{self.grpc_port}')
        stub = larky_pb2_grpc.LarkyProcessServiceStub(channel)
        modified_data = stub.Process(larky_pb2.LarkyProcessRequest(script=self.script_data,
                                                                   input=http_message.data.encode('utf-8'),
                                                                   context=context))
        print(modified_data)
        return HttpMessage(http_message.url, modified_data, http_message.headers)


class FailedEvaluation(Exception):
    pass
