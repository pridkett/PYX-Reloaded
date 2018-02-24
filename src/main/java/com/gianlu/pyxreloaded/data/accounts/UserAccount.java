package com.gianlu.pyxreloaded.data.accounts;

import com.gianlu.pyxreloaded.Consts;
import com.gianlu.pyxreloaded.data.JsonWrapper;
import com.gianlu.pyxreloaded.server.BaseCahHandler;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class UserAccount {
    public final String username;
    public final boolean admin;
    public final String email;
    private final Consts.AuthType auth;
    public final String avatarUrl;

    UserAccount(ResultSet set) throws SQLException, BaseCahHandler.CahException {
        username = set.getString("username");
        email = set.getString("email");
        auth = Consts.AuthType.parse(set.getString("auth"));
        admin = set.getBoolean("admin");
        avatarUrl = set.getString("avatar_url");
    }

    UserAccount(String username, String email, Consts.AuthType auth, @Nullable String avatarUrl) {
        this.username = username;
        this.email = email;
        this.auth = auth;
        this.avatarUrl = avatarUrl;
        this.admin = false;
    }

    public JsonWrapper toJson() {
        JsonWrapper obj = new JsonWrapper();
        obj.add(Consts.UserData.EMAIL, email);
        obj.add(Consts.GeneralKeys.AUTH_TYPE, auth.toString());
        obj.add(Consts.UserData.PICTURE, avatarUrl);
        obj.add(Consts.GeneralKeys.NICKNAME, username);
        obj.add(Consts.GeneralKeys.IS_ADMIN, admin);
        return obj;
    }
}
