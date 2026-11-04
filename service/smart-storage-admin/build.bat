@echo off
chcp 65001 >nul
:: 设置控制台标题（可选）
title ChinkFrame Builder

echo ==========================================
echo   [Win] 开始构建 ChinkFrame...
echo ==========================================
echo.

:: 核心构建命令
:: 注意：Windows 下换行使用 ^
:: 并且 ^ 后面不能有空格
mvn compile jib:build ^
  -DsendCredentialsOverHttp=true ^
  -Djib.allowInsecureRegistries=true ^
  -Djib.sendCredentialsOverHttp=true ^
  -Dmaven.wagon.http.ssl.insecure=true ^
  -Dmaven.wagon.http.ssl.allowall=true

:: 检查执行结果
if %ERRORLEVEL% EQU 0 (
    echo.
    echo 构建成功！
) else (
    echo.
    echo 构建失败！错误代码: %ERRORLEVEL%
)

:: 防止窗口立即关闭（可选，方便查看日志）
:: pause