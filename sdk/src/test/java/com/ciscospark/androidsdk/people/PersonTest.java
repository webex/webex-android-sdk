/*
 * Copyright 2016-2017 Cisco Systems Inc
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

package com.ciscospark.androidsdk.people;

import com.ciscospark.androidsdk.utils.Utils;
import com.google.gson.annotations.SerializedName;

import org.junit.Test;

/**
 * 200 / success
 * {
 * "items": [
 * {
 * "id": "Y2lzY29zcGFyazovL3VzL1BFT1BMRS9jZjlmMGNlZC0xZmJiLTQzOWUtODEyMi01NmZiNjAwNDE2Y2Q",
 * "emails": [
 * "xionxiao@cisco.com"
 * ],
 * "displayName": "Allen Xiao",
 * "nickName": "Allen",
 * "firstName": "Allen",
 * "lastName": "Xiao",
 * "avatar": "https://1efa7a94ed216783e352-c62266528714497a17239ececf39e9e2.ssl.cf1.rackcdn.com/V1~f007634e3cba657ae074fcef8fc62ba7~IM13HqRHQzua0FIk5Jq92w==~1600",
 * "orgId": "Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi8xZWI2NWZkZi05NjQzLTQxN2YtOTk3NC1hZDcyY2FlMGUxMGY",
 * "created": "2017-04-14T11:13:13.467Z",
 * "lastActivity": "2017-08-17T00:42:15.084Z",
 * "status": "active",
 * "type": "person"
 * }
 * ],
 * "notFoundIds": null
 * }
 * <p>
 * <p>
 * {
 * "items": [
 * {
 * "id": "Y2lzY29zcGFyazovL3VzL1BFT1BMRS9jNjJkNTBiMi0yYzFkLTRhNWMtOTIzNy0wNjJkZDk3NDc2MmM",
 * "emails": [
 * "shepx@163.com"
 * ],
 * "displayName": "Eric",
 * "nickName": "Eric",
 * "avatar": "https://c74213ddaf67eb02dabb-04de5163e3f90393a9f7bb6f7f0967f1.ssl.cf1.rackcdn.com/V1~691d27c5e879d9b4126edbc0ed0d1683~_V1ZjGddToOslLXzNQPueg==~1600",
 * "orgId": "Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi9jb25zdW1lcg",
 * "created": "2017-04-15T23:56:15.177Z",
 * "status": "unknown",
 * "type": "person"
 * }
 * ],
 * "notFoundIds": null
 * }
 * <p>
 * <p>
 * {
 * "items": [],
 * "notFoundIds": null
 * }
 * <p>
 * <p>
 * {
 * "items": [
 * {
 * "id": "Y2lzY29zcGFyazovL3VzL1BFT1BMRS83NjkxNjA0NS03MDc3LTQ1ODgtYTUxMi1mZDYyODkxNWZmYzc",
 * "emails": [
 * "lmtest3@cd5c9af7-8ed3-4e15-9705-025ef30b1b6a"
 * ],
 * "displayName": "lmtest3",
 * "avatar": "https://00792fd90955bc2b54b7-dde9bcd8a491bb35da928cc2123a400b.ssl.cf1.rackcdn.com/default_machine~80",
 * "orgId": "Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi9jZDVjOWFmNy04ZWQzLTRlMTUtOTcwNS0wMjVlZjMwYjFiNmE",
 * "created": "2017-05-11T08:32:06.017Z",
 * "type": "bot"
 * }
 * ],
 * "notFoundIds": null
 * }
 */
public class PersonTest {

    @Test
    public void testConvert() {
        System.out.println(Utils.timestampUTC());

//        String jsonString = "{\n" +
//                " \"id\": \"Y2lzY29zcGFyazovL3VzL1BFT1BMRS9jNjJkNTBiMi0yYzFkLTRhNWMtOTIzNy0wNjJkZDk3NDc2MmM\",\n" +
//                " \"emails\": [\n" +
//                " \"shepx@163.com\"\n" +
//                " ],\n" +
//                " \"displayName\": \"Eric\",\n" +
//                " \"nickName\": \"Eric\",\n" +
//                " \"avatar\": \"https://c74213ddaf67eb02dabb-04de5163e3f90393a9f7bb6f7f0967f1.ssl.cf1.rackcdn.com/V1~691d27c5e879d9b4126edbc0ed0d1683~_V1ZjGddToOslLXzNQPueg==~1600\",\n" +
//                " \"orgId\": \"Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi9jb25zdW1lcg\",\n" +
//                " \"created\": \"2017-04-15T23:56:15.177Z\",\n" +
//                " \"status\": \"unknown\",\n" +
//                " \"type\": \"person\"\n" +
//                " }";
//        Gson gson = new Gson();
//        Person person = gson.fromJson(jsonString, Person.class);
//        assertNotNull(person);
//        assertEquals(person.getId(), "Y2lzY29zcGFyazovL3VzL1BFT1BMRS9jNjJkNTBiMi0yYzFkLTRhNWMtOTIzNy0wNjJkZDk3NDc2MmM");
//        assertEquals(person.getEmails()[0], "shepx@163.com");
//        assertEquals(person.getDisplayName(), "Eric");
//        assertEquals(person.getNickName(), "Eric");
//        assertNull(person.getFirstName());
//        assertNull(person.getLastName());
//        assertEquals(person.getAvatar(), "https://c74213ddaf67eb02dabb-04de5163e3f90393a9f7bb6f7f0967f1.ssl.cf1.rackcdn.com/V1~691d27c5e879d9b4126edbc0ed0d1683~_V1ZjGddToOslLXzNQPueg==~1600");
//        assertEquals(person.getOrgId(), "Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi9jb25zdW1lcg");
//        assertEquals(person.getCreated(), "2017-04-15T23:56:15.177Z");
//        assertEquals(person.getStatus(), "unknown");
//        assertEquals(person.getType(), "person");
    }


    class NestedResponse {
        public Person[] getItems() {
            return items;
        }

        @SerializedName("items")
        private Person[] items;
    }

    @Test
    public void testNestedConvert() {
//        String jsonString = "{\n" +
//                " \"items\": [\n" +
//                " {\n" +
//                " \"id\": \"Y2lzY29zcGFyazovL3VzL1BFT1BMRS9jNjJkNTBiMi0yYzFkLTRhNWMtOTIzNy0wNjJkZDk3NDc2MmM\",\n" +
//                " \"emails\": [\n" +
//                " \"shepx@163.com\"\n" +
//                " ],\n" +
//                " \"displayName\": \"Eric\",\n" +
//                " \"nickName\": \"Eric\",\n" +
//                " \"avatar\": \"https://c74213ddaf67eb02dabb-04de5163e3f90393a9f7bb6f7f0967f1.ssl.cf1.rackcdn.com/V1~691d27c5e879d9b4126edbc0ed0d1683~_V1ZjGddToOslLXzNQPueg==~1600\",\n" +
//                " \"orgId\": \"Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi9jb25zdW1lcg\",\n" +
//                " \"created\": \"2017-04-15T23:56:15.177Z\",\n" +
//                " \"status\": \"unknown\",\n" +
//                " \"type\": \"person\"\n" +
//                " }\n" +
//                " ],\n" +
//                " \"notFoundIds\": null\n" +
//                " }";
//
//
//        Gson gson = new Gson();
//        NestedResponse nr = gson.fromJson(jsonString, NestedResponse.class);
//        assertNotNull(nr);
//        assertEquals(nr.getItems().length, 1);
//        Person person = nr.getItems()[0];
//        assertEquals(person.getId(), "Y2lzY29zcGFyazovL3VzL1BFT1BMRS9jNjJkNTBiMi0yYzFkLTRhNWMtOTIzNy0wNjJkZDk3NDc2MmM");
//        assertEquals(person.getEmails()[0], "shepx@163.com");
//        assertEquals(person.getDisplayName(), "Eric");
//        assertEquals(person.getNickName(), "Eric");
//        assertNull(person.getFirstName());
//        assertNull(person.getLastName());
//        assertEquals(person.getAvatar(), "https://c74213ddaf67eb02dabb-04de5163e3f90393a9f7bb6f7f0967f1.ssl.cf1.rackcdn.com/V1~691d27c5e879d9b4126edbc0ed0d1683~_V1ZjGddToOslLXzNQPueg==~1600");
//        assertEquals(person.getOrgId(), "Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi9jb25zdW1lcg");
//        assertEquals(person.getCreated(), "2017-04-15T23:56:15.177Z");
//        assertEquals(person.getStatus(), "unknown");
//        assertEquals(person.getType(), "person");
//
//        jsonString = "{\n" +
//                " \"items\": [],\n" +
//                " \"notFoundIds\": null\n" +
//                " }";
//        nr = gson.fromJson(jsonString, NestedResponse.class);
//        assertNotNull(nr);
//        assertNotNull(nr.getItems());
//        assertEquals(nr.getItems().length, 0);

    }
}