package org.sakaiproject.api.login;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.sakaiproject.api.general.Actions;
import org.sakaiproject.api.general.ConnectionType;
import org.sakaiproject.api.json.JsonParser;
import org.sakaiproject.api.general.Connection;
import org.sakaiproject.api.user.profile.Profile;
import org.sakaiproject.api.user.User;
import org.sakaiproject.sakai.R;

/**
 * Created by vasilis on 10/12/15.
 */
public class OnlineLogin implements ILogin {

    private User user;
    private Profile profile;
    private JsonParser jsonParse;
    private InputStream inputStream;
    private String sessionId;
    private String loginJson;
    private String userDataJson;
    private String userProfileDataJson;
    private Context context;
    private Connection connection;

    public OnlineLogin(Context context) {
        this.context = context;
        jsonParse = new JsonParser();
        connection = Connection.getInstance();
        user = User.getInstance();
        profile = Profile.getInstance();
    }

    @Override
    public LoginType login(String... params) {
        try {
            String data = "_username=" + URLEncoder.encode(params[1], "UTF-8");
            data += "&";
            data += "_password=" + URLEncoder.encode(params[2], "UTF-8");

            connection.openConnection(params[0], ConnectionType.POST, false, false, data);

            Integer status = connection.getResponseCode();
            if (status >= 200 && status < 300) {
                inputStream = new BufferedInputStream(connection.getInputStream());
                sessionId = Actions.readJsonStream(inputStream);
                inputStream.close();
                connection.setSessionId(sessionId);
                getLoginJson(context.getResources().getString(R.string.url) + "session/" + sessionId + ".json");
                getUserDataJson(context.getResources().getString(R.string.url) + "user/" + user.getUserEid() + ".json");
                getUserProfileDataJson(context.getResources().getString(R.string.url) + "profile/" + user.getUserEid() + ".json");
                getUserImage(profile.getImageUrl());
                getUserThumbnailImage(profile.getImageThumbUrl());

                return LoginType.LOGIN_WITH_INTERNET;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.closeConnection();
        }

        return LoginType.INVALID_ARGUMENTS;
    }

    @Override
    public void getLoginJson(String... params) throws IOException {
        try {
            connection.openConnection(params[0], ConnectionType.GET, true, false, null);

            Integer status = connection.getResponseCode();
            if (status >= 200 && status < 300) {
                inputStream = new BufferedInputStream(connection.getInputStream());
                loginJson = Actions.readJsonStream(inputStream);
                inputStream.close();
                jsonParse.parseLoginResult(loginJson);
                Actions.writeJsonFile(context, loginJson, "loginJson");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.closeConnection();
        }
    }

    @Override
    public void getUserDataJson(String... params) throws IOException {
        try {
            connection.openConnection(params[0], ConnectionType.GET, true, false, null);

            Integer status = connection.getResponseCode();
            if (status >= 200 && status < 300) {
                inputStream = new BufferedInputStream(connection.getInputStream());
                userDataJson = Actions.readJsonStream(inputStream);
                inputStream.close();
                jsonParse.parseUserDataJson(userDataJson);
                Actions.writeJsonFile(context, userDataJson, "fullUserDataJson");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.closeConnection();
        }
    }

    @Override
    public void getUserProfileDataJson(String... params) throws IOException {
        try {
            connection.openConnection(params[0], ConnectionType.GET, true, false, null);

            Integer status = connection.getResponseCode();
            if (status >= 200 && status < 300) {
                inputStream = new BufferedInputStream(connection.getInputStream());
                userProfileDataJson = Actions.readJsonStream(inputStream);
                inputStream.close();
                jsonParse.parseUserProfileDataJson(userProfileDataJson);
                Actions.writeJsonFile(context, userProfileDataJson, "userProfileDataJson");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.closeConnection();
        }
    }

    @Override
    public void getUserImage(String... params) {
        Bitmap bitmap = null;
        try {
            connection.openConnection(params[0], ConnectionType.GET, false, false, null);

            Integer status = connection.getResponseCode();
            if (status >= 200 && status < 300) {
                InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                bitmap = BitmapFactory.decodeStream(inputStream);
                Actions.saveImage(context, bitmap, "user_image");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.closeConnection();
        }
    }

    @Override
    public void getUserThumbnailImage(String... params) {
        Bitmap bitmap = null;
        try {
            connection.openConnection(params[0], ConnectionType.GET, false, false, null);

            Integer status = connection.getResponseCode();
            if (status >= 200 && status < 300) {
                InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                bitmap = BitmapFactory.decodeStream(inputStream);
                Actions.saveImage(context, bitmap, "user_thumbnail_image");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.closeConnection();
        }
    }

}