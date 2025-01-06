package keystrokesmod.utility.command.impl;

import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;

public class Friend extends Command {
    public Friend() {
        super("friend", new String[] { "friend", "f" });
    }

    @Override
    public void onExecute(String[] args) {
        if (args.length == 2) {
            if (args[1].equals("clear")) {
                chatWithPrefix("&b" + Utils.friends.size() + " &7friend" + (Utils.friends.size() == 1 ? "" : "s") + " cleared.");
                Utils.friends.clear();
                return;
            }

            boolean added = Utils.addFriend(args[1]);
            if (!added) {
                Utils.removeFriend(args[1]);
            }
        }
        else {
            syntaxError();
        }
    }
}
