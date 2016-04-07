use Data::Dump;

my $filename = "temp.txt";
our @names = ["Sara","Magda", "Sameer", "Sameh", "Samar", "Hadeel", "Samer", "Medhat","Salma","Sondos","Omar","Mohamed","Amgad","Menna","Lamees","Farah","Ismail","Magdy","Amal","Laila"];

my $fh = open $filename, :w;
$fh.say("jk");
$fh.say("jk");
$fh.say(@names>>.gist);
@names.hyper.map({$fh.say($_);});
# $fh.print("second line\n");
$fh.close;

say $*DISTRO;
say $*KERNEL;
say $*VM;
