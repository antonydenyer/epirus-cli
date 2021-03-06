/*
 * Copyright 2019 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.epirus.console.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.epirus.console.config.CliConfig;
import io.epirus.console.utils.Version;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Updater {

    private CliConfig config;

    public Updater(CliConfig config) {
        this.config = config;
    }

    public void promptIfUpdateAvailable() {

        if (config.isUpdateAvailable()) {
            System.out.println(
                    String.format(
                            "Your current Epirus version is: "
                                    + config.getVersion()
                                    + ". The latest Version is: "
                                    + config.getLatestVersion()
                                    + ". To update, run: %s",
                            config.getUpdatePrompt()));
        }
    }

    public void onlineUpdateCheck() {
        OkHttpClient client = new OkHttpClient();

        RequestBody updateBody =
                new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("os", CliConfig.determineOS().toString())
                        .addFormDataPart("clientId", config.getClientId())
                        .addFormDataPart("data", "update_check")
                        .build();

        Request updateCheckRequest =
                new okhttp3.Request.Builder().url(config.getServicesUrl()).post(updateBody).build();

        try {
            Response sendRawResponse = client.newCall(updateCheckRequest).execute();
            JsonElement element;
            if (sendRawResponse.code() == 200
                    && sendRawResponse.body() != null
                    && (element = JsonParser.parseString(sendRawResponse.body().string())) != null
                    && element.isJsonObject()) {
                JsonObject rootObj = element.getAsJsonObject().get("latest").getAsJsonObject();
                String latestVersion = rootObj.get("version").getAsString();
                if (!latestVersion.equals(Version.getVersion())) {
                    config.setLatestVersion(latestVersion);
                    config.setUpdatePrompt(
                            rootObj.get(
                                            CliConfig.determineOS() == CliConfig.OS.WINDOWS
                                                    ? "install_win"
                                                    : "install_unix")
                                    .getAsString());
                    config.save();
                }
            }
        } catch (Exception ignored) {
        }
    }
}
