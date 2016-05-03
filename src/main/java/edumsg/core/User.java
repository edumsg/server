/*
EduMsg is made available under the OSI-approved MIT license.
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
IN THE SOFTWARE.
*/

package edumsg.core;

import java.sql.Timestamp;

@SuppressWarnings("unused")
public class User {
    private Integer id;
    private String username;
    private String email;
    private String encrypted_password;
    private String name;
    private String language;
    private String country;
    private String bio;
    private String website;
    private String created_at;
    private String avatar_url;
    private Boolean overlay;
    private String link_color;
    private String background_color;
    private Boolean protected_tweets;
    private String session_id;

    private String gender;

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setDate_of_birth(String date_of_birth) {
        this.date_of_birth = date_of_birth;
    }

    private String date_of_birth;

    public void setId(Integer id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setEncryptedPassword(String encrypted_password) {
        this.encrypted_password = encrypted_password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public void setCreatedAt(Timestamp created_at) {
        this.created_at = created_at.toString();
    }

    public void setAvatarUrl(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    public void setOverlay(Boolean overlay) {
        this.overlay = overlay;
    }

    public void setLinkColor(String link_color) {
        this.link_color = link_color;
    }

    public void setBackgroundColor(String background_color) {
        this.background_color = background_color;
    }

    public void setProtectedTweets(Boolean protected_tweets) {
        this.protected_tweets = protected_tweets;
    }

    public void setSessionID(String sessionID) {
        this.session_id = sessionID;
    }
}
