#!/bin/bash

echo "=========================================="
echo "  [Mac] 开始构建 ChinkFrame..."
echo "=========================================="

# 核心构建命令
# 注意：Mac 下换行用 \
mvn compile jib:build \
  -DsendCredentialsOverHttp=true \
  -Djib.allowInsecureRegistries=true \
  -Djib.sendCredentialsOverHttp=true \
  -Dmaven.wagon.http.ssl.insecure=true \
  -Dmaven.wagon.http.ssl.allowall=true

# 检查执行结果
if [ $? -eq 0 ]; then
    echo ""
    echo "构建成功！🎉"
else
    echo ""
    echo "构建失败！"
fi