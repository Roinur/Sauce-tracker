#!/usr/bin/env python3
"""Local QA proxy for the Bridge's self-signed HTTPS endpoint."""

import argparse
import http.server
import ssl
import urllib.error
import urllib.request


class Proxy(http.server.BaseHTTPRequestHandler):
    upstream = "https://localhost:17366"
    tls = ssl._create_unverified_context()

    def do_GET(self) -> None:
        self._forward()

    def do_POST(self) -> None:
        self._forward()

    def _forward(self) -> None:
        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length) if length else None
        headers = {
            key: value
            for key, value in self.headers.items()
            if key.lower() not in {"host", "connection", "content-length", "accept-encoding"}
        }
        request = urllib.request.Request(
            self.upstream + self.path,
            data=body,
            headers=headers,
            method=self.command,
        )
        try:
            response = urllib.request.urlopen(request, timeout=30, context=self.tls)
        except urllib.error.HTTPError as error:
            response = error
        payload = response.read()
        self.send_response(response.status)
        self.send_header("Content-Type", response.headers.get("Content-Type", "application/octet-stream"))
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, _format: str, *_args: object) -> None:
        return


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8766)
    parser.add_argument("--upstream", default="https://localhost:17366")
    args = parser.parse_args()
    Proxy.upstream = args.upstream.rstrip("/")
    http.server.ThreadingHTTPServer(("127.0.0.1", args.port), Proxy).serve_forever()


if __name__ == "__main__":
    main()
