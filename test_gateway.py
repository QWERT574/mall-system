# -*- coding: utf-8 -*-
"""
全量重启后端到端网关测试脚本
- 生成 JWT token（与 JwtUtil HS256 + 原始 UTF-8 密钥一致）
- 通过网关 (8080) 测试各微服务关键端点
"""
import json
import hmac
import hashlib
import base64
import time
import urllib.request
import urllib.error

SECRET = "ChangeMe_Use256bit_RandomKey_GeneratedBy_openssl"
GATEWAY = "http://localhost:8080"
USER_ID = 1
USERNAME = "admin"


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def gen_jwt(user_id: int, username: str, expires_in: int = 86400) -> str:
    header = {"alg": "HS256", "typ": "JWT"}
    now = int(time.time())
    payload = {
        "sub": str(user_id),
        "userId": user_id,
        "username": username,
        "iat": now,
        "exp": now + expires_in,
    }
    h = b64url(json.dumps(header, separators=(",", ":")).encode())
    p = b64url(json.dumps(payload, separators=(",", ":")).encode())
    signing_input = f"{h}.{p}".encode("ascii")
    sig = hmac.new(SECRET.encode("utf-8"), signing_input, hashlib.sha256).digest()
    return f"{h}.{p}.{b64url(sig)}"


def http_request(method, url, body=None, token=None, timeout=30):
    headers = {"Accept": "application/json"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", errors="replace")
    except Exception as e:
        return -1, str(e)


def truncate(s, n=300):
    s = s.replace("\n", " ")
    return s if len(s) <= n else s[:n] + "..."


def main():
    print("=" * 70)
    print("  MiniMall 全量重启后端到端测试")
    print("=" * 70)

    token = gen_jwt(USER_ID, USERNAME)
    print(f"\n[JWT] 已生成 token (userId={USER_ID}, username={USERNAME})")
    print(f"[JWT] token 前 60 字符: {token[:60]}...")

    results = []

    def test(name, method, path, body=None, need_auth=True, desc=""):
        url = f"{GATEWAY}{path}"
        t = token if need_auth else None
        code, resp = http_request(method, url, body, t)
        ok = code == 200
        results.append((name, ok, code, desc))
        status = "✅ PASS" if ok else "❌ FAIL"
        print(f"\n{status} [{code}] {name}")
        print(f"  {method} {path}")
        if desc:
            print(f"  说明: {desc}")
        print(f"  响应: {truncate(resp)}")
        return code, resp

    # ========== 1. 公开端点（无需认证）==========
    print("\n" + "─" * 70)
    print("  一、公开端点（网关白名单，无需认证）")
    print("─" * 70)

    test("商品列表", "GET", "/api/product/list?page=1&size=3",
         need_auth=False, desc="product-service 商品分页查询")
    test("FAQ模板列表", "GET", "/api/faq/templates",
         need_auth=False, desc="ai-service FAQ 模板查询")
    test("AI问答", "POST", "/api/ai/query",
         body={"query": "hello recommend product", "userId": 1, "serviceType": 1},
         need_auth=False, desc="ai-service AI 助手问答")
    test("AI聊天", "POST", "/api/ai/chat",
         body={"query": "hi", "userId": 1},
         need_auth=False, desc="ai-service AI 聊天端点")
    test("活动列表", "GET", "/api/activity/list",
         need_auth=False, desc="product-service 活动列表")
    test("优惠券可用列表", "GET", "/api/coupon/available",
         need_auth=False, desc="product-service 可用优惠券")

    # ========== 2. 需认证端点（携带 JWT）==========
    print("\n" + "─" * 70)
    print("  二、认证端点（携带 JWT token）")
    print("─" * 70)

    test("商品详情", "GET", "/api/product/1",
         desc="product-service 商品详情(公开)")
    test("聊天会话列表", "GET", "/api/chat/sessions",
         desc="chat-service 当前用户会话列表")
    test("聊天未读数", "GET", "/api/chat/unread/count",
         desc="chat-service 未读消息总数")
    test("订单列表", "GET", "/api/order/list?page=1&size=5",
         desc="order-service 当前用户订单列表")
    test("支付-发起支付(不存在订单)", "POST", "/api/payment/pay/999999",
         body={}, desc="payment-service 对不存在的订单发起支付，预期业务错误而非404")

    # ========== 汇总 ==========
    print("\n" + "=" * 70)
    print("  测试结果汇总")
    print("=" * 70)
    passed = sum(1 for _, ok, _, _ in results if ok)
    failed = len(results) - passed
    for name, ok, code, desc in results:
        mark = "✅" if ok else "❌"
        print(f"  {mark} [{code}] {name}  — {desc}")
    print(f"\n  总计: {len(results)} 项 | 通过: {passed} | 失败: {failed}")
    print("=" * 70)


if __name__ == "__main__":
    main()
