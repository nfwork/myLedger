-- myLedger 全量建表：用户、资金账户、分类、流水
--
-- 请先在 MySQL 中创建目标库并授予应用账号所需权限，再将下方 USE 改为该库名，
-- 并与 myledger-api 数据源（application*.yml 中 JDBC URL）中的库名一致。

USE myledger;

-- ---------------------------------------------------------------------------
-- 用户（登录与数据归属）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ml_user (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(64)     NOT NULL COMMENT '登录名，唯一',
  password_hash VARCHAR(255)    NOT NULL COMMENT '密码哈希',
  nickname      VARCHAR(64)     NULL     COMMENT '显示昵称',
  created_at    DATETIME        NOT NULL COMMENT '创建时间（由应用写入）',
  updated_at    DATETIME        NOT NULL COMMENT '更新时间（由应用写入）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ml_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户';

-- ---------------------------------------------------------------------------
-- 资金账户（现金 / 银行卡 / 第三方支付等）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ml_account (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '所属用户',
  name        VARCHAR(64)     NOT NULL COMMENT '账户名称',
  is_default  TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否默认账户 1=是',
  sort_order  INT             NOT NULL DEFAULT 0 COMMENT '排序，小在前',
  created_at  DATETIME        NOT NULL,
  updated_at  DATETIME        NOT NULL,
  PRIMARY KEY (id),
  KEY idx_ml_account_user (user_id),
  CONSTRAINT fk_ml_account_user FOREIGN KEY (user_id) REFERENCES ml_user (id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='资金账户';

-- ---------------------------------------------------------------------------
-- 收支分类（一级，归属用户）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ml_category (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '所属用户',
  name        VARCHAR(64)     NOT NULL COMMENT '分类名称',
  type        ENUM('income','expense') NOT NULL COMMENT '收入/支出',
  sort_order  INT             NOT NULL DEFAULT 0 COMMENT '排序，小在前',
  created_at  DATETIME        NOT NULL,
  updated_at  DATETIME        NOT NULL,
  PRIMARY KEY (id),
  KEY idx_ml_category_user_type (user_id, type),
  CONSTRAINT fk_ml_category_user FOREIGN KEY (user_id) REFERENCES ml_user (id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='收支分类';

-- ---------------------------------------------------------------------------
-- 流水（单笔收入或支出）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ml_entry (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id      BIGINT UNSIGNED NOT NULL COMMENT '所属用户',
  account_id   BIGINT UNSIGNED NOT NULL COMMENT '资金账户',
  category_id  BIGINT UNSIGNED NOT NULL COMMENT '分类',
  entry_type   ENUM('income','expense') NOT NULL COMMENT '收入/支出',
  amount       DECIMAL(14,2)   NOT NULL COMMENT '金额，恒为正数',
  entry_date   DATE            NOT NULL COMMENT '业务日期',
  remark       VARCHAR(512)    NULL     COMMENT '备注',
  created_at   DATETIME        NOT NULL,
  updated_at   DATETIME        NOT NULL,
  PRIMARY KEY (id),
  KEY idx_ml_entry_user_date (user_id, entry_date),
  KEY idx_ml_entry_account (account_id),
  KEY idx_ml_entry_category (category_id),
  CONSTRAINT fk_ml_entry_user FOREIGN KEY (user_id) REFERENCES ml_user (id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_ml_entry_account FOREIGN KEY (account_id) REFERENCES ml_account (id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_ml_entry_category FOREIGN KEY (category_id) REFERENCES ml_category (id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_ml_entry_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='记账流水';
