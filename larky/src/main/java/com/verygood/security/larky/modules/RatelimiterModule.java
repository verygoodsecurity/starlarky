package com.verygood.security.larky.modules;

import com.google.gson.Gson;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@StarlarkBuiltin(
        name = "ratelimiter",
        category = "BUILTIN",
        doc = "Envoy ratelimiter service")
public class RatelimiterModule implements StarlarkValue {

    public static final RatelimiterModule INSTANCE = new RatelimiterModule();

    @StarlarkMethod(
            name = "is_limited",
            doc = "check whether a request reaches ratelimit",
            parameters = {
                    @Param(
                            name = "domain",
                            doc = "A container for a set of rate limits. " +
                                    "All domains known to the Ratelimit service must be globally unique. " +
                                    "They serve as a way for different teams/projects to have rate limit configurations " +
                                    "that don't conflict.",
                            allowedTypes = {
                                    @ParamType(type = String.class),
                            }),
                    @Param(
                            name = "descriptors",
                            doc = "a dict of key/value pairs owned by a domain that the Ratelimit service uses to select" +
                                    " the correct rate limit to use when limiting",
                            allowedTypes = {
                                    @ParamType(type = Dict.class)
                            }),
                    @Param(
                            name = "limit",
                            doc = "Request per unit, dynamic to override default static configs",
                            named = true,
                            allowedTypes = {
                                    @ParamType(type = StarlarkInt.class),
                            }),
                    @Param(
                            name = "unit",
                            doc = "Ratelimit unit: SECOND, MINUTE, HOUR, DAY",
                            named = true,
                            defaultValue = "None",
                            allowedTypes = {
                                    @ParamType(type = String.class),
                            }),
            })
    public Boolean isLimited(String domain, Dict<String, Object> descriptors, StarlarkInt limit, String unit) throws IOException {
        URL url = new URL("http://localhost:8080/json");
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection)con;
        http.setRequestMethod("POST");
        http.setDoOutput(true);

        List<HashMap<String, Object>> entries = new ArrayList<>();
        for(Map.Entry<String, Object> entry: descriptors.entrySet()) {
            entries.add(new HashMap<String, Object>(){{
                put("key", entry.getKey());
                put("value", entry.getValue());
            }});
        }
        // need to construct json array with nested objects instead
        // String body = String.format("{\"domain\": %s, \"descriptors\": [{ \"entries\": %s,\"limit\": {\"requests_per_unit\": %s, \"unit\": %s}}] }", domain, entries.toString(), limit, unit);
        byte[] out = "{\"domain\": \"rl\", \"descriptors\": [{ \"entries\": [{ \"key\":  \"tenant\", \"value\": \"tnt03\" }, {\"key\": \"ip\", \"value\": \"111\"}],\"limit\": {\"requests_per_unit\": 5, \"unit\": \"MINUTE\"}}] }".getBytes(StandardCharsets.UTF_8);
        // byte[] out = body.getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.connect();

        OutputStream outputStream = http.getOutputStream();
        outputStream.write(out);
        InputStream inputStream = http.getInputStream();
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder responseBuilder = new StringBuilder();
        String inputStr;
        while ((inputStr = streamReader.readLine()) != null){
            responseBuilder.append(inputStr);
        }
        Gson g = new Gson();
        Map<String, Object> resp = g.fromJson(responseBuilder.toString(), Map.class);
        return resp.get("overallCode").toString().equals("OK");
    }
}
