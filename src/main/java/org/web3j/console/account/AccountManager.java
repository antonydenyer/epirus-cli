/*
 * Copyright 2020 Web3 Labs Ltd.
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
package org.web3j.console.account;

import java.io.IOException;
import java.util.Scanner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.web3j.console.config.CliConfig;

import static org.web3j.codegen.Console.exitError;

public class AccountManager {
    private static final String USAGE = "account login|logout|create";
    private static final String CLOUD_URL = "https://auth.epirus.io";

    public static void main(final CliConfig config, final String[] args) {
        OkHttpClient client = new OkHttpClient();

        Scanner console = new Scanner(System.in);
        switch (args[0]) {
            case "create":
                {
                    if (config.getLoginToken() != null) {
                        exitError(
                                "You are already logged in. To create a new account, please log out first.");
                    }

                    System.out.println("Please enter your email address: ");
                    String email = console.nextLine().trim();

                    RequestBody accountBody =
                            new MultipartBody.Builder()
                                    .setType(MultipartBody.FORM)
                                    .addFormDataPart("email", email)
                                    .build();

                    Request newAccountRequest =
                            new Request.Builder()
                                    .url(
                                            String.format(
                                                    "%s/auth/realms/EpirusPortal/web3j-token/create",
                                                    CLOUD_URL))
                                    .post(accountBody)
                                    .build();

                    try {
                        Response sendRawResponse = client.newCall(newAccountRequest).execute();
                        ResponseBody body;
                        if (sendRawResponse.code() == 200
                                && (body = sendRawResponse.body()) != null) {
                            String rawResponse = body.string();
                            JsonObject responseJsonObj =
                                    JsonParser.parseString(rawResponse).getAsJsonObject();
                            String token = responseJsonObj.get("token").getAsString();
                            config.setLoginToken(token);
                            System.out.println(
                                    "Account created successfully. You can now use Web3j Cloud. Please confirm your e-mail within 24 hours to continue using all features without interruption.");
                        } else {
                            System.out.println("Account creation failed. Please try again later.");
                        }
                        client.connectionPool().evictAll();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;
                }
            case "login":
                {
                    if (config.getLoginToken() != null) {
                        exitError(
                                "You are already logged in. To log in again, please log out first.");
                    }

                    System.out.println("Please enter your email address: ");
                    String email = console.nextLine().trim();
                    System.out.println("Please enter your password: ");
                    String password = console.nextLine().trim();

                    RequestBody loginBody =
                            new MultipartBody.Builder()
                                    .setType(MultipartBody.FORM)
                                    .addFormDataPart("username", email)
                                    .addFormDataPart("password", password)
                                    .build();

                    Request loginRequest =
                            new Request.Builder()
                                    .url(String.format("%s/api/api-token-auth/", CLOUD_URL))
                                    .post(loginBody)
                                    .build();

                    try {
                        Response sendRawResponse = client.newCall(loginRequest).execute();
                        ResponseBody body;
                        if (sendRawResponse.code() == 200
                                && (body = sendRawResponse.body()) != null) {
                            String rawResponse = body.string();
                            JsonObject responseJsonObj =
                                    JsonParser.parseString(rawResponse).getAsJsonObject();
                            String token = responseJsonObj.get("token").getAsString();
                            config.setLoginToken(token);
                            System.out.println(
                                    "You have been successfully logged in to Web3j Cloud.");
                        } else {
                            System.out.println(
                                    "Error while attempting to log you in. Please check your username and password, and if the problem persists, try again later.");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            case "logout":
                config.setLoginToken(null);
                System.out.println("Logged out successfully of Web3j Cloud.");
                break;
            default:
                exitError(USAGE);
                break;
        }
    }
}
