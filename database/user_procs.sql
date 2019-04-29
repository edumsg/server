CREATE OR REPLACE FUNCTION get_user_id_from_username(user_name VARCHAR)
    RETURNS INTEGER AS $$
DECLARE userID INTEGER;

BEGIN
    -- Gets the user's id using its username and saving it to userID.
    SELECT id
    INTO userID
    FROM users
    WHERE username = $1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'no such username exists';
    ELSE
        RETURN userID;
    END IF;

END; $$
LANGUAGE PLPGSQL;


CREATE OR REPLACE FUNCTION get_user_id_from_session(session VARCHAR)
    RETURNS INTEGER AS $$
DECLARE userID INTEGER;

BEGIN

    -- Gets the user's id using its session_id and saving it to userID.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'no such session exists';
    ELSE
        RETURN userID;
    END IF;

END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_user_tweets_count(session VARCHAR)
    RETURNS INTEGER AS $$
DECLARE userID INTEGER := get_user_id_from_session($1);
        tweets_count INTEGER;
BEGIN
    SELECT count(*)
    INTO tweets_count
    FROM    users U 
        INNER JOIN 
            tweets T
        ON 
            U.id = T.creator_id
    WHERE U.id = $1;

    RETURN tweets_count;

END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_user_followers_count(session VARCHAR)
    RETURNS INTEGER AS $$
DECLARE userID INTEGER := get_user_id_from_session($1);
        followers_count INTEGER;
BEGIN
    SELECT count(*)
    INTO followers_count
    FROM    users U 
        INNER JOIN 
            followships F
        ON 
             U.id = user_id
    WHERE U.id = userID;

    RETURN followers_count;

END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_user_followings_count(session VARCHAR)
    RETURNS INTEGER AS $$
DECLARE userID INTEGER := get_user_id_from_session($1);
        followings_count INTEGER;
BEGIN
    SELECT count(*)
    INTO followings_count
    FROM    users U 
        INNER JOIN 
            followships F
        ON 
             U.id = F.follower_of_user_id
    WHERE U.id = userID;

    RETURN followings_count;

END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION create_user(username   VARCHAR(30),
                                       email      VARCHAR(100),
                                       password   VARCHAR(150),
                                       name       VARCHAR(100),
                                       avatar_url VARCHAR(70) DEFAULT 'http://i.imgur.com/LkovBT3.png')
    RETURNS SETOF users AS $$
BEGIN
    RETURN QUERY
        INSERT 
        INTO users (username, email, encrypted_password, name, created_at, avatar_url)
        VALUES (username, email, password, name, now() :: TIMESTAMP, avatar_url)
        RETURNING *;
    RETURN;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION edit_user(session VARCHAR, params TEXT [] [2])
    RETURNS VOID AS $$
DECLARE userID INTEGER := get_user_id_from_session($1);
BEGIN
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
        WHERE U.id = user_id;
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
        WHERE U.username = $1;
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
        FROM    users U 
            INNER JOIN 
                tweets T
            ON 
                U.id = T.creator_id
        WHERE U.username = $1 AND T.type = $2;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION my_profile(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER := get_user_id_from_session($1);
        tweets_count INTEGER := get_user_tweets_count($1);
        followings_count INTEGER := get_user_followings_count($1);
        followers_count INTEGER := get_user_followers_count($1);
BEGIN
    -- Finds user with ID.
    OPEN cursor FOR
        SELECT U.*, tweets_count, followings_count, followers_count
        FROM users U
        WHERE U.id = userID;
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
        userID       INTEGER := get_user_id_from_session($1);
BEGIN

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
        IF private_user THEN
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
DECLARE userID INTEGER := get_user_id_from_session($1);
BEGIN
    DELETE FROM followships F
    WHERE F.user_id = $2 AND F.follower_of_user_id = userID;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE
CREATE OR REPLACE FUNCTION confirm_follow(session VARCHAR, followerid INTEGER)
    RETURNS VOID AS $$
DECLARE userID INTEGER := get_user_id_from_session($1);
BEGIN
    UPDATE followships
    SET confirmed = TRUE
    WHERE user_id = userID AND follower_of_user_id = $2;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_followers(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE userID INTEGER := get_user_id_from_session($1);
        cursor REFCURSOR := 'cur';
BEGIN
    OPEN cursor FOR
        SELECT
            U.id,
            U.username,
            U.name,
            U.avatar_url

        FROM    users U 
            INNER JOIN 
                followships F 
            ON 
                U.id = F.follower_of_user_id
                
        WHERE F.user_id = userID AND F.confirmed = TRUE;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_following(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER := get_user_id_from_session($1);
BEGIN
    OPEN cursor FOR
        SELECT
            U.username,
            U.name,
            U.avatar_url
        
        FROM    users U 
            INNER JOIN 
                followships F 
            ON 
                U.id = F.user_id
        
        WHERE F.follower_of_user_id = userID AND F.confirmed = TRUE;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_unconfirmed_followers(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER := get_user_id_from_session($1);
BEGIN

    OPEN cursor FOR
        SELECT
            U.username,
            U.name,
            U.avatar_url
        FROM    users U 
            INNER JOIN 
                followships F 
            ON 
                U.id = F.follower_of_user_id
        WHERE F.user_id = userID AND F.confirmed = FALSE;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- Gets user tweets and retweets using its session.
CREATE OR REPLACE FUNCTION get_tweets(session VARCHAR, type VARCHAR) 
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER := get_user_id_from_session($1);
BEGIN

    OPEN cursor FOR
    SELECT
        id,
        tweet_text,
        creation,
        timeline.type,
        image_url,
        creator_id,
        name,
        username,
        avatar_url
    FROM (
            -- Gets tweets originally by user.
            ( SELECT
                T.*,
                U.name,
                U.username,
                U.avatar_url,
                T.created_at AS "creation"

            FROM    tweets T 
                INNER JOIN 
                    users U 
                ON 
                    T.creator_id = U.id

            WHERE T.creator_id = userID AND T.type = $2 )
        UNION
            -- Gets tweets retweeted by user.
            ( SELECT
                T.*,
                C.name,
                C.username,
                C.avatar_url,
                R.created_at AS "creation"

            FROM    tweets T 
                INNER JOIN 
                    retweets R 
                ON 
                    T.id = R.tweet_id
                INNER JOIN 
                    users U 
                ON 
                    R.retweeter_id = U.id
                INNER JOIN
                    users C
                ON 
                    C.id = R.creator_id
                    
            WHERE U.id = userID AND T.type = $2) ) AS timeline

    ORDER BY creation DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- Gets user tweets and retweets using its username.
CREATE OR REPLACE FUNCTION get_tweets2(user_name VARCHAR, type VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER := get_user_id_from_username($1);
BEGIN

    OPEN cursor FOR
    SELECT
        id,
        tweet_text,
        creation,
        timeline.type,
        image_url,
        creator_id,
        name,
        username,
        avatar_url
    FROM (
            -- Gets tweets originally by user.
            ( SELECT
                T.*,
                U.name,
                U.username,
                U.avatar_url,
                T.created_at AS "creation"

            FROM    tweets T 
                INNER JOIN 
                    users U 
                ON 
                    T.creator_id = U.id

            WHERE T.creator_id = userID AND T.type = $2 )
        UNION
            -- Gets tweets retweeted by user.
            ( SELECT
                T.*,
                C.name,
                C.username,
                C.avatar_url,
                R.created_at AS "creation"

            FROM    tweets T 
                INNER JOIN 
                    retweets R 
                ON 
                    T.id = R.tweet_id
                INNER JOIN 
                    users U 
                ON 
                    R.retweeter_id = U.id
                INNER JOIN 
                    users C
                ON
                    R.creator_id = C.id                    
            WHERE U.id = userID AND T.type = $2) ) AS timeline

    ORDER BY creation DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;


CREATE OR REPLACE FUNCTION get_tweets_by_id(user_id INTEGER, type VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER := $1;
BEGIN

    OPEN cursor FOR
    SELECT
        id,
        tweet_text,
        creation,
        timeline.type,
        image_url,
        name,
        username,
        avatar_url
    FROM (
            -- Gets tweets originally by user.
            ( SELECT
                T.*,
                U.id AS "creator_id",
                U.name,
                U.username,
                U.avatar_url,
                T.created_at AS "creation"

            FROM    tweets T 
                INNER JOIN 
                    users U 
                ON 
                    T.creator_id = U.id

            WHERE T.creator_id = userID AND T.type = $2 )
        UNION
            -- Gets tweets retweeted by user.
            ( SELECT
                T.*,
                R.retweeter_id AS "creator_id",
                U.name,
                U.username,
                U.avatar_url,
                R.created_at AS "creation"

            FROM    tweets T 
                INNER JOIN 
                    retweets R 
                ON 
                    T.id = R.tweet_id
                INNER JOIN 
                    users U 
                ON 
                    R.retweeter_id = U.id
                AND 
                    T.creator_id = U.id
                    
            WHERE U.id = userID AND T.type = $2) ) AS timeline

    ORDER BY creation DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;



-- JAVA / JSON DONE
-- Gets timeline of user
CREATE OR REPLACE FUNCTION get_feeds(session VARCHAR , type VARCHAR) 
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER := get_user_id_from_session($1);
BEGIN

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
              WHERE F.confirmed = TRUE AND F.follower_of_user_id = userID AND T.type = $2)
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
              WHERE F.confirmed = TRUE AND F.follower_of_user_id = userID AND T.type = $2)) AS feeds
    ORDER BY creation DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;


CREATE OR REPLACE FUNCTION get_timeline_with_type(session VARCHAR, type VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE userID INTEGER := get_user_id_from_session($1);
        followedUser followships%ROWTYPE;
        fetchedTweet RECORD;
        timelineCursor REFCURSOR := 'cur';

BEGIN

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

        -- Fetches followed user tweets & retweets.
        DECLARE cursor REFCURSOR := get_tweets_by_id(followedUser.user_id,$2);

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
            CAST (tweet->>'image_url' AS VARCHAR) AS image_url,
            CAST (tweet->>'name' AS VARCHAR) AS name,
            CAST (tweet->>'username' AS VARCHAR) AS username,
            CAST (tweet->>'avatar_url' AS VARCHAR) AS avatar_url,
            CAST (tweet->>'name2' AS VARCHAR) AS retweeter_name,
            CAST (tweet->>'retweeter_id' AS INTEGER) AS retweeter_id
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
        userID INTEGER := get_user_id_from_session($1);
BEGIN
    OPEN cursor FOR
        SELECT
            T.id,
            T.tweet_text,
            T.image_url,
            C.name,
            C.username,
            C.avatar_url

        FROM    tweets T 
            INNER JOIN 
                retweets R 
            ON 
                T.id = R.tweet_id
            INNER JOIN 
                users U 
            ON 
                R.retweeter_id = U.id
            INNER JOIN 
                users C 
            ON 
                T.creator_id = C.id
                
        WHERE U.id = userID
        ORDER BY R.created_at DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- NOT USED
CREATE OR REPLACE FUNCTION get_retweets_ids(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER := get_user_id_from_session($1);
BEGIN

    OPEN cursor FOR
        SELECT R.tweet_id
        
        FROM    retweets R 
            INNER JOIN 
                users U 
            ON 
                R.retweeter_id = U.id

        WHERE U.id = userID
        ORDER BY R.created_at DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JAVA DONE
CREATE OR REPLACE FUNCTION get_user_favorites(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER := get_user_id_from_session($1);
BEGIN

    OPEN cursor FOR
        SELECT
            T.id,
            T.tweet_text,
            T.image_url,
            T.created_at,
            C.name,
            C.username,
            C.avatar_url
            
        FROM    tweets T 
            INNER JOIN 
                favorites F 
            ON 
                T.id = F.tweet_id
            INNER JOIN 
                users U 
            ON 
                F.user_id = U.id
            INNER JOIN 
                users C 
            ON 
                T.creator_id = C.id

        WHERE U.id = userID
        ORDER BY F.created_at DESC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_subscribed_lists(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER := get_user_id_from_session($1);
BEGIN
    OPEN cursor FOR
        SELECT
            L.id,
            L.name,
            L.description,
            C.name,
            C.username,
            C.avatar_url

        FROM    lists L 
            INNER JOIN 
                subscriptions S 
            ON
                L.id = S.list_id
            INNER JOIN 
                users C 
            ON 
                L.creator_id = C.id
                
        WHERE S.subscriber_id = userID
        ORDER BY L.name ASC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_list_memberships(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER := get_user_id_from_session($1);
BEGIN
    OPEN cursor FOR
        SELECT
            L.id,
            L.name,
            L.description,
            C.name,
            C.username,
            C.avatar_url
        
        FROM    lists L 
            INNER JOIN 
                memberships M 
            ON 
                L.id = M.list_id
            INNER JOIN 
                users C 
            ON 
                L.creator_id = C.id
        
        WHERE M.member_id = userID;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA DONE / JSON DONE
CREATE OR REPLACE FUNCTION get_mentions(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
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
DECLARE userID INTEGER := get_user_id_from_session($2);
BEGIN
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
        userID INTEGER := get_user_id_from_username($1);

BEGIN
    INSERT INTO sessions AS S (id, user_id, session_start, created_at)
    VALUES ($2, userID, now() :: TIMESTAMP, now() :: TIMESTAMP)
    ON CONFLICT (user_id)
        DO UPDATE SET session_start = now() :: TIMESTAMP, id = $2, session_end = NULL
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
DECLARE userID INTEGER := get_user_id_from_username($1);
        session VARCHAR;
BEGIN
    SELECT id
    INTO session
    FROM sessions
    WHERE user_id = userID;
    RETURN session;
END; $$
LANGUAGE PLPGSQL;

-- Checks if user_name follows seassion's user
CREATE OR REPLACE FUNCTION is_following(session VARCHAR, user_name VARCHAR)
    RETURNS BOOLEAN AS $$
DECLARE isFollowing BOOLEAN := FALSE;
        userID       INTEGER := get_user_id_from_session($1);
        followerID   INTEGER;
BEGIN

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
        userID INTEGER := get_user_id_from_session($1);
        followedUserID INTEGER;
BEGIN

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
