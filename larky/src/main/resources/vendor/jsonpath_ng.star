load("@stdlib//json", "json")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")


_WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION


def _read(node, keys):
    data = node
    # First key should always be $, ignore
    for index in range(1, len(keys)):
        key = keys[index]
        print(key, data, types.is_dict(data), types.is_list(data))
        if types.is_dict(data):
            if key in data:
                data = data[key]
            else:
                fail("ParsingException('Key \"{%s}\" does not exist in node')" % key)
        elif types.is_list(data):
            if key > -1 and key < len(data):
                data = data[key]
            else:
                fail("ParsingException('Key \"{%s}\" does not exist in node')" % key)

    return data


def _write(node, keys, value):
    data = node
    for index in range(1, len(keys)):
        key = keys[index]
        if types.is_dict(data):
            # If this is the leaf node, write
            if index == len(keys) - 1:
                data[key] = value

            elif key in data:
                data = data[key]

            else:
                data[key] = {}
                data = data[key]

        elif types.is_list(data):
            if key > -1 and key < len(data):
                data = data[key]
            else:
                fail("ParsingException('Key \"{%s}\" does not exist in node')" % key)


def _nodeValue(node, query, value=None):
    keys = _parse(query)
    if value == None:
        return _read(node, keys)
    _write(node, keys, value)



def val(data, query, value=None):
    """
    Very basic implementation of jsonpath. All queries must start with $ and specify a full path
    """
    if not types.is_dict(data):
        fail("Data must be a dict!")
    return _nodeValue(data, query, value)


def _result(o):
    return larky.mutablestruct(
        __class__='result',
        value=o,
    )


def datum(parsed):
    self = larky.mutablestruct(__class__='datum')

    def __init__(parsed):
        self.parsed = parsed
        return self

    def find(node):
        return _result(_read(node, self.parsed))

    self.find = find

    def update(original, updated):
        return _result(_write(original, self.parsed, updated))

    self.update = update

    self = __init__(parsed)
    return self


def _parse(query):
    keys = []
    element = ""
    index = 0

    for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
        if index >= len(query):
            break
        if query[index] == ".":
            if len(element):
                keys.append(element)
                element = ""

        elif query[index] == "[":
            if len(element):
                keys.append(element)
                element = ""

            close = query.find("]", index)
            key = query[index + 1 : close]
            if key.startswith("'") and key.endswith("'"):
                key = key[1:-1]
            elif key == "*":
                key = key
            else:
                key = int(key)

            keys.append(key)
            index = close

        else:
            element += query[index]

        index += 1

    if len(element):
        keys.append(element)

    return keys


def parse(query):
    keys = _parse(query)
    return datum(keys)


jsonpath_ng = larky.struct(
    parse=parse
)