-- =====================================================================
-- MySQL 启动自愈脚本
-- 通过 --init-file 在每次 mysqld 启动时执行（幂等）
-- 解决：数据目录被重置后，trans_platform 库缺失 + root@% 权限丢失
-- =====================================================================

-- 确保数据库存在
CREATE DATABASE IF NOT EXISTS trans_platform
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 确保 root@% 拥有全部权限（应用容器通过 Docker 网络连接）
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
