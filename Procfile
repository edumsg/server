web: java $JAVA_OPTS -cp target/classes:target/dependency/* edumsg.netty.EduMsgNettyServer
worker: java $JAVA_OPTS -cp target/classes:target/dependency/* edumsg.shared.UserMain
tweetWorker: java $JAVA_OPTS -cp target/classes:target/dependency/* edumsg.shared.TweetMain
listWorker: java $JAVA_OPTS -cp target/classes:target/dependency/* edumsg.shared.ListMain
dmWorker: java $JAVA_OPTS -cp target/classes:target/dependency/* edumsg.shared.DMMain
activeMQWorker: java apache-activemq-5.13.0/bin/activemq