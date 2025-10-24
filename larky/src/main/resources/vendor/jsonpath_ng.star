load("@stdlib//json", "json")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@vendor//option/result", Result="Result")


def _read(node, keys, error_safe=False):
    data = node
    # First key should always be $, ignore
    for index in range(1, len(keys)):
        key = keys[index]
        # print(key, data, types.is_dict(data), types.is_list(data))
        if types.is_dict(data):
            if key in data:
                data = data[key]
            else:
                msg = "ParsingException('Key \"{%s}\" does not exist in node')" % key
                if error_safe:
                    return Result.Error(msg)
                else:
                    fail(msg)
        elif types.is_list(data):
            if key > -1 and key < len(data):
                data = data[key]
            else:
                msg = "ParsingException('Key \"{%s}\" does not exist in node')" % key
                if error_safe:
                    return Result.Error(msg)
                else:
                    fail(msg)

    if error_safe:
        return Result.Ok(data)
    else:
        return data


def _write(node, keys, value):
    data = node
    for index in range(1, len(keys)):
        key = keys[index]
        is_last_key = index == len(keys) - 1
        if types.is_dict(data):
            # If this is the leaf node, write
            if is_last_key:
                # print('data to update:', data)
                data[key] = value
                return node
            elif key in data:
                data = data[key]

            else:
                data[key] = {}
                data = data[key]

        elif types.is_list(data):
            # If this is the leaf node, write
            if is_last_key:
                # print('data to update:', data)
                data[key] = value
                return node
            elif key > -1 and key < len(data):
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

    def find(node, error_safe=False):
        value = _read(node, self.parsed, error_safe=error_safe)
        if error_safe:
            # for error safe, we are already wrapping the return value in
            # result object (different one from _result).
            # ideally, the API should be consistent, but the current result
            # struct is widely used, so we have to keep it as is
            return value
        return _result(value)

    self.find = find

    def update(original, updated):
        return _result(_write(original, self.parsed, updated))

    self.update = update

    self = __init__(parsed)
    return self


def _parse_bracketed_key(text):
    quote_char = None
    if text.startswith('"'):
        quote_char = '"'
    elif text.startswith("'"):
        quote_char = "'"
    if quote_char != None:
        end_quote = text.find(quote_char, 1)
        return text[:end_quote + 1]
    close_bracket = text.find(']')
    return text[:close_bracket]


def _parse(query):
    keys = []
    element = ""
    index = 0

    for _while_ in larky.while_true():
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

            key = _parse_bracketed_key(query[index + 1:])
            key_len = len(key)
            if key.startswith("'") or key.startswith('"'):
                key = key[1:-1]
            elif key == "*":
                # key = key
                fail("ParsingException('Key \"*\" is not supported, please use for loop')")
            else:
                key = int(key)

            keys.append(key)
            index += key_len + 1
        elif query[index] == "*":
            fail("ParsingException('Key \"*\" is not supported, please use for loop')")
        else:
            element += query[index]

        index += 1

    if len(element):
        keys.append(element)

    # print('json path keys:', keys)
    return keys


def parse(query):
    keys = _parse(query)
    return datum(keys)


jsonpath_ng = larky.struct(
    parse=parse
)