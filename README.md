![Diagram](https://github.com/edumsg/server/assets/68449722/326aef4a-3232-422b-85ff-a30fda0e9256)

This project uses Intellij Idea IDE settings and configurations

1. Redis: Install Docker, pull the [redis](https://hub.docker.com/_/redis) image and run its container. It should be running on port `6379`.
2. ActiveMQ: Install Docker and run this command in powershell
   `docker run -p 61616:61616 -p 8161:8161 rmohr/activemq`
   or pull the image [rmohr/activemq](https://hub.docker.com/r/rmohr/activemq) and run its conatiner using Docker Desktop.
3. PostgreSQL: Install PostgreSQL from [here](https://www.postgresql.org/) and make sure its server is running, then edit/create `postgres.conf` file with the next format and make sure you have cteated the database with the required insertion using postgresSQL

```

username = [database user-name]
database = [database name]
pass = [password]
host = [database host]
port = [postgresSQL port]
```

4. You might need to run the file `all_insertions.pl6` to make the database insertions.
5. Edit `config.conf` file as follows

```
# config attributes
instance_user = [username of the host]
instance_host = [ip address]
instance_pass = [password]
main_host = [ip address of the main host that has the loadbalancer and controller]
```

6. Edit `IPs.properties` file to include the info about the other hosts in the system in the following manner:

```
# config attributes
ip1= 123.123.123.123
user1=host1
password1=password1
ip2= 123.123.123.123
user2=host2
password2=password2
```

7. You should have Maven installed. move to the directory of the project and run the command `mvn package` to create the jar file.
8. After that, open 5 shells and execute the following seven commands, each command in a shell. These commands are required to run the 4 micro-services and the Netty Main server.
   `java -jar target/TwitterBackend-1.0.jar user 1`
   `java -jar target/TwitterBackend-1.0.jar tweet 1`
   `java -jar target/TwitterBackend-1.0.jar dm 1`
   `java -jar target/TwitterBackend-1.0.jar list 1`
   `java -jar target/TwitterBackend-1.0.jar server 1`
   In case you need quick testing, you will find a file called `Test.java` in te `NodeManager` package that will run these instances in seperate threads.
9. In another shell run the command
   `java -jar target/TwitterBackend-1.0.jar admin`
   to start the admin CLI.
10. In `controller` package, run `EduMsgController.java`. This step must be done through intellij.
11. In `loadbalancer` package, run `loadBalancerServer.java`. This step must be done through intellij.
12. Clone the front end [repo](https://github.com/edumsg/webfrontend) and open it in a browser to interact with the website. However, configure the urls and port numbers so that it can communicate with the backend.
