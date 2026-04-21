package com.myledger.api.model.entity;

/**
 * 与表 {@code ml_user} 对应的实体；用于 dbfound 用户模块 {@code user/user} 等查询的列投影。
 * <p>
 * 通过 {@link com.github.nfwork.dbfound.starter.ModelExecutor#queryOne(com.nfwork.dbfound.core.Context, String, String, Class)}
 * 注入。强类型 JavaBean 下，SQL 列 {@code user_id} 等与驼峰属性 {@code userId} 由框架默认对齐，无需为 Bean 单独开启 {@code underscore-to-camel-case}（该配置面向 Map 等结果的键名策略）。
 */
public class MlUser {

    private Long userId;
    private String username;
    private String nickname;

    public MlUser() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
