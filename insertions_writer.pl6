use lib '.';
use insertion_generator;
my @files = ["dm_insertions.sql","mentions_insertions.sql","replies_insertions.sql","retweets_insertions.sql","tweets_insertions.sql","user_insertions.sql", "list_insertions.sql"];

my $file;
sub write_all {
    $file = open "database\@files[0]", :w;
    followships_insertions.hyper.map($file.say($_));
    dm_insertions.hyper.map({$file.say($_)});
    $file.close;

    $file = open "database\@files[1]", :w;
    mentions_insertions.hyper.map({$file.say($_)});
    $file.close;

    $file = open "database\@files[2]", :w;
    replies_insertions.hyper.map({$file.say($_)});
    $file.close;

    $file = open "database\@files[3]", :w;
    retweets_insertions.hyper.map({$file.say($_)});
    $file.close;

    $file = open "database\@files[4]", :w;
    tweets_insertions.hyper.map({$file.say($_)});
    $file.close;

    $file = open "database\@files[5]", :w;
    user_insertions.hyper.map({$file.say($_)});
    $file.close;

    $file = open "database\@files[6]", :w;
    lists_insertions.hyper.map({$file.say($_)});
    $file.close;
}
