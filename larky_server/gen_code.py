from grpc_tools import protoc

protoc.main((
    '',
    '--proto_path=./protos',
    '--python_out=.',
    '--grpc_python_out=.',
    'larky.proto',
))