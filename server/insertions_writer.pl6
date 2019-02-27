use lib '.';
use insertion_generator;
my @files = ["dm_insertions.sql","mentions_insertions.sql","replies_insertions.sql","retweets_insertions.sql","tweets_insertions.sql","user_insertions.sql", "list_insertions.sql"];

my $file;

sub write_lists {
    $file = open "database/@files[6]", :w;
    lists_insertions.map({$file.say($_)});
    $file.close;
}

sub write_dms {
    $file = open "database/@files[0]", :w;
    followships_insertions.unique.map({$file.say($_)});
    dm_insertions.map({$file.say($_)});
    $file.close;
}

sub write_all {
    $file = open "database/@files[0]", :w;
    followships_insertions.unique.map({$file.say($_)});
    dm_insertions.map({$file.say($_)});
    $file.close;

    $file = open "database/@files[1]", :w;
    mentions_insertions.map({$file.say($_)});
    $file.close;

    $file = open "database/@files[2]", :w;
    replies_insertions.map({$file.say($_)});
    $file.close;

    $file = open "database/@files[3]", :w;
    retweets_insertions.map({$file.say($_)});
    $file.close;

    $file = open "database/@files[4]", :w;
    tweets_insertions.map({$file.say($_)});
    $file.close;

    $file = open "database/@files[5]", :w;
    user_insertions.map({$file.say($_)});
    $file.close;

    $file = open "database/@files[6]", :w;
    lists_insertions.map({$file.say($_)});
    $file.close;
}


write_lists;
