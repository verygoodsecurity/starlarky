from aiohttp import web
import asyncio
import aiohttp_cors
from pylarky.eval.evaluator import Evaluator
import traceback

routes = web.RouteTableDef()
routes.static('/larky_editor', 'larky_editor')

@routes.get('/')
async def root(request):
    return web.HTTPFound('/larky_editor/index.html')

@routes.post('/run')
async def run(request):
    try:
        evaluator = Evaluator(await request.text())
        result = evaluator.evaluate("")
        return web.Response(text=result)
    except Exception:
        return web.Response(text=traceback.format_exc())

if __name__ == "__main__":
    app = web.Application()
    cors = aiohttp_cors.setup(app, defaults={
    "*": aiohttp_cors.ResourceOptions(
            allow_credentials=True,
            expose_headers="*",
            allow_headers="*",
        )})
    app.add_routes(routes)
    for route in list(app.router.routes()):
        cors.add(route)
    web.run_app(app)
