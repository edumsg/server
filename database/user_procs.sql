-- JAVA DONE
CREATE OR REPLACE FUNCTION create_user(username varchar(30),
  email varchar(100),
  password varchar(150),
  name varchar(100),
  created_at timestamp,
  avatar_url varchar(70) DEFAULT null)
RETURNS void AS $$
  BEGIN
    INSERT INTO users(username, email, encrypted_password, name, created_at, avatar_url)
    VALUES (username, email, password, name, created_at, avatar_url);
  END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION edit_user(user_id integer, params TEXT[][2])
RETURNS void AS $$
BEGIN
  FOR i IN array_lower(params, 1)..array_upper(params, 1) LOOP
    EXECUTE 'UPDATE users' ||
      ' SET ' || quote_ident(params[i][1]) || ' = ' || quote_literal(params[i][2]) ||
      ' WHERE id = ' || user_id || ';';
  END LOOP;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_user(user_id integer)
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT * FROM users U WHERE U.id = user_id LIMIT 1;
    RETURN cursor; 
  END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_users(user_substring text)
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT U.username, U.name, U.avatar_url FROM users U
    WHERE U.name LIKE '%' || $1 || '%' OR U.username LIKE '%' || $1 || '%';
    RETURN cursor;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION follow(user_id integer, follower_id integer,
  created_at timestamp)
RETURNS void AS $$
DECLARE private_user boolean;
  BEGIN
    SELECT U.protected_tweets INTO private_user FROM users U
    WHERE U.id = $1;

    IF private_user THEN
      INSERT INTO followships(user_id, follower_id, confirmed, created_at)
      VALUES (user_id, follower_id, '0', created_at);
    ELSE
      INSERT INTO followships(user_id, follower_id, created_at)
      VALUES (user_id, follower_id, created_at);
    END IF;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION unfollow(user_id integer, follower_id integer)
RETURNS void AS $$
  BEGIN
    DELETE FROM followships F WHERE F.user_id = $1 AND F.follower_id = $2;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION confirm_follow(userid integer, followerid integer)
RETURNS void AS $$
  BEGIN
    UPDATE followships SET confirmed = TRUE
    WHERE user_id = $1 AND follower_id = $2;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_followers(user_id integer)
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT U.username, U.name, U.avatar_url
    FROM users U INNER JOIN followships F ON U.id = F.follower_id
    WHERE F.user_id = $1 AND F.confirmed = TRUE;
    RETURN cursor;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_following(user_id integer)
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT U.username, U.name, U.avatar_url
    FROM users U INNER JOIN followships F ON U.id = F.user_id
    WHERE F.follower_id = $1 AND F.confirmed = TRUE;
    RETURN cursor;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_unconfirmed_followers(user_id integer)
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT U.username, U.name, U.avatar_url
    FROM users U INNER JOIN followships F ON U.id = F.follower_id
    WHERE F.user_id = $1 AND F.confirmed = FALSE;
    RETURN cursor;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_tweets(user_id integer)
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT id, tweet_text, image_url, created_at, name, username, avatar_url
    FROM (
      (SELECT T.id, T.tweet_text, T.image_url, T.created_at, U.name, U.username, U.avatar_url, T.created_at AS "creation"
      FROM tweets T INNER JOIN users U ON T.creator_id = U.id
      WHERE T.creator_id = $1)
      UNION
      (SELECT T.id, T.tweet_text, T.image_url, T.created_at, C.name, C.username, C.avatar_url, R.created_at AS "creation"
      FROM tweets T INNER JOIN retweets R ON T.id = R.tweet_id 
        INNER JOIN users U ON R.user_id = U.id INNER JOIN users C ON T.creator_id = C.id
      WHERE U.id = $1)) AS timeline
    ORDER BY creation DESC;
    RETURN cursor;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_feeds(user_id integer)
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT id, tweet_text, image_url, created_at, name, username, avatar_url, name2
    FROM (
      (SELECT T.id, T.tweet_text, T.image_url, T.created_at, C.name, C.username, C.avatar_url, C.name AS "name2", T.created_at AS "creation"
      FROM tweets T INNER JOIN users C ON T.creator_id = C.id INNER JOIN followships F ON C.id = F.user_id
      WHERE F.confirmed = TRUE AND F.follower_id = $1)
      UNION
      (SELECT T.id, T.tweet_text, T.image_url, T.created_at, C.name, C.username, C.avatar_url, U.name AS "name2", R.created_at AS "creation"
      FROM tweets T INNER JOIN retweets R ON T.id = R.tweet_id INNER JOIN users C ON T.creator_id = C.id
        INNER JOIN followships F ON R.user_id = F.user_id INNER JOIN users U ON U.id = F.user_id
      WHERE F.confirmed = TRUE AND F.follower_id = $1)) AS feeds
    ORDER BY creation DESC;
    RETURN cursor;
  END; $$
LANGUAGE PLPGSQL;

-- NOT USED
CREATE OR REPLACE FUNCTION get_user_retweets(user_id integer)
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT T.id, T.tweet_text, T.image_url, C.name, C.username, C.avatar_url
    FROM tweets T INNER JOIN retweets R ON T.id = R.tweet_id 
      INNER JOIN users U ON R.user_id = U.id INNER JOIN users C ON T.creator_id = C.id
    WHERE U.id = $1
    ORDER BY R.created_at DESC;
    RETURN cursor;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JAVA DONE
CREATE OR REPLACE FUNCTION get_user_favorites(user_id integer)
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT T.id, T.tweet_text, T.image_url, T.created_at, C.name, C.username, C.avatar_url
    FROM tweets T INNER JOIN favorites F ON T.id = F.tweet_id
      INNER JOIN users U ON F.user_id = U.id INNER JOIN users C ON T.creator_id = C.id 
    WHERE U.id = $1
    ORDER BY F.created_at DESC;
    RETURN cursor;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_subscribed_lists(user_id integer)
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT L.id, L.name, L.description, C.name, C.username, C.avatar_url 
    FROM lists L INNER JOIN subscriptions S ON L.id = S.list_id INNER JOIN users C ON L.creator_id = C.id
    WHERE S.subscriber_id = $1;
    RETURN cursor;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_list_memberships(user_id integer)
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT L.id, L.name, L.description, C.name, C.username, C.avatar_url 
    FROM lists L INNER JOIN memberships M ON L.id = M.list_id INNER JOIN users C ON L.creator_id = C.id
    WHERE M.member_id = $1;
    RETURN cursor;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_mentions(username varchar(30))
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT T.id, T.tweet_text, T.image_url, T.created_at, U.name, U.username, U.avatar_url
    FROM tweets T INNER JOIN users U ON T.creator_id = U.id
    WHERE T.tweet_text LIKE '%@' || $1 || '%'
    ORDER BY T.created_at DESC;
    RETURN cursor;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION report_user(reported_id integer, creator_id integer, created_at timestamp)
RETURNS void AS $$
  BEGIN
    INSERT INTO reports(reported_id, creator_id, created_at, type)
    VALUES (reported_id, creator_id, created_at, 'users');
  END; $$
LANGUAGE PLPGSQL;

-- JAVA NOT NEEDED
CREATE OR REPLACE FUNCTION is_private_user(user_id integer)
RETURNS integer AS $$
DECLARE is_private boolean;
  BEGIN
    SELECT U.protected_tweets INTO is_private FROM users U WHERE U.id = $1;
    RETURN is_private;
  END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION login(user_name varchar, session_id text)
RETURNS void AS $$
  BEGIN
    UPDATE users SET session_id = $2 WHERE username = $1;
  END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION logout(user_id integer)
RETURNS void AS $$
  BEGIN
    UPDATE users SET session_id = null WHERE id = $1;
  END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION is_logged_in(user_id integer)
RETURNS integer AS $$
DECLARE session_id integer;
  BEGIN
    SELECT U.session_id INTO session_id FROM users U WHERE U.id = $1;
    RETURN session_id;
  END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_password_info(username varchar(30))
RETURNS varchar AS $$
DECLARE enc_password varchar;
  BEGIN
    SELECT U.encrypted_password INTO enc_password FROM users U WHERE U.username = $1;
    RETURN enc_password;
  END; $$
LANGUAGE PLPGSQL;
