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

import edumsg.core.commands.dm.*;
import edumsg.core.commands.list.*;
import edumsg.core.commands.tweet.*;
import edumsg.core.commands.user.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class CommandsMap {
    private static ConcurrentMap<String, Class<?>> cmdMap;
//Each new function you create, you must add it below
    public static void instantiate() {
        cmdMap = new ConcurrentHashMap<>();
        cmdMap.put("register", RegisterCommand.class);
        cmdMap.put("follow", FollowCommand.class);
        cmdMap.put("is_following", IsFollowingCommand.class);
        cmdMap.put("is_following_user",isFollowingUserCommand.class);
        cmdMap.put("unfollow", UnFollowCommand.class);
        cmdMap.put("confirm_follow", ConfirmFollowCommand.class);
        cmdMap.put("report_user", ReportUserCommand.class);
        cmdMap.put("tweet", NewTweetCommand.class);
        cmdMap.put("reply", ReplyCommand.class);
        cmdMap.put("delete_tweet", DeleteTweetCommand.class);
        cmdMap.put("report_tweet", ReportTweetCommand.class);
        cmdMap.put("favorite", FavoriteCommand.class);
        cmdMap.put("unfavorite", UnFavoriteCommand.class);
        cmdMap.put("retweet", RetweetCommand.class);
        cmdMap.put("unretweet", UnRetweetCommand.class);
        cmdMap.put("get_users", GetUsersCommand.class);
        cmdMap.put("followers", FollowersCommand.class);
        cmdMap.put("following", FollowingCommand.class);
        cmdMap.put("unconfirmed_followers", UnconfirmedFollowersCommand.class);
        cmdMap.put("update_user", UpdateUserCommand.class);
        cmdMap.put("get_user", GetUserCommand.class);
        cmdMap.put("get_user2", GetUser2Command.class);
        cmdMap.put("my_profile", MyProfileCommand.class);
        cmdMap.put("get_mentions", GetMentionsCommand.class);
        cmdMap.put("get_retweets", GetRetweetsCommand.class);
        cmdMap.put("get_tweet", GetTweetCommand.class);
        cmdMap.put("user_tweets", GetUserTweetsCommand.class);
        cmdMap.put("user_tweets2", GetUserTweets2Command.class);
        cmdMap.put("get_favorites", GetFavoritesCommand.class);
        cmdMap.put("timeline", GetTimelineCommand.class);
        cmdMap.put("get_subscribed_lists", GetSubscribedListsCommand.class);
        cmdMap.put("get_list_memberships", GetListMembershipsCommand.class);
        cmdMap.put("login", LoginCommand.class);
        cmdMap.put("logout", LogoutCommand.class);
        cmdMap.put("get_earliest_replies", GetEarliestRepliesCommand.class);
        cmdMap.put("get_replies", GetRepliesCommand.class);
        cmdMap.put("user_with_tweets", GetUserWithTweetsCommand.class);



        cmdMap.put("create_dm", CreateDmCommand.class);
        cmdMap.put("create_dm2", CreateDm2Command.class);
        cmdMap.put("delete_dm", DeleteDmCommand.class);
        cmdMap.put("create_conversation", CreateConversationCommand.class);
        cmdMap.put("get_conv", GetConversationCommand.class);
        cmdMap.put("get_convs", GetConversationsCommand.class);
        cmdMap.put("delete_conv", DeleteConversationCommand.class);
        cmdMap.put("mark_conv_read", MarkReadCommand.class);
        cmdMap.put("mark_all_conv_read", MarkAllReadCommand.class);

        cmdMap.put("add_member", AddMemberCommand.class);
        cmdMap.put("create_list", CreateListCommand.class);
        cmdMap.put("create_list_with_members", CreateListMembersCommand.class);
        cmdMap.put("delete_list", DeleteListCommand.class);
        cmdMap.put("delete_member", DeleteMemberCommand.class);
        cmdMap.put("list_members", GetListMembersCommand.class);
        cmdMap.put("list_subscribers", GetListSubscribersCommand.class);
        cmdMap.put("subscribe", SubscribeCommand.class);
        cmdMap.put("unsubscribe", UnSubscribeCommand.class);
        cmdMap.put("update_list", UpdateListCommand.class);
        cmdMap.put("get_list_feeds", GetListFeedsCommand.class);
        cmdMap.put("get_list", GetListCommand.class);
        cmdMap.put("is_owner_of_list", isOwnerOfListCommand.class);
    }

    public static Class<?> queryClass(String cmd) {
        return cmdMap.get(cmd);
    }
}
