This project uses Intellij Idea IDE settings and configurations

Dependencies:
1) Redis
2) ActiveMQ
3) PostgreSQL

To run the project
At first, make sure that you have the required dependicies installed on your device 
1 - Apache activeMQ :
download the open-sorce package and run it using the terminal by navigating to the bin folder in the package and execute command "activemq start"
2 - redis server : download redis package and run redis-server executable file inside it.
3 - edit/create postgres.conf file with the next format and make sure you have cteated the database with the required insertion using postgresSQL
postgres data
user = [database user-name]
database = [database name]
pass = [password]
host = [database host]
port = [postgresSQL port]
4 - compile the following classes in order:
 - in "shared" package:
1)UserMain
2)TweetMain
3)ListMain
4)DMMain
 - in "netty" package: 
5)EduMsgNettyServer
 - in "controller" package:
 6)EduMsgController
  - in "loadBalancer" package
 7)loadBalancerServer
5 - use an editor like "webstorm" to run the front-end package to open access the platform through the browser.