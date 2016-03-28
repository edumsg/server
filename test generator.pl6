our @user_methods = ["confirm_follow", "follow", "followers", "following", "get_favorites", "get_feeds", "get_list_memberships", "get_mentions", "get_retweets" , "get_subscribed_lists", "timeline", "get_user", "get_users", "login", "logout", "register", "report_user", "unconfirmed_followers", "unfollow", "update_user"];

our @tweet_methods = ["delete_tweet", "favorite", "get_tweet", "tweet", "reply", "report_tweet", "retweet", "unfavorite", "unretweet"];

our @list_methods = ["add_member", "create_list", "delete_list","delete_member","get_list_feeds","list_members","list_subscribers", "subscribe", "unsubscribe", "update_list"];

our @dm_methods = ["create_dm", "delete_conv", "delete_dm", "get_conv", "get_convs", "mark_all_conv_read", "mark_conv_read"];

sub user_tests{
    for @user_methods -> $i {
        say "def $i \nreq = Net::HTTP::Post.new(@uri, initheader = \{'Content-Type' => 'application/json'\})\nreq.body = \{'queue': 'USER', 'method': '$i', 'user_id': '2', '_id': ''\}.to_json\nres = Net::HTTP.start(@uri.hostname, @uri.port) do |http|\nhttp.request(req)\nend\nreturn res.body\nend\n";
    }
}


sub tweet_tests{
    for @tweet_methods -> $i {
        say "def $i \nreq = Net::HTTP::Post.new(@uri, initheader = \{'Content-Type' => 'application/json'\})\nreq.body = \{'queue': 'TWEET', 'method': '$i', 'tweet_id': '', '_id': ''\}.to_json\nres = Net::HTTP.start(@uri.hostname, @uri.port) do |http|\nhttp.request(req)\nend\nreturn res.body\nend\n";
    }
}


sub list_tests{
    for @list_methods -> $i {
        say "def $i \nreq = Net::HTTP::Post.new(@uri, initheader = \{'Content-Type' => 'application/json'\})\nreq.body = \{'queue': 'LIST', 'method': '$i', 'list_id': '2', '_id': ''\}.to_json\nres = Net::HTTP.start(@uri.hostname, @uri.port) do |http|\nhttp.request(req)\nend\nreturn res.body\nend\n";
    }
}


sub dm_tests{
    for @dm_methods -> $i {
        say "def $i \nreq = Net::HTTP::Post.new(@uri, initheader = \{'Content-Type' => 'application/json'\})\nreq.body = \{'queue': 'DM', 'method': '$i', 'dm_id': '2', '_id': ''\}.to_json\nres = Net::HTTP.start(@uri.hostname, @uri.port) do |http|\nhttp.request(req)\nend\nreturn res.body\nend\n";
    }
}

user_tests;
tweet_tests;
list_tests;
dm_tests;
