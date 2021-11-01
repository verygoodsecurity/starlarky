package com.verygood.security.larky.modules;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
    public Boolean isLimited(String domain, Dict<String, String> descriptors, StarlarkInt limit, String unit) throws IOException {
        String envoyURL = System.getenv("envoyURL");
        URL url;
        if (envoyURL != null) {
            url = new URL(envoyURL);
        } else {
            url = new URL("http://localhost:8080/json");
        }
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection)con;
        http.setRequestMethod("POST");
        http.setDoOutput(true);

        JsonArray entries = new JsonArray();
        for(Map.Entry<String, String> entry: descriptors.entrySet()) {
            JsonObject descriptorEntry = new JsonObject();
            descriptorEntry.addProperty("key", entry.getKey());
            descriptorEntry.addProperty("value", entry.getValue());
            entries.add(descriptorEntry);
        }
        JsonObject limitObj = new JsonObject();
        limitObj.addProperty("requests_per_unit", limit.toIntUnchecked());
        limitObj.addProperty("unit", unit);
        JsonObject descriptorObj = new JsonObject();
        descriptorObj.add("entries", entries);
        descriptorObj.add("limit", limitObj);
        JsonArray descriptorsArr = new JsonArray();
        descriptorsArr.add(descriptorObj);
        JsonObject body = new JsonObject();
        body.addProperty("domain", domain);
        body.add("descriptors", descriptorsArr);

        byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
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
        return resp.get("overallCode").toString().equals("OVER_LIMIT");
    }
}