package net.socialgamer.cah.task;

import net.socialgamer.cah.data.ConnectedUsers;

import java.util.concurrent.ScheduledThreadPoolExecutor;


/**
 * Timer task to check for disconnected and idle clients.
 *
 * @author Andy Janata (ajanata@gmail.com)
 */
public class UserPingTask extends SafeTimerTask {
    private final ConnectedUsers users;
    private final ScheduledThreadPoolExecutor globalTimer;

    public UserPingTask(ConnectedUsers users, ScheduledThreadPoolExecutor globalTimer) {
        this.users = users;
        this.globalTimer = globalTimer;
    }

    @Override
    public void process() {
        users.checkForPingAndIdleTimeouts();
        globalTimer.purge();
    }
}