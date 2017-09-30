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

package com.ciscospark.androidsdk.message;


import java.util.Date;

import com.ciscospark.androidsdk.room.Room;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * This class represents a Message on Cisco Spark.
 * 
 * @since 0.1
 */
public class Message {

    @SerializedName("id")
    private String _id;

    @SerializedName("personId")
    private String _personId;

    @SerializedName("personEmail")
    private String _personEmail;

    @SerializedName("roomId")
    private String _roomId;

    @SerializedName("roomType")
    private Room.RoomType _roomType;

    @SerializedName("text")
    private String _text;

    @SerializedName("markdown")
    private String _markdown;

    @SerializedName("toPersonId")
    private String _toPersonId;

    @SerializedName("toPersonEmail")
    private String _toPersonEmail;

    @SerializedName("mentionedPeople")
    private String[] _mentionedPeople;

    @SerializedName("files")
    private String[] _files;

    @SerializedName("created")
    private Date _created;

	/**
	 * @return The identifier of this message.
	 * @since 0.1
	 */
	public String getId() {
        return _id;
    }

	/**
	 * @return The identifier of the person who sent this message.
	 * @since 0.1
	 */
	public String getPersonId() {
        return _personId;
    }

	/**
	 * @return The email address of the person who sent this message.
	 * @since 0.1
	 */
	public String getPersonEmail() {
        return _personEmail;
    }

	/**
	 * @return The identifier of the room where this message was posted.
	 * @since 0.1
	 */
	public String getRoomId() {
        return _roomId;
    }

	/**
	 * @return The type of the room where this message was posted.
	 * @since 0.1
	 */
	public Room.RoomType getRoomType() {
        return _roomType;
    }

	/**
	 * @return The content of the message in plain text.
	 * @since 0.1
	 */
	public String getText() {
        return _text;
    }

	/**
	 * @return  The content of the message in markdown.
	 * @since 0.1
	 */
	public String getMarkdown() {
        return _markdown;
    }

	/**
	 * @return The identifier of the recipient when sending a private 1:1 message.
	 * @since 0.1
	 */
	public String getToPersonId() {
        return _toPersonId;
    }

	/**
	 * @return The email address of the recipient when sending a private 1:1 message.
	 * @since 0.1
	 */
	public String getToPersonEmail() {
        return _toPersonEmail;
    }

	/**
	 * @return The array of mentioned peoples in the message.
	 * @since 0.1
	 */
	public String[] getMentionedPeople() {
        return _mentionedPeople;
    }

	/**
	 * @return A array of public URLs of the attachments in the message.
	 * @since 0.1
	 */
	public String[] getFiles() {
        return _files;
    }

	/**
	 * @return The timestamp that the message being created.
	 * @since 0.1
	 */
	public Date getCreated() {
        return _created;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
