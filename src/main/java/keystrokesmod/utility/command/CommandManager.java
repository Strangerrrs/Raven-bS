package keystrokesmod.utility.command;

import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.impl.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager {
    private List<Command> commands = new ArrayList<>();
    public String[] latestAutoComplete = new String[]{};

    public CommandManager() {
        registerCommand(new Help());
        registerCommand(new Ping());
        registerCommand(new Name());
        registerCommand(new Binds());
        registerCommand(new Cname());
        registerCommand(new Debug());
        registerCommand(new Friend());
        registerCommand(new Enemy());
    }

    public void executeCommand(String input) {
        String[] args = input.split(" ");
        for (Command command : commands) {
            if (args[0].equalsIgnoreCase("." + command.command)) {
                command.onExecute(args);
                return;
            }

            if (command.alias == null || command.alias.length == 0) {
                continue;
            }

            for (String alias : command.alias) {
                if (args[0].equalsIgnoreCase("." + alias)) {
                    command.onExecute(args);
                    return;
                }
            }
        }

        Utils.sendMessage("§cunknown command. use .help for a list of commands.");
    }

    public boolean autoComplete(String input) {
        String[] completions = getCompletions(input);
        this.latestAutoComplete = completions != null ? completions : new String[]{};
        return input.startsWith(".") && this.latestAutoComplete.length > 0;
    }

    private String[] getCompletions(String input) {
        if (!input.isEmpty() && input.charAt(0) == '.') {
            String[] args = input.split(" ");

            if (args.length > 1) {
                Command command = getCommand(args[0].substring(1));
                if (command != null) {
                    List<String> tabCompletions = command.tabComplete(Arrays.copyOfRange(args, 1, args.length));
                    return tabCompletions.toArray(new String[0]);
                }
            } else {
                String rawInput = input.substring(1);
                List<String> completions = new ArrayList<>();
                for (Command command : commands) {
                    if (command.command.startsWith(rawInput) || Arrays.stream(command.alias).anyMatch(alias -> alias.startsWith(rawInput))) {
                        String alias = command.command.startsWith(rawInput) ? command.command : Arrays.stream(command.alias).filter(aliaz -> aliaz.startsWith(rawInput)).findFirst().orElse("");
                        completions.add("." + alias);
                    }
                }
                return completions.toArray(new String[0]);
            }
        }
        return null;
    }

    private Command getCommand(String name) {
        for (Command cmd : commands) {
            if (cmd.command.equalsIgnoreCase(name) || Arrays.stream(cmd.alias).anyMatch(alias -> alias.equalsIgnoreCase(name))) {
                return cmd;
            }
        }
        return null;
    }

    public void registerCommand(Command command) {
        commands.add(command);
    }
}
