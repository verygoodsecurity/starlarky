def modify():
    return {"body": ctx["body"], "headers": {"accept": "json", "content": "still-json"}}

modify()
