-- JAVA DONE
CREATE OR REPLACE FUNCTION create_user(username   VARCHAR(30),
                                       email      VARCHAR(100),
                                       password   VARCHAR(150),
                                       name       VARCHAR(100),
                                       avatar_url VARCHAR(70) DEFAULT 'http://i.imgur.com/LkovBT3.png')
    RETURNS SETOF users AS $$
BEGIN
    INSERT INTO users (username, email, encrypted_password, name, created_at, avatar_url)
    VALUES (username, email, password, name, now() :: TIMESTAMP, avatar_url);
    RETURN QUERY
    SELECT *
    FROM users
    WHERE id = CURRVAL(pg_get_serial_sequence('users', 'id')); -- Gets the last user id entered.
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION edit_user(session VARCHAR, params TEXT [] [2])
    RETURNS VOID AS $$
DECLARE userID INTEGER;
BEGIN

    -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    FOR i IN array_lower(params, 1)..array_upper(params, 1) LOOP
        -- EXECUTE 'UPDATE users' ||
        --         ' SET ' || quote_ident(params [i] [1]) || ' = ' || quote_literal(params [i] [2]) ||
        --         ' WHERE id = ' || userID || ';';
        EXECUTE format('UPDATE users SET %I = %L WHERE id = $1;', params[i][1], params[i][2])
                USING (userID);
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
    WHERE U.username = $1
    LIMIT 1;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_user_with_tweets(username VARCHAR, type VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
BEGIN

    -- Finds tweets created by username.
    OPEN cursor FOR
    SELECT *
    FROM users U JOIN tweets T
    ON U.id = T.creator_id
    WHERE U.username = $1 AND T.type = $2;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION my_profile(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN

    -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    -- Finds user with ID.
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

    -- Finds users with username or names that contains (user_substring).
    OPEN cursor FOR
    SELECT
        U.id,
        U.username,
        U.name,
        U.avatar_url
    FROM users U
    WHERE U.name LIKE '%' || $1 || '%' OR U.username LIKE '%' || $1 || '%';
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION follow(session VARCHAR, followee_id INTEGER)
    RETURNS VOID AS $$
DECLARE private_user BOOLEAN;
        userID       INTEGER;
BEGIN

    -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    -- Checks if followee has a private or public account.
    SELECT U.protected_tweets
    INTO private_user
    FROM users U
    WHERE U.id = $2;

    -- Checks if user already follows followee_id.
    PERFORM ID
    FROM followships
    WHERE user_id = $2 AND follower_of_user_id = userID;

    IF FOUND THEN
        -- If yes unfollow user.
        PERFORM unfollow($1,$2);
    ELSE
        IF private_user
        THEN
            INSERT INTO followships (user_id, follower_of_user_id, confirmed, created_at)
            VALUES (followee_id, userID, '0', now() :: TIMESTAMP);
        ELSE
            INSERT INTO followships (user_id, follower_of_user_id, confirmed, created_at)
            VALUES (followee_id, userID, '1', now() :: TIMESTAMP);
        END IF;
    END IF;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION unfollow(session VARCHAR, follower_of_user_id INTEGER)
    RETURNS VOID AS $$
DECLARE userID INTEGER;
BEGIN

    -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    DELETE FROM followships F
    WHERE F.user_id = $2 AND F.follower_of_user_id = userID;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION confirm_follow(session VARCHAR, followerid INTEGER)
    RETURNS VOID AS $$
DECLARE userID INTEGER;
BEGIN

    -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    UPDATE followships
    SET confirmed = TRUE
    WHERE user_id = userID AND follower_of_user_id = $2;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_followers(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE userID INTEGER;
        cursor REFCURSOR := 'cur';
BEGIN

    -- Finds user's id through user's session.
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

    -- Finds user's id through user's session.
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

    -- Finds user's id through user's session.
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

CREATE OR REPLACE FUNCTION get_tweets(session VARCHAR, type VARCHAR) -- Gets user tweets and retweets
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN

    -- Finds user's id through user's session.
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
            -- Gets tweets created by the user.
             (SELECT
                  T.id,
                  T.tweet_text,
                  T.image_url,
                  T.created_at,
                  U.id AS "creator_id",
                  U.name,
                  U.username,
                  U.avatar_url,
                  T.created_at AS "creation"
              FROM tweets T INNER JOIN users U ON T.creator_id = U.id
              WHERE T.creator_id = userID AND T.type = $2)
             UNION
             -- Gets tweets retweeted by the user.
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
              WHERE U.id = userID AND T.type = $2)) AS timeline
    ORDER BY creation DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_tweets2(uname VARCHAR, type VARCHAR) -- Gets user tweets and retweets
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN

    -- Finds user's id through user's username.
    SELECT id
    INTO userID
    FROM users
    WHERE username = $1;

    OPEN cursor FOR
    SELECT
        id,
        tweet_text,
        creator_id,
        created_at,
        timeline.type,
        image_url,
        name,
        username,
        avatar_url
    FROM (
            -- Gets tweets created by the user.
             (SELECT
                  T.id,
                  T.tweet_text,
                  T.image_url,
                  T.created_at,
                  T.type,
                  U.id         AS "creator_id",
                  U.name,
                  U.username,
                  U.avatar_url,
                  T.created_at AS "creation"
              FROM tweets T INNER JOIN users U ON T.creator_id = U.id
              WHERE T.creator_id = userID AND T.type = $2)
             UNION
             -- Gets tweets retweeted by the user.
             (SELECT
                  T.id,
                  T.tweet_text,
                  T.image_url,
                  T.created_at,
                  T.type,
                  C.id         AS "creator_id",
                  C.name,
                  C.username,
                  C.avatar_url,
                  R.created_at AS "creation"
              FROM tweets T INNER JOIN retweets R ON T.id = R.tweet_id
                  INNER JOIN users U ON R.retweeter_id = U.id
                  INNER JOIN users C ON T.creator_id = C.id
              WHERE U.id = userID AND T.type = $2)) AS timeline
    ORDER BY creation DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_feeds(session VARCHAR) -- Gets timeline of user
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN

    -- Finds user's id through user's session.
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

-- Use The Type Attribute.
CREATE OR REPLACE FUNCTION get_timeline_with_type(session VARCHAR, type VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE userID INTEGER;
        followedUser followships%ROWTYPE;
        fetchedTweet tweets%ROWTYPE;
        followedUsername VARCHAR;
        timelineCursor REFCURSOR := 'cur';

BEGIN

    -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    -- A table to hold the result set from the query.
    CREATE TEMP TABLE IF NOT EXISTS tweetsResultSet (
        tweet JSONB
    );

    -- Loops overs the users followed by the session's user.
    FOR followedUser IN 
        SELECT *
        FROM followships
        WHERE follower_of_user_id = userID
    LOOP
        -- Fetches the id of the followed user.
        SELECT username
        INTO followedUsername
        FROM users U INNER JOIN followships F ON U.id = F.user_id
        WHERE user_id = followedUser.user_id;

        -- Fetches followed user tweets & retweets.
        DECLARE cursor REFCURSOR := get_tweets2(followedUsername,$2);

        BEGIN
            LOOP
                -- Loops over tweets & retweets and saves them as json.
                FETCH cursor INTO fetchedTweet;
                EXIT WHEN NOT FOUND;
                INSERT INTO tweetsResultSet VALUES (to_jsonb(fetchedTweet));
            END LOOP;
            CLOSE cursor;
		END;
    
    END LOOP;

    -- Returns all tweets with the specified type.
    OPEN timelineCursor FOR
        SELECT 
            CAST (tweet->>'id' AS INTEGER) AS id,
            CAST (tweet->>'tweet_text' AS VARCHAR) AS tweet_text,
            CAST (tweet->>'creator_id' AS INTEGER) AS creator_id,
            CAST (tweet->>'created_at' AS TIMESTAMP) AS created_at,
            CAST (tweet->>'type' AS VARCHAR) AS type,
            CAST (tweet->>'image_url' AS VARCHAR) AS image_url
        FROM tweetsResultSet;

        -- Removes all data in table to avoid accumlation of results.
        BEGIN
            DELETE FROM tweetsResultSet;
        END;

    RETURN timelineCursor;
END; $$
LANGUAGE PLPGSQL;



-- NOT USED
CREATE OR REPLACE FUNCTION get_user_retweets(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;
BEGIN

    -- Finds user's id through user's session.
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

    -- Finds user's id through user's session.
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
    WHERE S.subscriber_id = userID
    ORDER BY L.name ASC;
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
    INSERT INTO reports (reported_id, creator_id, created_at, type)
    VALUES (reported_id, userID, now()::TIMESTAMP, 'users');
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
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER;

BEGIN
    SELECT id
    INTO userID
    FROM users
    WHERE username = $1;

    INSERT INTO sessions AS S (id, user_id, session_start, created_at)
    VALUES ($2, userID, now() :: TIMESTAMP, now() :: TIMESTAMP)
    ON CONFLICT (user_id)
        DO UPDATE SET session_start = now() :: TIMESTAMP, id = $2, session_end = null
            WHERE S.user_id = userID;

    OPEN cursor FOR
    SELECT *
    FROM users
    WHERE id = userID;

    RETURN cursor;
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

-- Checks if user_name follows seassion's user
CREATE OR REPLACE FUNCTION is_following(session VARCHAR, user_name VARCHAR)
    RETURNS BOOLEAN AS $$
DECLARE isFollowing BOOLEAN := FALSE;
        userID       INTEGER;
        followerID   INTEGER;
BEGIN

    -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    -- Finds the id of follower using username.
    SELECT U.id
    INTO followerID
    FROM users U
    WHERE U.username = $2;

    SELECT confirmed
    INTO isFollowing
    FROM followships
    WHERE user_id = userID AND follower_of_user_id = followerID;
    
    IF isFollowing = true THEN
        RETURN TRUE;
    ELSE  
        RETURN FALSE;
    END IF;
END; $$
LANGUAGE PLPGSQL;


CREATE OR REPLACE FUNCTION is_following_user(session VARCHAR, username VARCHAR)
    RETURNS BOOLEAN AS $$
DECLARE isFollowing BOOLEAN:= FALSE;
        userID INTEGER;
        followedUserID INTEGER;
BEGIN

    -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    -- Finds the id of follower using username.
    SELECT U.id
    INTO followedUserID
    FROM users U
    WHERE U.username = $2;

    SELECT F.confirmed
    INTO isFollowing
    FROM users U INNER JOIN followships F ON U.id = F.user_id
    WHERE F.follower_of_user_id = userID AND F.confirmed = TRUE AND F.user_id = followedUserID;

    IF isFollowing = true THEN
        RETURN TRUE;
    ELSE  
        RETURN FALSE;
    END IF;

END; $$
LANGUAGE PLPGSQL;
