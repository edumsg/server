-- JAVA DONE
CREATE OR REPLACE FUNCTION create_tweet(tweet_text VARCHAR(140), session VARCHAR, image_url VARCHAR(100) DEFAULT NULL)
  RETURNS SETOF tweets AS $$
DECLARE userID INTEGER;
BEGIN
  SELECT user_id
  INTO userID
  FROM sessions
  WHERE id = $2;
  INSERT INTO tweets (tweet_text, creator_id, created_at, image_url)
  VALUES (tweet_text, userID, now()::timestamp, image_url);
  RETURN QUERY
  SELECT *
  FROM tweets
  WHERE id = CURRVAL(pg_get_serial_sequence('tweets', 'id'));
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION delete_tweet(tweet_id INTEGER)
  RETURNS VOID AS $$
BEGIN
  DELETE FROM tweets T
  WHERE T.id = tweet_id;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_tweet(tweet_id INTEGER)
  RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
BEGIN
  OPEN cursor FOR
  SELECT
    T.*,
    U.username,
    U.name,
    U.avatar_url,
    get_retweets_count(tweet_id)  AS "retweets",
    get_favorites_count(tweet_id) AS "favorites"
  FROM tweets T INNER JOIN users U ON T.creator_id = U.id
  WHERE T.id = tweet_id;
  RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- NOT USED
CREATE OR REPLACE FUNCTION get_retweets(tweet_id INTEGER)
  RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
BEGIN
  OPEN cursor FOR
  SELECT
    U.username,
    U.name,
    U.avatar_url
  FROM retweets R INNER JOIN users U ON R.user_id = U.id
  WHERE R.tweet_id = $1
  LIMIT 6;
  RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- NOT USED
CREATE OR REPLACE FUNCTION get_favorites(tweet_id INTEGER)
  RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
BEGIN
  OPEN cursor FOR
  SELECT
    U.username,
    U.name,
    U.avatar_url
  FROM favorites F INNER JOIN users U ON F.user_id = U.id
  WHERE F.tweet_id = $1
  LIMIT 6;
  RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION favorite(tweet_id INTEGER, session VARCHAR)
  RETURNS INTEGER AS $$
DECLARE userID INTEGER;
BEGIN
  SELECT user_id
  INTO userID
  FROM sessions
  WHERE id = $2;
  INSERT INTO favorites (tweet_id, user_id, created_at) VALUES (tweet_id, userID, now()::timestamp);
  RETURN get_favorites_count(tweet_id);
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION unfavorite(tweet_id INTEGER, session VARCHAR)
  RETURNS INTEGER AS $$
DECLARE userID INTEGER;
BEGIN
  SELECT user_id
  INTO userID
  FROM sessions
  WHERE id = $2;
  DELETE FROM favorites F
  WHERE F.tweet_id = $1 AND F.user_id = userID;
  RETURN get_favorites_count(tweet_id);
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION retweet(tweet_id INTEGER, session VARCHAR)
  RETURNS INTEGER AS $$
DECLARE temp   INTEGER;
        userID INTEGER;
BEGIN
  SELECT user_id
  INTO userID
  FROM sessions
  WHERE id = $2;
  SELECT T.creator_id
  INTO temp
  FROM tweets T
  WHERE T.id = $1
  LIMIT 1;
  IF temp != userID
  THEN
    INSERT INTO retweets (tweet_id, creator_id, retweeter_id, created_at)
    VALUES (tweet_id, temp, userID, now()::timestamp);
  END IF;
  RETURN get_retweets_count(tweet_id);
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION unretweet(tweet_id INTEGER, session VARCHAR)
  RETURNS INTEGER AS $$
DECLARE userID INTEGER;
BEGIN
  SELECT user_id
  INTO userID
  FROM sessions
  WHERE id = $2;
  DELETE FROM retweets R
  WHERE R.tweet_id = $1 AND R.retweeter_id = userID;
  RETURN get_retweets_count(tweet_id);
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION get_retweets_count(tweet_id INTEGER)
  RETURNS INTEGER AS $$
DECLARE res INTEGER;
BEGIN
  SELECT count(*)
  INTO res
  FROM retweets R
  WHERE R.tweet_id = $1;
  RETURN res;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION get_favorites_count(tweet_id INTEGER)
  RETURNS INTEGER AS $$
DECLARE res INTEGER;
BEGIN
  SELECT count(*)
  INTO res
  FROM favorites F
  WHERE F.tweet_id = $1;
  RETURN res;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION reply(tweet_id  INTEGER, tweet_text VARCHAR(140), session VARCHAR,
                                 image_url VARCHAR(100) DEFAULT NULL)
  RETURNS VOID AS $$
DECLARE reply_id INTEGER;
        userID   INTEGER;
BEGIN
  SELECT id
  FROM create_tweet(tweet_text, session, image_url)
  INTO reply_id;
  INSERT INTO replies (original_tweet_id, reply_id, created_at) VALUES (tweet_id, reply_id, now()::timestamp);
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION report_tweet(reported_id INTEGER, session VARCHAR)
  RETURNS VOID AS $$
DECLARE userID INTEGER;
BEGIN
  SELECT user_id
  INTO userID
  FROM sessions
  WHERE id = $2;
  INSERT INTO reports (reported_id, creator_id, created_at) VALUES (reported_id, userID, now()::timestamp);
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_earliest_replies(tweet_id INTEGER, session VARCHAR)
  RETURNS REFCURSOR AS $$
DECLARE cursor   REFCURSOR := 'cur';
        favorite INTEGER;
        retweet  INTEGER;
        userID   INTEGER;
BEGIN
  SELECT user_id
  INTO userID
  FROM sessions
  WHERE id = $2;


  SELECT COUNT(*)
  INTO favorite
  FROM favorites F
  WHERE F.tweet_id = $1 AND F.user_id = userID;
  SELECT COUNT(*)
  INTO retweet
  FROM retweets rt
  WHERE rt.tweet_id = $1 AND rt.retweeter_id = userID;

  OPEN cursor FOR
  SELECT
    U.id,
    U.username,
    U.name,
    U.avatar_url,
    R.reply_id,
    T2.tweet_text,
    T2.image_url,
    favorite,
    retweet,
    T2.created_at
  FROM replies R INNER JOIN tweets T ON R.original_tweet_id = T.id
    INNER JOIN tweets T2 ON R.reply_id = T2.id
    INNER JOIN users U ON T2.creator_id = U.id
  WHERE R.original_tweet_id = $1
  ORDER BY T2.created_at ASC
  LIMIT 4;
  RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_replies(tweet_id INTEGER)
  RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
BEGIN
  OPEN cursor FOR
  SELECT
    U.id,
    U.username,
    U.name,
    U.avatar_url,
    R.reply_id,
    T2.tweet_text,
    T2.image_url,
    T2.created_at
  FROM replies R INNER JOIN tweets T ON R.original_tweet_id = T.id
    INNER JOIN tweets T2 ON R.reply_id = T2.id
    INNER JOIN users U ON T2.creator_id = U.id
  WHERE R.original_tweet_id = $1
  ORDER BY T2.created_at ASC;
  RETURN cursor;
END; $$
LANGUAGE PLPGSQL;
