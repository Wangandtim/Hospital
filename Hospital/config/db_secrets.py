"""数据库凭据读取：优先环境变量 DB_PASSWORD，其次本地 config/db_password.txt。

db_password.txt 已被 .gitignore 排除，不会进入仓库；部署环境请使用环境变量注入。
"""
import os
from pathlib import Path

_LOCAL_FILE = Path(__file__).resolve().parent / "db_password.txt"


def db_password() -> str:
    pwd = os.environ.get("DB_PASSWORD")
    if pwd:
        return pwd
    if _LOCAL_FILE.exists():
        pwd = _LOCAL_FILE.read_text(encoding="utf-8").strip()
        if pwd:
            return pwd
    raise SystemExit(
        "缺少数据库密码：请设置环境变量 DB_PASSWORD，"
        "或将密码写入 Hospital/config/db_password.txt（该文件不入库）"
    )
