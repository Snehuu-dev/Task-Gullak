# Task Gullak Vercel Serverless & Static Entrypoint
def handler(event, context):
    return {
        'statusCode': 200,
        'headers': {'Content-Type': 'text/html; charset=utf-8'},
        'body': 'Task Gullak'
    }

def app(environ, start_response):
    status = '200 OK'
    headers = [('Content-Type', 'text/html; charset=utf-8')]
    start_response(status, headers)
    try:
        with open('index.html', 'rb') as f:
            return [f.read()]
    except Exception:
        return [b'<!DOCTYPE html><html><head><title>Task Gullak</title></head><body><h1>Task Gullak</h1></body></html>']

application = app
