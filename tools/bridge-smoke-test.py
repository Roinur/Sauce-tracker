#!/usr/bin/env python3
"""Small local-only smoke probe for a running Desktop Bridge."""

import argparse
import re
import ssl
import urllib.request
import urllib.parse


def request(url: str, token: str = "", body: bytes | None = None) -> tuple[int, bytes]:
    headers = {"X-Sauce-Token": token}
    if body is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, headers=headers, data=body, method="POST" if body is not None else "GET")
    context = ssl._create_unverified_context()
    with urllib.request.urlopen(req, timeout=8, context=context) as response:
        return response.status, response.read()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="https://localhost:17366")
    parser.add_argument("--unlock-code")
    parser.add_argument("--api-smoke", action="store_true")
    args = parser.parse_args()
    health_status, health = request(f"{args.base}/health")
    index_status, index = request(f"{args.base}/")
    css_status, css = request(f"{args.base}/reader.css")
    html = index.decode("utf-8")
    token_match = re.search(r"SAUCE_BRIDGE_TOKEN='([^']+)'", html)
    if not token_match:
        raise SystemExit("Bridge token was not injected into index.html")
    print(f"health={health_status} index={index_status} reader_css={css_status}:{len(css)}")
    has_browser = 'data-view="browser"' in html
    has_reader = 'id="reader"' in html
    print(f"browser_ui={has_browser} reader_ui={has_reader}")
    import json

    _, unlock_payload = request(f"{args.base}/api/unlock-status", token_match.group(1))
    unlock_state = json.loads(unlock_payload)
    print(f"bridge_unlocked={unlock_state.get('unlocked', False)} round={unlock_state.get('round')}")
    if args.unlock_code:
        status, payload = request(
            f"{args.base}/api/unlock",
            token_match.group(1),
            json.dumps({"code": args.unlock_code}).encode("utf-8"),
        )
        result = json.loads(payload)
        print(f"unlock={status} accepted={result.get('accepted', False)} unlocked={result.get('unlocked', False)}")
    try:
        private_status, private_payload = request(f"{args.base}/api/v2/profiles", token_match.group(1))
        private = json.loads(private_payload)
        print(f"profiles={private_status}:{len(private.get('profiles', []))}")
    except urllib.error.HTTPError as error:
        print(f"profiles={error.code}:locked")
        private = {}
    if args.api_smoke and private.get("profiles"):
        profile_id = private.get("active_profile_id") or private["profiles"][0]["id"]
        active = next((item for item in private["profiles"] if item["id"] == profile_id), private["profiles"][0])
        scope = ",".join(active.get("sources", []))

        def api_get(path: str, **params: object) -> dict:
            encoded = urllib.parse.urlencode({"profile_id": profile_id, "sources": scope, **params})
            _, payload = request(f"{args.base}{path}?{encoded}", token_match.group(1))
            return json.loads(payload)

        dashboard = api_get("/api/v2/dashboard")
        library = api_get("/api/v2/library", limit=20, offset=0)
        browser = api_get("/api/v2/browser/search", q="", limit=6, offset=0)
        print(f"dashboard_entries={dashboard.get('entries')} library_items={len(library.get('items', []))} browser_items={len(browser.get('items', []))} browser_errors={len(browser.get('errors', []))}")
        entries = library.get("items", [])
        entry = next((item for item in entries if item.get("source_id") == "mangadex"), entries[0] if entries else None)
        if entry:
            identity = {"source_id": entry["source_id"], "remote_id": entry["remote_id"]}
            chapters = api_get("/api/v2/reader/chapters", **identity).get("chapters", [])
            if chapters:
                manifest = api_get("/api/v2/reader/manifest", chapter_id=chapters[0]["id"], **identity)
                page_status, page = request(
                    f"{args.base}/api/v2/reader/page?" + urllib.parse.urlencode({"reader_session_id": manifest["reader_session_id"], "index": 0}),
                    token_match.group(1),
                )
                print(f"reader={entry['source_id']} chapters={len(chapters)} pages={manifest.get('page_count')} first_page={page_status}:{len(page)}")


if __name__ == "__main__":
    main()
