package ru.itmo.sd.bash.res.utils;

import ru.itmo.sd.bash.res.commands.*;
import ru.itmo.sd.bash.res.utils.exceptions.WrongSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Executor {
    private final HashMap<String, Command> cmdStorage;

    public Executor() {
        cmdStorage = new HashMap<>();

        cmdStorage.put("cat", new CatCommand());
        cmdStorage.put("echo", new EchoCommand());
        cmdStorage.put("pwd", new PwdCommand());
        cmdStorage.put("wc", new WcCommand());
    }


    public InputStream run(List<Token> givenTokens, EnvManager envManager) {
        var inputStream = System.in;

        var pipedTokens = givenTokens.stream().collect(new PipeCollector());


        for (var tokenList : pipedTokens) {
            if (tokenList.isEmpty()) {
                throw new WrongSyntaxException("executor error");
            }

            var maybeCmd = tokenList.get(0);

            if (maybeCmd.getType() == Token.Type.ASSIGN) {
                inputStream = assignmentCase(maybeCmd, inputStream, envManager);
                continue;
            }

            if (cmdStorage.containsKey(maybeCmd.getInside())) {
                inputStream = defaultCase(
                        maybeCmd,
                        tokenList.subList(1, tokenList.size()),
                        inputStream,
                        envManager);

            }
        }

        return inputStream;
    }

    private InputStream assignmentCase(Token assignToken, InputStream inputStream, EnvManager envManager) {
        try {
            var parts = assignToken.getInside().split("=");

            var cmd = new AssignVarCommand();
            return cmd.run(inputStream, List.of(parts), envManager);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Utils.emptyInputStream();
    }


    private InputStream defaultCase(Token cmdToken,
                                    List<Token> restTokens,
                                    InputStream inputStream,
                                    EnvManager envManager) {
        try {
            var actualCmd = cmdStorage.get(cmdToken.getInside());
            var actualArgs =
                    restTokens
                            .stream()
                            .map(Token::getInside)
                            .collect(Collectors.toList());


            return actualCmd.run(inputStream, actualArgs, envManager);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Utils.emptyInputStream();
    }


}