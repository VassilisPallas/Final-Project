package org.sakaiproject.api.events;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.json.JSONObject;
import org.sakaiproject.api.pojos.UserEventOwner;
import org.sakaiproject.api.pojos.events.EventInfo;
import org.sakaiproject.api.pojos.events.Event;
import org.sakaiproject.api.json.JsonParser;
import org.sakaiproject.api.sync.CalendarRefreshUI;
import org.sakaiproject.customviews.CustomSwipeRefreshLayout;
import org.sakaiproject.general.Actions;
import org.sakaiproject.api.user.User;
import org.sakaiproject.sakai.AppController;
import org.sakaiproject.sakai.R;

import java.io.File;
import java.io.IOException;

/**
 * Created by vasilis on 10/20/15.
 * Get all the events
 */
public class UserEventsService {
    private final String tag_user_events = User.getUserEid() + " events";

    private Context context;
    private final Gson gson = new Gson();
    private JsonObjectRequest ownerDataRequest;
    private CalendarRefreshUI calendarRefreshUI;
    private org.sakaiproject.customviews.CustomSwipeRefreshLayout swipeRefreshLayout;

    /**
     * the UserEventsService constructor
     *
     * @param context the context
     */
    public UserEventsService(Context context, CalendarRefreshUI calendarRefreshUI) {
        this.context = context;
        this.calendarRefreshUI = calendarRefreshUI;
    }

    public void setSwipeRefreshLayout(CustomSwipeRefreshLayout swipeRefreshLayout) {
        this.swipeRefreshLayout = swipeRefreshLayout;
    }

    /**
     * make REST call and get the json responses, then parse them for the data
     *
     * @param eventUrl the url
     */
    public void getEvents(String eventUrl) {
        EventsCollection.getUserEventsList().clear();
        EventsCollection.getMonthEvents().clear();

        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setRefreshing(true);

        JsonObjectRequest eventsRequest = new JsonObjectRequest(Request.Method.GET, eventUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Event event = gson.fromJson(response.toString(), Event.class);
                JsonParser.parseUserEventJson(event);
                if (Actions.createDirIfNotExists(context, User.getUserEid() + File.separator + "events"))
                    try {
                        Actions.writeJsonFile(context, response.toString(), "userEventsJson", User.getUserEid() + File.separator + "events");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                for (int i = 0; i < EventsCollection.getUserEventsList().size(); i++) {
                    String owner = EventsCollection.getUserEventsList().get(i).getCreator();
                    String eventId = EventsCollection.getUserEventsList().get(i).getEventId();
                    String url;
                    String tag = User.getUserEid() + " " + EventsCollection.getUserEventsList().get(i).getEventId() + " event";
                    if (!owner.equals(User.getUserId())) {
                        getOwnerData(context.getResources().getString(R.string.url) + "profile/" + EventsCollection.getUserEventsList().get(i).getCreator() + ".json", i, tag);
                    }

                    String siteId = EventsCollection.getUserEventsList().get(i).getReference().replaceAll("/calendar/calendar/", "");
                    siteId = siteId.replaceAll("/main", "");
                    EventsCollection.getUserEventsList().get(i).setSiteId(siteId);
                    url = context.getResources().getString(R.string.url) + "calendar/event/" + siteId + "/" + eventId + ".json";

                    getEventInfo(url, i, tag);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(tag_user_events, error.getMessage());
                if (swipeRefreshLayout != null)
                    swipeRefreshLayout.setRefreshing(false);
            }
        });

        AppController.getInstance().addToRequestQueue(eventsRequest, tag_user_events);
    }

    private void getOwnerData(String url, final int index, final String tag) {

        ownerDataRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                UserEventOwner userEventOwner = gson.fromJson(response.toString(), UserEventOwner.class);
                JsonParser.getEventCreatorDisplayName(context, userEventOwner, index);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(tag, error.getMessage());
                error.printStackTrace();
                AppController.getInstance().addToRequestQueue(ownerDataRequest, tag);
            }
        });

        AppController.getInstance().addToRequestQueue(ownerDataRequest, tag);
    }

    private void getEventInfo(String url, final int index, final String tag) {

        JsonObjectRequest eventInfoRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                EventInfo userEventOwnerPojo = gson.fromJson(response.toString(), EventInfo.class);
                JsonParser.parseUserEventInfoJson(userEventOwnerPojo, index);
                if (Actions.createDirIfNotExists(context, User.getUserEid() + File.separator + "events"))
                    try {
                        Actions.writeJsonFile(context, response.toString(), EventsCollection.getUserEventsList().get(index).getEventId(), User.getUserEid() + File.separator + "events");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                if (index == EventsCollection.getUserEventsList().size() - 1) {
                    calendarRefreshUI.updateUI();

                    if (swipeRefreshLayout != null)
                        swipeRefreshLayout.setRefreshing(false);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(tag, error.getMessage());
                if (swipeRefreshLayout != null)
                    swipeRefreshLayout.setRefreshing(false);
            }
        });

        AppController.getInstance().addToRequestQueue(eventInfoRequest, tag);
    }

}