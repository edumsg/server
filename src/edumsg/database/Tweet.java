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

package edumsg.database;

import java.sql.Timestamp;

@SuppressWarnings("unused")
public class Tweet {
	private Integer id;
	private String tweet_text;
	private String image_url;
	private String created_at;
	private User creator;
	private User retweeter;
	private User favoriter;
	private Integer retweets;
	private Integer favorites;

	public void setId(Integer id) {
		this.id = id;
	}

	public void setTweetText(String tweet_text) {
		this.tweet_text = tweet_text;
	}

	public void setImageUrl(String image_url) {
		this.image_url = image_url;
	}

	public void setCreatedAt(Timestamp timestamp) {
		this.created_at = timestamp.toString();
	}

	public void setCreator(User creator) {
		this.creator = creator;
	}

	public void setRetweeter(User retweeter) {
		this.retweeter = retweeter;
	}

	public void setFavoriter(User favoriter) {
		this.favoriter = favoriter;
	}

	public void setRetweets(Integer retweets) {
		this.retweets = retweets;
	}

	public void setFavorites(Integer favorites) {
		this.favorites = favorites;
	}
}
