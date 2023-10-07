package com.hawolt.mitm.interpreter.impl;

import com.hawolt.lcu.LCU;
import com.hawolt.lcu.LeagueClient;
import com.hawolt.lcu.WMIC;
import com.hawolt.mitm.interpreter.AbstractInstruction;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

public class LCUInstruction extends AbstractInstruction {

    @Override
    protected String modify(String id, String[] args) throws IOException {
        String method = args[1];
        LeagueClient client = WMIC.retrieve(args[2]);
        if (client == null) throw new IOException("WMIC was unable to find associated Client");
        byte[] body = args.length == 5 ? args[4].getBytes() : new byte[0];
        LCU lcu = new LCU(client, method, args[3], body);
        try (Response response = lcu.execute()) {
            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) throw new IOException("NO_RESPONSE_BODY");
                return responseBody.string();
            }
        }
    }

    @Override
    protected int getArguments() {
        return 3;
    }

    @Override
    public String getName() {
        return "lcu";
    }
}
