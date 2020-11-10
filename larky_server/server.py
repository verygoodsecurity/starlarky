import logging
from concurrent import futures

import grpc
import larky_pb2
import larky_pb2_grpc

from larky_server.utils import preprocess_larky_request, eval_request


class LarkyService(larky_pb2_grpc.ComputeService):

    def __init__(self):
        pass

    def Compute(self, request, context):
        p = preprocess_larky_request(
            script=request.script,
            main_fn=request.main_fn,
            ctx=request.ctx,
            payload=request.payload,
        )
        result = eval_request(p[0], p[1], p[2])
        return larky_pb2.StarlarkResponse(payload=result["payload"], ctx=result["ctx"])


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    larky_pb2_grpc.add_ComputeServiceServicer_to_server(LarkyService(), server)
    server.add_insecure_port('[::]:50051')
    server.start()
    server.wait_for_termination()


if __name__ == '__main__':
    logging.basicConfig()
    serve()
