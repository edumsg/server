our @names = ["Sara","Magda", "Sameer", "Sameh", "Samar", "Hadeel", "Samer", "Medhat","Salma","Sondos","Omar","Mohamed","Amgad","Menna","Lamees","Farah","Ismail","Magdy","Amal","Laila"];

our %ids = (Sara => 1, Magda => 2, Sameer => 3, Sameh => 4, Samar => 5, Hadeel => 6, Samer => 7, Medhat => 8, Salma => 9, Sondos => 10, Omar => 11, Mohamed => 12, Amgad => 13, Menna => 14, Lamees => 15, Farah => 16, Ismail => 17, Magdy => 18, Amal => 19, Laila => 20);

sub user_insertions{
    for 0..19 -> $i {
        say "INSERT INTO users VALUES(DEFAULT,'"  ~@names[$i].gist.lc~ "', 'a$i@a.com', '\$2a\$10\$LWTMQA4F1.jqctvsJtFapu7X.wTo8lXhLybg5.7haAF2Vt7k7DKIG','" ~@names[$i].gist~ "', 'english', 'egypt',  'fsdfdsjlfdlkjdfk', 'www.google.com', CURRENT_TIMESTAMP, 'http://bit.ly/20VGjpB', true, '#FF0000', '#00FF00', false, NULL );\n";
    }
}

sub tweets_insertions{
    my @temp;
    for %ids.kv -> $name , $id {
        for 1..20 {
            @temp.push("INSERT INTO tweets VALUES(DEFAULT, 'HELLO', $id, CURRENT_TIMESTAMP, NULL);");
        }
    }
    @temp.sort>>.say
}

sub followships_insertions{
    my @temp;
    for 1..20 -> $i {
        for 1..20 -> $j {
            next if ($i == $j);
            @temp.push("INSERT INTO followships VALUES(DEFAULT, $i, $j, true, CURRENT_TIMESTAMP);");
            @temp.push("INSERT INTO followships VALUES(DEFAULT, $j, $i, true, CURRENT_TIMESTAMP);");
        }
    }
    @temp.unique>>.say;
}

sub dm_insertions{
    my @temp;
    for 1..11 -> $i {
        for 1..11 -> $j {
            next if ($i == $j);
            @temp.push("INSERT INTO conversations VALUES(DEFAULT, $i, $j);");

            @temp.push("INSERT INTO direct_messages VALUES(DEFAULT, $i, $j, 'Hello', NULL, true, CURRVAL(pg_get_serial_sequence('conversations','id')), CURRENT_TIMESTAMP);");

            @temp.push("INSERT INTO direct_messages VALUES(DEFAULT, $j, $i, 'Hello', NULL, true, CURRVAL(pg_get_serial_sequence('conversations','id')), CURRENT_TIMESTAMP);");

        }
    }
    @temp>>.say;
}

sub replies_insertions{
    for 0..19 -> $i {
        say "INSERT INTO replies VALUES(DEFAULT, (SELECT id FROM tweets LIMIT 1 OFFSET " ~$i~ "), (SELECT id FROM tweets LIMIT 1 OFFSET " ~$i+1~ "), CURRENT_TIMESTAMP);";
    }
}
