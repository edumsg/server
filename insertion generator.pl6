sub user_insertions{
    my @names = ["Sara","Magda", "Sameer", "Sameh", "Samar", "Hadeel", "Samer", "Medhat","Salma","Sondos"];
    for 0...10 -> $i {
        say "INSERT INTO users values(NULL,"  ~@names[$i].gist.lc~ ", a$i@a.com, \$2a\$10\$LWTMQA4F1.jqctvsJtFapu7X.wTo8lXhLybg5.7haAF2Vt7k7DKIG," ~@names[$i].gist~ ", english, egypt,  fsdfdsjlfdlkjdfk, www.google.com, CURRENT_TIMESTAMP, http://bit.ly/20VGjpB, 0, #FF0000, #00FF00, 0, NULL )\n"
    }
}

user_insertions;
