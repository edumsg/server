our @names = ["Sara","Magda", "Sameer", "Sameh", "Samar", "Hadeel", "Samer", "Medhat","Salma","Sondos"];

our %ids = (Sara => 1, Magda => 2, Sameer => 3, Sameh => 4, Samar => 5, Hadeel => 6, Samer => 7, Medhat => 8, Salma => 9, Sondos => 10);

sub user_insertions{
    for 0..9 -> $i {
        say "INSERT INTO users VALUES(DEFAULT,'"  ~@names[$i].gist.lc~ "', 'a$i@a.com', '\$2a\$10\$LWTMQA4F1.jqctvsJtFapu7X.wTo8lXhLybg5.7haAF2Vt7k7DKIG','" ~@names[$i].gist~ "', 'english', 'egypt',  'fsdfdsjlfdlkjdfk', 'www.google.com', CURRENT_TIMESTAMP, 'http://bit.ly/20VGjpB', true, '#FF0000', '#00FF00', false, NULL );\n"
    }
}

sub tweets_insertions{
    for %ids.kv -> $name , $id {
        for 0..5 {
            say "INSERT INTO tweets VALUES(DEFAULT, 'HELLO', $id, CURRENT_TIMESTAMP, NULL);\n";
        }
    }
}

sub dm_insertions{
    
}

tweets_insertions;
