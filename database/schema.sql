DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users(
  id serial PRIMARY KEY NOT NULL,
  username varchar(30) UNIQUE NOT NULL, 
  email varchar(100) UNIQUE NOT NULL,
  encrypted_password varchar(150) NOT NULL,
  name varchar(100) NOT NULL,
  language varchar(50),
  country varchar(50),
  bio varchar(160) CHECK (char_length(bio) <= 160),
  website varchar(60),
  created_at timestamp NOT NULL,
  avatar_url varchar(70),
  overlay boolean DEFAULT '0', -- white 0 and black 1
  link_color varchar(10),
  background_color varchar(10),
  protected_tweets boolean DEFAULT '0', -- public 0 and private 1
  session_id text
);

DROP TABLE IF EXISTS tweets CASCADE;

CREATE TABLE tweets(
  id serial PRIMARY KEY NOT NULL,
  tweet_text varchar(140) NOT NULL CHECK (char_length(tweet_text) <= 140),
  creator_id integer NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at timestamp NOT NULL,
  image_url varchar(100)
);

DROP TABLE IF EXISTS reports CASCADE;

CREATE TABLE reports(
  id serial PRIMARY KEY NOT NULL,
  type varchar(10) DEFAULT 'tweets',
  reported_id integer NOT NULL,
  creator_id integer NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at timestamp NOT NULL,
  UNIQUE (reported_id, creator_id)
);

DROP TABLE IF EXISTS favorites CASCADE;

CREATE TABLE favorites(
  id serial PRIMARY KEY NOT NULL,
  tweet_id integer REFERENCES tweets(id) ON DELETE CASCADE,
  user_id integer REFERENCES users(id) ON DELETE CASCADE,
  created_at timestamp NOT NULL,
  UNIQUE (tweet_id, user_id)
);

DROP TABLE IF EXISTS retweets CASCADE;

CREATE TABLE retweets(
  id serial PRIMARY KEY NOT NULL,
  tweet_id integer REFERENCES tweets(id) ON DELETE CASCADE,
  user_id integer REFERENCES users(id) ON DELETE CASCADE,
  created_at timestamp NOT NULL,
  UNIQUE (tweet_id, user_id)
);

DROP TABLE IF EXISTS followships CASCADE;

CREATE TABLE followships(
  id serial PRIMARY KEY NOT NULL,
  user_id integer REFERENCES users(id) ON DELETE CASCADE,
  follower_id integer REFERENCES users(id) ON DELETE CASCADE,
  confirmed boolean DEFAULT '1', -- pending 0 and confirmed 1
  created_at timestamp NOT NULL,
  UNIQUE (user_id, follower_id)
);

DROP TABLE IF EXISTS lists CASCADE;

CREATE TABLE lists(
  id serial PRIMARY KEY NOT NULL,
  name varchar(50) UNIQUE NOT NULL,
  description varchar(160),
  creator_id integer REFERENCES users(id) ON DELETE CASCADE,
  private boolean DEFAULT '0', -- public 0 and private 1
  created_at timestamp NOT NULL
);

DROP TABLE IF EXISTS subscriptions CASCADE;

CREATE TABLE subscriptions(
  id serial PRIMARY KEY NOT NULL,
  subscriber_id integer REFERENCES users(id) ON DELETE CASCADE,
  list_id integer REFERENCES lists(id) ON DELETE CASCADE,
  created_at timestamp NOT NULL,
  UNIQUE(subscriber_id, list_id)
);

DROP TABLE IF EXISTS memberships CASCADE;

CREATE TABLE memberships(
  id serial PRIMARY KEY NOT NULL,
  member_id integer REFERENCES users(id) ON DELETE CASCADE,
  list_id integer REFERENCES lists(id) ON DELETE CASCADE,
  created_at timestamp NOT NULL,
  UNIQUE(member_id, list_id)
);

DROP TABLE IF EXISTS direct_messages CASCADE;

CREATE TABLE direct_messages(
  id serial PRIMARY KEY NOT NULL,
  sender_id integer REFERENCES users(id),
  reciever_id integer REFERENCES users(id),
  dm_text varchar(140) NOT NULL CHECK (char_length(dm_text) <= 140),
  image_url varchar(100),
  read boolean DEFAULT '0', -- read 1 and unread 0
  conv_id integer REFERENCES conversations(id) ON DELETE CASCADE,
  created_at timestamp NOT NULL
);

DROP TABLE IF EXISTS conversations CASCADE;

CREATE TABLE conversations(
  id serial PRIMARY KEY NOT NULL,
  user_id integer REFERENCES users(id),
  user2_id integer REFERENCES users(id)
);

DROP TABLE IF EXISTS replies CASCADE;

CREATE TABLE replies(
  id serial PRIMARY KEY NOT NULL,
  original_tweet_id integer REFERENCES tweets(id) ON DELETE CASCADE,
  reply_id integer REFERENCES tweets(id) ON DELETE CASCADE,
  created_at timestamp NOT NULL
);
