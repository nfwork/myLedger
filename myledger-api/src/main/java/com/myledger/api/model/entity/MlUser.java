package com.myledger.api.model.entity;

/**
 * 与表 {@code ml_user} 对应的实体；当前用于 dbfound 用户模块 {@code user/user} 中 {@code login}、{@code getById} 等查询的列投影。
 * <p>
 * 通过 {@link com.github.nfwork.dbfound.starter.ModelExecutor#queryOne(com.nfwork.dbfound.core.Context, String, String, Class)}
 * 由 dbfound 注入。未开启 {@code underscore-to-camel-case}（默认为 false）时，列别名与 JavaBean 属性名一致（下划线），
 * 例如 {@code user_id} 对应 {@code getUser_id}/{@code setUser_id}。
 */
public class MlUser {

    private Long user_id;
    private String username;
    private String nickname;

    public MlUser() {
    }

    public Long getUser_id() {
        return user_id;
    }

    public void setUser_id(Long user_id) {
        this.user_id = user_id;
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
