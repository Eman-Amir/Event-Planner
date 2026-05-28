package com.example.network;

import com.example.model.Event;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

public class MockEventInterceptor implements Interceptor {
    private final List<Event> eventsList = new ArrayList<>();
    private final Gson gson = new Gson();
    private long currentId = 1L;

    public MockEventInterceptor() {
        // Hydrate with elegant initial campus helper events
        addDefaultEvent("Spring Career Fair", "Connect with over 50 tech companies, get resume reviews, and apply for summer internships.", "2026-06-15", "10:00", "Conference", "Grand Ballroom Hall B");
        addDefaultEvent("AI & ML Workshop", "Hands-on coding session in TensorFlow and Jetpack Compose. Bring your laptop and questions.", "2026-07-02", "14:30", "Workshop", "Engineering Bldg Rm 304");
        addDefaultEvent("Alumni Dinner & Gala", "A premium networking dinner with distinguished alumni from various fields. Formal attire recommended.", "2026-06-20", "18:00", "Social", "University Alumni Center");
        addDefaultEvent("Startup Pitch Night", "Watch 6 student-led teams pitch their innovative ideas to real venture capital judges for a cash prize.", "2026-07-10", "19:00", "Meeting", "Auditorium Hall A");
        addDefaultEvent("Google AI Studio Build Lab", "A collaborative session dedicated to exploring the boundaries of modern AI-powered tools.", "2026-08-05", "11:15", "Workshop", "Computer Science Annex");
    }

    private void addDefaultEvent(String title, String desc, String date, String time, String category, String location) {
        eventsList.add(new Event(currentId++, title, desc, date, time, category, location));
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String urlString = request.url().toString();
        String method = request.method();

        // Introduce simulated latency for real progress bars/feedback
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Handle path router
        if (urlString.contains("api/events")) {
            String idStr = urlString.substring(urlString.indexOf("api/events") + "api/events".length()).replaceAll("^/", "");

            if (idStr.isEmpty()) {
                // Route: /api/events
                if ("GET".equalsIgnoreCase(method)) {
                    String json = gson.toJson(eventsList);
                    return createSuccessResponse(request, 200, json);
                } else if ("POST".equalsIgnoreCase(method)) {
                    String requestBody = readRequestBody(request);
                    Event newEvent = gson.fromJson(requestBody, Event.class);
                    newEvent.setId(currentId++);
                    eventsList.add(newEvent);
                    String json = gson.toJson(newEvent);
                    return createSuccessResponse(request, 201, json);
                }
            } else {
                // Route: /api/events/{id}
                try {
                    Long id = Long.parseLong(idStr);
                    if ("GET".equalsIgnoreCase(method)) {
                        Event found = findEventById(id);
                        if (found != null) {
                            return createSuccessResponse(request, 200, gson.toJson(found));
                        } else {
                            return createErrorResponse(request, 404, "Event not found with ID: " + id);
                        }
                    } else if ("PUT".equalsIgnoreCase(method)) {
                        String requestBody = readRequestBody(request);
                        Event updateData = gson.fromJson(requestBody, Event.class);
                        Event found = findEventById(id);
                        if (found != null) {
                            found.setTitle(updateData.getTitle());
                            found.setDescription(updateData.getDescription());
                            found.setDate(updateData.getDate());
                            found.setTime(updateData.getTime());
                            found.setCategory(updateData.getCategory());
                            found.setLocation(updateData.getLocation());
                            return createSuccessResponse(request, 200, gson.toJson(found));
                        } else {
                            return createErrorResponse(request, 404, "Event not found to update");
                        }
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        Event found = findEventById(id);
                        if (found != null) {
                            eventsList.remove(found);
                            return createSuccessResponse(request, 200, "{\"success\":true,\"deletedId\":" + id + "}");
                        } else {
                            return createErrorResponse(request, 404, "Event not found to delete");
                        }
                    }
                } catch (NumberFormatException e) {
                    return createErrorResponse(request, 400, "Invalid Event ID format");
                }
            }
        }

        // Fallthrough default mock endpoint
        return createErrorResponse(request, 404, "Endpoint mock not mapped");
    }

    private Event findEventById(Long id) {
        for (Event event : eventsList) {
            if (event.getId().equals(id)) {
                return event;
            }
        }
        return null;
    }

    private String readRequestBody(Request request) {
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            if (copy.body() != null) {
                copy.body().writeTo(buffer);
                return buffer.readUtf8();
            }
            return "";
        } catch (final IOException e) {
            return "";
        }
    }

    private Response createSuccessResponse(Request request, int statusCode, String bodyJson) {
        return new Response.Builder()
                .code(statusCode)
                .message("OK")
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .body(ResponseBody.create(bodyJson, MediaType.parse("application/json")))
                .addHeader("content-type", "application/json")
                .build();
    }

    private Response createErrorResponse(Request request, int statusCode, String message) {
        String errorJson = "{\"error\": \"" + message + "\"}";
        return new Response.Builder()
                .code(statusCode)
                .message("Error")
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .body(ResponseBody.create(errorJson, MediaType.parse("application/json")))
                .addHeader("content-type", "application/json")
                .build();
    }
}
