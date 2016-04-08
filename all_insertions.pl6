my $current_path;
my $path;
my @postgres_conf;
if (($*KERNEL.name) === "win32") {
    $current_path = qx/cd/;
    $current_path ~~ (m:g/\w+\:[\\\w+]+\\server/);
    $path = $/;
    @postgres_conf = ($path~"\\Postgres.conf").IO.lines;
} else {
    $current_path = qx/pwd/;
    $current_path ~~ (m:g/[\/\w+]+\/server/);
    $path = $/;
    @postgres_conf = ($path~"/Postgres.conf").IO.lines;
}

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

my @sql_files = ["schema.sql", "user_procs.sql", "tweet_procs.sql", "dm_procs.sql", "lists_procs.sql", "user_insertions.sql", "dm_insertions.sql", "tweets_insertions.sql", "replies_insertions.sql", "retweets_insertions.sql", "mentions_insertions.sql", "list_insertions.sql"];

if (($*KERNEL.name) === "win32") {
    shell "psql -c \"drop schema public cascade\" $database $username ";
    shell "psql -c \"create schema public\" $database $username ";
    for @sql_files -> $file {
        shell "psql -f {$path}/database/$file $database $username ";
    }
} else {
    for @sql_files -> $file {
        shell "psql $database $username -f {$path}/database/$file";
    }

}
