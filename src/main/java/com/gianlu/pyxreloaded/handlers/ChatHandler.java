package com.gianlu.pyxreloaded.handlers;

import com.gianlu.pyxreloaded.Consts;
import com.gianlu.pyxreloaded.data.EventWrapper;
import com.gianlu.pyxreloaded.data.JsonWrapper;
import com.gianlu.pyxreloaded.data.QueuedMessage.MessageType;
import com.gianlu.pyxreloaded.data.User;
import com.gianlu.pyxreloaded.server.Annotations;
import com.gianlu.pyxreloaded.server.BaseCahHandler;
import com.gianlu.pyxreloaded.server.BaseJsonHandler;
import com.gianlu.pyxreloaded.server.Parameters;
import com.gianlu.pyxreloaded.singletons.ConnectedUsers;
import com.gianlu.pyxreloaded.singletons.Preferences;
import io.undertow.server.HttpServerExchange;
import org.jetbrains.annotations.NotNull;

public class ChatHandler extends BaseHandler {
    public static final String OP = Consts.Operation.CHAT.toString();
    private final ConnectedUsers users;
    private final boolean registeredOnly;

    public ChatHandler(@Annotations.Preferences Preferences preferences,
                       @Annotations.ConnectedUsers ConnectedUsers users) {
        this.users = users;
        this.registeredOnly = preferences.getBoolean("chat/registeredOnly", false);
    }

    @NotNull
    @Override
    public JsonWrapper handle(User user, Parameters params, HttpServerExchange exchange) throws BaseJsonHandler.StatusException {
        users.checkChatFlood(user);

        if (registeredOnly && !user.isEmailVerified())
            throw new BaseCahHandler.CahException(Consts.ErrorCode.ACCOUNT_NOT_VERIFIED);

        String msg = params.getStringNotNull(Consts.ChatData.MESSAGE);
        if (msg.isEmpty()) throw new BaseCahHandler.CahException(Consts.ErrorCode.BAD_REQUEST);

        if (msg.length() > Consts.CHAT_MAX_LENGTH) {
            throw new BaseCahHandler.CahException(Consts.ErrorCode.MESSAGE_TOO_LONG);
        } else if (!users.runChatCommand(user, msg)) {
            user.getLastMessageTimes().add(System.currentTimeMillis());
            EventWrapper ev = new EventWrapper(Consts.Event.CHAT);
            ev.add(Consts.ChatData.FROM, user.getNickname());
            ev.add(Consts.ChatData.MESSAGE, msg);
            ev.add(Consts.ChatData.FROM_ADMIN, user.isAdmin());
            if (user.getAccount() != null) ev.add(Consts.UserData.PICTURE, user.getAccount().avatarUrl);

            users.broadcastToAll(MessageType.CHAT, ev);
        }

        return JsonWrapper.EMPTY;
    }
}
