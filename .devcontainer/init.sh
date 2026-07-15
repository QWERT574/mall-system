#!/usr/bin/env bash
set -e

echo "=========================================="
echo "  乡村振兴农产品电商平台 - 环境初始化"
echo "=========================================="

echo ""
echo "[1/3] 等待 MySQL 就绪..."
# MySQL 容器首次启动会自动执行 /docker-entrypoint-initdb.d/ 下的建库脚本
for i in $(seq 1 30); do
  if (echo > /dev/tcp/mysql/3306) 2>/dev/null; then
    echo "  MySQL 端口已就绪"
    break
  fi
  echo "  等待 MySQL 启动... ($i/30)"
  sleep 2
done

echo ""
echo "[2/3] 安装前端依赖（web-mall / admin-web / seller-web）..."
cd /workspace/web-mall && npm install --no-audit --no-fund
cd /workspace/admin-web && npm install --no-audit --no-fund
cd /workspace/seller-web && npm install --no-audit --no-fund
echo "  前端依赖安装完成"

echo ""
echo "[3/3] 准备 Maven 依赖（首次会下载，可能需要几分钟）..."
cd /workspace/backend && mvn -q dependency:resolve -DskipTests || echo "  Maven 依赖解析失败，将在首次启动时重试"

echo ""
echo "=========================================="
echo "  ✅ 环境准备完成"
echo "=========================================="
echo ""
echo "启动方式（在终端中执行）："
echo ""
echo "  1) 启动后端："
echo "     cd backend && mvn spring-boot:run"
echo ""
echo "  2) 启动用户商城（端口 5176）："
echo "     cd web-mall && npm run dev"
echo ""
echo "  3) 启动管理后台："
echo "     cd admin-web && npm run dev"
echo ""
echo "  4) 启动商家端："
echo "     cd seller-web && npm run dev"
echo ""
echo "⚠️  如需使用 AI 智能客服功能，请先设置 DeepSeek API Key："
echo "     export DEEPSEEK_API_KEY=你的key"
echo ""
echo "启动后，点击 VS Code 底部 'PORTS' 标签页中的端口链接（8081/5176）即可在浏览器中访问。"
