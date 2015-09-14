/**
 * The MIT License
 * Copyright (c) 2014-2015 Techcable
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.techcable.techutils.uuid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.collect.Lists;

import static net.techcable.techutils.HttpUtils.getJson;
import static net.techcable.techutils.HttpUtils.postJson;

public class MojangLookup implements Lookup {


    private List<PlayerProfile> postNames(Collection<String> names) { //This one doesn't cache
        JSONArray request = new JSONArray();
        for (String name : names) {
            request.add(name);
        }
        Object rawResponse = postJson("https://api.mojang.com/profiles/minecraft", request);
        if (!(rawResponse instanceof JSONArray)) return null;
        JSONArray response = (JSONArray) rawResponse;
        List<PlayerProfile> profiles = new ArrayList<>();
        for (Object rawEntry : response) {
            if (!(rawEntry instanceof JSONObject)) return null;
            JSONObject entry = (JSONObject) rawEntry;
            PlayerProfile profile = deserializeProfile(entry);
            if (profile != null) profiles.add(profile);
        }
        return profiles;
    }

    private PlayerProfile lookupProperties(UUID id) {
        Object rawResponse = getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + id.toString().replace("-", ""));
        if (rawResponse == null || !(rawResponse instanceof JSONObject)) return null;
        JSONObject response = (JSONObject) rawResponse;
        PlayerProfile profile = deserializeProfile(response);
        if (profile == null) return null;
        return profile;
    }

    //Json Serialization

    private PlayerProfile deserializeProfile(JSONObject json) {
        if (!json.containsKey("name") || !json.containsKey("id")) return null;
        if (!(json.get("name") instanceof String) || !(json.get("id") instanceof String)) return null;
        String name = (String) json.get("name");
        UUID id = toUUID((String) json.get("id"));
        if (id == null) return null;
        PlayerProfile profile = new PlayerProfile(id, name);
        if (json.containsKey("properties") && json.get("properties") instanceof JSONArray) {
            profile.setProperties((JSONArray) json.get("properties"));
        }
        return profile;
    }

    //Utilities

    private static String toString(UUID id) {
        return id.toString().replace("-", "");
    }

    private static UUID toUUID(String s) {
        if (s.length() == 32) {
            s = s.substring(0, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16) + "-" + s.substring(16, 20) + "-" + s.substring(20, 32);
        }
        return UUID.fromString(s);
    }

    @Override
    public PlayerProfile lookup(String name) {
        Iterator<PlayerProfile> iterator = postNames(Lists.newArrayList(name)).iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    @Override
    public Collection<PlayerProfile> lookup(Collection<String> names) {
        return postNames(names);
    }

    @Override
    public PlayerProfile lookup(UUID id) {
        return lookupProperties(id);
    }

    @Override
    public void lookupProperties(PlayerProfile profile) {
        if (profile.getProperties() != null) return;
    }
}