-- JAVA DONE
CREATE OR REPLACE FUNCTION create_user(username   VARCHAR(30),
                                       email      VARCHAR(100),
                                       password   VARCHAR(150),
                                       name       VARCHAR(100),
                                       avatar_url VARCHAR(70) DEFAULT NULL)
    RETURNS SETOF users AS $$
BEGIN
    INSERT INTO users (username, email, encrypted_password, name, created_at, avatar_url)
    VALUES (username, email, password, name, now() :: TIMESTAMP, avatar_url);
    RETURN QUERY
    SELECT *
    FROM users
    WHERE id = CURRVAL(pg_get_serial_sequence('users', 'id'));
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION edit_user(session VARCHAR, params TEXT [] [2])
    RETURNS VOID AS $$
DECLARE userID INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;
    FOR i IN array_lower(params, 1)..array_upper(params, 1) LOOP
        EXECUTE 'UPDATE users' ||
                ' SET ' || quote_ident(params [i] [1]) || ' = ' || quote_literal(params [i] [2]) ||
                ' WHERE id = ' || userID || ';';
    END LOOP;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_user(user_id INTEGER)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
BEGIN
    OPEN cursor FOR
    SELECT *
    FROM users U
    WHERE U.id = user_id
    LIMIT 1;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_user2(username VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
BEGIN
    OPEN cursor FOR
    SELECT *
    FROM users U
    WHERE U.username = username
    LIMIT 1;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_user_with_tweets(username VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
BEGIN
    OPEN cursor FOR
    SELECT *
    FROM users U JOIN tweets T
    ON U.id = T.creator_id
    WHERE U.username = username;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION my_profile(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    OPEN cursor FOR
    SELECT *
    FROM users U
    WHERE U.id = userID
    LIMIT 1;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_users(user_substring TEXT)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
BEGIN
    OPEN cursor FOR
    SELECT
        U.username,
        U.name,
        U.avatar_url
    FROM users U
    WHERE U.name LIKE '%' || $1 || '%' OR U.username LIKE '%' || $1 || '%';
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION follow(session VARCHAR, followee_id INTEGER, created_at TIMESTAMP)
    RETURNS VOID AS $$
DECLARE private_user BOOLEAN;
        userID       INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    SELECT U.protected_tweets
    INTO private_user
    FROM users U
    WHERE U.id = $1;

    IF private_user
    THEN
        INSERT INTO followships (user_id, follower_of_user_id, confirmed, created_at)
        VALUES (followee_id, user_id, '0', now() :: TIMESTAMP);
    ELSE
        INSERT INTO followships (user_id, follower_of_user_id, created_at)
        VALUES (followee_id, user_id, now() :: TIMESTAMP);
    END IF;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION unfollow(session VARCHAR, follower_of_user_id INTEGER)
    RETURNS VOID AS $$
DECLARE userID INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    DELETE FROM followships F
    WHERE F.user_id = $2 AND F.follower_of_user_id = user_id;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION confirm_follow(session VARCHAR, followerid INTEGER)
    RETURNS VOID AS $$
DECLARE userID INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    UPDATE followships
    SET confirmed = TRUE
    WHERE userID = $1 AND follower_of_user_id = $2;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_followers(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE userID INTEGER;
        cursor REFCURSOR := 'cur';
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    OPEN cursor FOR
    SELECT
        U.id,
        U.username,
        U.name,
        U.avatar_url
    FROM users U INNER JOIN followships F ON U.id = F.follower_of_user_id
    WHERE F.user_id = userID AND F.confirmed = TRUE;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_following(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN

    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;
    OPEN cursor FOR
    SELECT
        U.username,
        U.name,
        U.avatar_url
    FROM users U INNER JOIN followships F ON U.id = F.user_id
    WHERE F.follower_of_user_id = userID AND F.confirmed = TRUE;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_unconfirmed_followers(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    OPEN cursor FOR
    SELECT
        U.username,
        U.name,
        U.avatar_url
    FROM users U INNER JOIN followships F ON U.id = F.follower_of_user_id
    WHERE F.user_id = userID AND F.confirmed = FALSE;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_tweets(session VARCHAR) --gets user tweets and retweets
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    OPEN cursor FOR
    SELECT
        id,
        tweet_text,
        image_url,
        created_at,
        creator_id,
        name,
        username,
        avatar_url
    FROM (
             (SELECT
                  T.id,
                  T.tweet_text,
                  T.image_url,
                  T.created_at,
                  U.id         AS "creator_id",
                  U.name,
                  U.username,
                  U.avatar_url,
                  T.created_at AS "creation"
              FROM tweets T INNER JOIN users U ON T.creator_id = U.id
              WHERE T.creator_id = userID)
             UNION
             (SELECT
                  T.id,
                  T.tweet_text,
                  T.image_url,
                  T.created_at,
                  C.id         AS "creator_id",
                  C.name,
                  C.username,
                  C.avatar_url,
                  R.created_at AS "creation"
              FROM tweets T INNER JOIN retweets R ON T.id = R.tweet_id
                  INNER JOIN users U ON R.retweeter_id = U.id
                  INNER JOIN users C ON T.creator_id = C.id
              WHERE U.id = userID)) AS timeline
    ORDER BY creation DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_feeds(session VARCHAR) --gets timeline of user
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    OPEN cursor FOR
    SELECT
        id,
        tweet_text,
        image_url,
        created_at,
        name,
        username,
        avatar_url,
        name2,
        creator_id,
        retweeter_id
    FROM (
             (SELECT
                  T.id,
                  T.tweet_text,
                  T.image_url,
                  T.created_at,
                  C.id         AS "creator_id",
                  C.name,
                  C.username,
                  C.avatar_url,
                  C.name       AS "name2",
                  U.id         AS "retweeter_id",
                  T.created_at AS "creation"
              FROM tweets T INNER JOIN users C ON T.creator_id = C.id
                  INNER JOIN followships F ON C.id = F.user_id
                  INNER JOIN users U ON C.id = U.id
              WHERE F.confirmed = TRUE AND F.follower_of_user_id = userID)
             UNION ALL
             (SELECT
                  T.id,
                  T.tweet_text,
                  T.image_url,
                  T.created_at,
                  C.id         AS "creator_id",
                  C.name,
                  C.username,
                  C.avatar_url,
                  U.name       AS "name2",
                  U.id         AS "retweeter_id",
                  R.created_at AS "creation"
              FROM tweets T INNER JOIN retweets R ON T.id = R.tweet_id
                  INNER JOIN users C ON T.creator_id = C.id
                  INNER JOIN followships F ON R.retweeter_id = F.user_id
                  INNER JOIN users U ON U.id = F.user_id
              WHERE F.confirmed = TRUE AND F.follower_of_user_id = userID)) AS feeds
    ORDER BY creation DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- NOT USED
CREATE OR REPLACE FUNCTION get_user_retweets(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    OPEN cursor FOR
    SELECT
        T.id,
        T.tweet_text,
        T.image_url,
        C.name,
        C.username,
        C.avatar_url
    FROM tweets T INNER JOIN retweets R ON T.id = R.tweet_id
        INNER JOIN users U ON R.retweeter_id = U.id
        INNER JOIN users C ON T.creator_id = C.id
    WHERE U.id = userID
    ORDER BY R.created_at DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_retweets_ids(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    OPEN cursor FOR
    SELECT R.tweet_id
    FROM retweets R INNER JOIN users U ON R.retweeter_id = U.id
    WHERE U.id = userID
    ORDER BY R.created_at DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JAVA DONE
CREATE OR REPLACE FUNCTION get_user_favorites(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor  REFCURSOR := 'cur';
        DECLARE userID INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    OPEN cursor FOR
    SELECT
        T.id,
        T.tweet_text,
        T.image_url,
        T.created_at,
        C.name,
        C.username,
        C.avatar_url
    FROM tweets T INNER JOIN favorites F ON T.id = F.tweet_id
        INNER JOIN users U ON F.user_id = U.id
        INNER JOIN users C ON T.creator_id = C.id
    WHERE U.id = userID
    ORDER BY F.created_at DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_subscribed_lists(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    OPEN cursor FOR
    SELECT
        L.id,
        L.name,
        L.description,
        C.name,
        C.username,
        C.avatar_url
    FROM lists L INNER JOIN subscriptions S ON L.id = S.list_id
        INNER JOIN users C ON L.creator_id = C.id
    WHERE S.subscriber_id = userID;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_list_memberships(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    OPEN cursor FOR
    SELECT
        L.id,
        L.name,
        L.description,
        C.name,
        C.username,
        C.avatar_url
    FROM lists L INNER JOIN memberships M ON L.id = M.list_id
        INNER JOIN users C ON L.creator_id = C.id
    WHERE M.member_id = userID;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_mentions(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor        REFCURSOR := 'cur';
        user_username VARCHAR;
BEGIN
    SELECT username
    INTO user_username
    FROM users
    WHERE id = (SELECT user_id
                FROM sessions
                WHERE id = $1);

    OPEN cursor FOR
    SELECT
        T.id,
        T.tweet_text,
        T.image_url,
        T.created_at,
        U.name,
        U.username,
        U.avatar_url
    FROM tweets T INNER JOIN users U ON T.creator_id = U.id
    WHERE T.tweet_text LIKE '%@' || user_username || '%'
    ORDER BY T.created_at DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION report_user(reported_id INTEGER, session VARCHAR)
    RETURNS VOID AS $$
DECLARE userID INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $2;
    INSERT INTO reports (reported_id, userID, created_at, type)
    VALUES (reported_id, creator_id, created_at, 'users');
END; $$
LANGUAGE PLPGSQL;

-- JAVA NOT NEEDED
CREATE OR REPLACE FUNCTION is_private_user(user_id INTEGER)
    RETURNS INTEGER AS $$
DECLARE is_private BOOLEAN;
BEGIN
    SELECT U.protected_tweets
    INTO is_private
    FROM users U
    WHERE U.id = $1;
    RETURN is_private;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION login(user_name VARCHAR, session VARCHAR)
    RETURNS VOID AS $$
DECLARE userID INTEGER;

BEGIN
    SELECT id
    INTO userID
    FROM users
    WHERE username = $1;

    INSERT INTO sessions AS S (id, user_id, session_start, created_at)
    VALUES ($2, userID, now() :: TIMESTAMP, now() :: TIMESTAMP)
    ON CONFLICT (user_id)
        DO UPDATE SET session_start = now() :: TIMESTAMP, id = $2
            WHERE S.user_id = userID;
END; $$
LANGUAGE PLPGSQL;


CREATE OR REPLACE FUNCTION logout(session VARCHAR)
    RETURNS VOID AS $$
BEGIN
    UPDATE sessions
    SET session_end = now() :: TIMESTAMP
    WHERE id = $1;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_password_info(username VARCHAR(30))
    RETURNS VARCHAR AS $$
DECLARE enc_password VARCHAR;
BEGIN
    SELECT U.encrypted_password
    INTO enc_password
    FROM users U
    WHERE U.username = $1;
    RETURN enc_password;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_session(username VARCHAR(30))
    RETURNS VARCHAR AS $$
DECLARE userID  INTEGER;
        session VARCHAR;
BEGIN
    SELECT id
    INTO userID
    FROM users
    WHERE username = $1;
    SELECT id
    INTO session
    FROM sessions
    WHERE user_id = userID;
    RETURN SESSION;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION is_following(session VARCHAR, user_name VARCHAR)
    RETURNS BOOLEAN AS $$
DECLARE is_following BOOLEAN;
        userID       INTEGER;
        userID2      INTEGER;
BEGIN
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    SELECT id
    INTO userID2
    FROM users
    WHERE username = $2;

    SELECT confirmed
    INTO is_following
    FROM followships
    WHERE user_id = userID AND follower_of_user_id = userID2;

    IF FOUND
    THEN
        RETURN is_following;
    ELSE
        RETURN FALSE;
    END IF;

END; $$
LANGUAGE PLPGSQL;
