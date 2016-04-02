my $current_path = qx/pwd/;
$current_path ~~ (m:g/[\/\w+]+\/server/);
my $path = $/;
my @postgres_conf = ($path~"/Postgres.conf").IO.lines;

my $username;
my $database;

for @postgres_conf -> $line {
    if $line.contains("username") {
        $line ~~ (/\[(.+)\]/);
        $username = $0;
    }

    if $line.contains("database") {
        $line ~~ (/\[(.+)\]/);
        $database = $0;
    }
}

my @sql_files = ["schema.sql", "user_procs.sql", "tweet_procs.sql", "dm_procs.sql", "lists_procs.sql", "user_insertions.sql", "dm_insertions.sql", "tweets_insertions.sql", "replies_insertions.sql", "retweets_insertions.sql", "mentions_insertions.sql"];

for @sql_files -> $file {
    shell "psql $database $username -f {$path}/database/$file";
}
