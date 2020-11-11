# Copyright 2015 gRPC authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""The Python implementation of the gRPC route guide client."""

from __future__ import print_function

import logging

import grpc
import larky_pb2
import larky_pb2_grpc


def make_request(script, ctx, main_fn, payload: str):
    new_ctx = {}
    for key, value in ctx.items():
        new_ctx[key] = bytes(value, 'utf-8')
    return larky_pb2.StarlarkRequest(script=script, main_fn=main_fn, ctx=new_ctx, payload=payload)


def default_request():
    return make_request(
        script="""
def process(payload, ctx):
    ctx['foo'] = 'baz'
    return payload[::1]
""",
        ctx={
            "foo": "bar"
        },
        main_fn="process",
        payload="asdf",
    )


def run():
    # NOTE(gRPC Python Team): .close() is possible on a channel and should be
    # used in circumstances in which the with statement does not fit the needs
    # of the code.
    with grpc.insecure_channel('[::]:50051',) as channel:
        stub = larky_pb2_grpc.LarkyRuntimeServiceStub(channel)
        print(stub.Compute(default_request()))


if __name__ == '__main__':
    logging.basicConfig()
    run()
