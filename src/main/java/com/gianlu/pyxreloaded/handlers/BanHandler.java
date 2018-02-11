/*
 * Handler to ban users via either nick or IP.
 */
package com.gianlu.pyxreloaded.handlers;

import com.gianlu.pyxreloaded.Consts;
import com.gianlu.pyxreloaded.EventWrapper;
import com.gianlu.pyxreloaded.JsonWrapper;
import com.gianlu.pyxreloaded.data.ConnectedUsers;
import com.gianlu.pyxreloaded.data.QueuedMessage;
import com.gianlu.pyxreloaded.data.QueuedMessage.MessageType;
import com.gianlu.pyxreloaded.data.User;
import com.gianlu.pyxreloaded.servlets.Annotations;
import com.gianlu.pyxreloaded.servlets.BanList;
import com.gianlu.pyxreloaded.servlets.BaseCahHandler;
import com.gianlu.pyxreloaded.servlets.Parameters;
import io.undertow.server.HttpServerExchange;
import org.apache.log4j.Logger;

public class BanHandler extends BaseHandler {
    public static final String OP = Consts.Operation.BAN.toString();
    protected final Logger logger = Logger.getLogger(BanHandler.class);
    private final ConnectedUsers connectedUsers; //Presumably between here to line 28, we get the list of users currently connected.

    public BanHandler(@Annotations.ConnectedUsers ConnectedUsers connectedUsers) {
        this.connectedUsers = connectedUsers;
    }

    @Override
    public JsonWrapper handle(User user, Parameters params, HttpServerExchange exchange) throws BaseCahHandler.CahException {
        if (!user.isAdmin())
            throw new BaseCahHandler.CahException(Consts.ErrorCode.NOT_ADMIN); //Detect that user doesn't have permission to kick/ban etc

        String nickname = params.get(Consts.GeneralKeys.NICKNAME); //Set a variable "nickname" to the one entered through the command.

        //Assuming this is for when the command wasn't properly typed
        if (nickname == null || nickname.isEmpty())
            throw new BaseCahHandler.CahException(Consts.ErrorCode.NO_NICK_SPECIFIED);

        String banIp;
        User kickUser = connectedUsers.getUser(nickname); //Single out the user we want to ban, give it its own object

        /*
         * Assuming singled out user exists, set banIP to the IP(?) of the user in question.
         * Then, send a message via the LongPoll servlet to the client with a notif they've been kicked.
         * Remove the user in question from the list of connected users on the server
         * To everyone else: Send a message of who banned who
         */
        if (kickUser != null) {
            banIp = kickUser.getHostname();

            kickUser.enqueueMessage(new QueuedMessage(MessageType.KICKED, new EventWrapper(Consts.Event.BANNED)));

            connectedUsers.removeUser(kickUser, Consts.DisconnectReason.BANNED);
            logger.info(String.format("Banning %s (%s) by request of %s", kickUser.getNickname(), banIp, user.getNickname()));
        } else { //Ban via nickname instead of IP address.
            banIp = nickname;
            logger.info(String.format("Banning %s by request of %s", banIp, user.getNickname()));
        }

        BanList.add(banIp); //Whatever banIp was determined to be, it was documented server-side right here.
        return JsonWrapper.EMPTY; //Doesn't return any JSON
    }
}