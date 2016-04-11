-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION create_list(name       VARCHAR(50), description VARCHAR(140),
                                       session varchar, private BOOLEAN, created_at TIMESTAMP)
  RETURNS SETOF lists AS $$
DECLARE list_id INTEGER;
        userID INTEGER;
BEGIN
SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $3;
  INSERT INTO lists (name, description, creator_id, private, created_at)
  VALUES (name, description, userID, private, created_at) RETURNING id INTO list_id;

  PERFORM subscribe(userID, list_id, created_at);
  RETURN QUERY
  SELECT *
  FROM lists
  WHERE id = CURRVAL(pg_get_serial_sequence('lists', 'id'));

END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION delete_list(list_id INTEGER)
  RETURNS VOID AS $$
BEGIN
  DELETE FROM lists L
  WHERE L.id = $1;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION update_list(list_id INTEGER, params TEXT [] [2])
  RETURNS VOID AS $$
BEGIN
  FOR i IN array_lower(params, 1)..array_upper(params, 1) LOOP
    EXECUTE 'UPDATE lists' ||
            ' SET ' || quote_ident(params [i] [1]) || ' = ' || quote_literal(params [i] [2]) ||
            ' WHERE id = ' || list_id || ';';
  END LOOP;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION subscribe(session VARCHAR, list_id INTEGER)
  RETURNS VOID AS $$
DECLARE userID INTEGER;
BEGIN
  SELECT user_id
  INTO userID
  FROM sessions
  WHERE id = $1;
  INSERT INTO subscriptions (subscriber_id, list_id, created_at)
  VALUES (userID, list_id, now()::timestamp);
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION unsubscribe(session VARCHAR, list_id INTEGER)
  RETURNS VOID AS $$
DECLARE userID INTEGER;
BEGIN
  SELECT user_id
  INTO userID
  FROM sessions
  WHERE id = $1;
  DELETE FROM subscriptions S
  WHERE S.subscriber_id = userID AND S.list_id = $2;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION add_member(user_id INTEGER, list_id INTEGER)
  RETURNS VOID AS $$
BEGIN
  INSERT INTO memberships (member_id, list_id, created_at)
  VALUES (user_id, list_id, now()::timestamp);
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION delete_member(user_id INTEGER, list_id INTEGER)
  RETURNS VOID AS $$
BEGIN
  DELETE FROM memberships M
  WHERE M.member_id = $1 AND M.list_id = $2;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_list_subscribers(list_id INTEGER)
  RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
BEGIN
  OPEN cursor FOR
  SELECT
    U.name,
    U.username,
    U.avatar_url
  FROM lists L INNER JOIN subscriptions S ON L.id = S.list_id
    INNER JOIN users U ON U.id = S.subscriber_id
  WHERE L.id = $1;
  RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_list_members(list_id INTEGER)
  RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
BEGIN
  OPEN cursor FOR
  SELECT
    U.name,
    U.username,
    U.avatar_url
  FROM lists L INNER JOIN memberships M ON L.id = M.list_id
    INNER JOIN users U ON U.id = M.member_id
  WHERE L.id = $1;
  RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_list_feeds(list_id INTEGER)
  RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
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
            INNER JOIN memberships M ON M.member_id = C.id
            INNER JOIN users U ON C.id = U.id
          WHERE M.list_id = $1)
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
            U.name       AS "name2",
            U.id         AS "retweeter_id",
            R.created_at AS "creation"
          FROM tweets T INNER JOIN retweets R ON T.id = R.tweet_id
            INNER JOIN users C ON T.creator_id = C.id
            INNER JOIN memberships M ON R.retweeter_id = M.member_id
            INNER JOIN users U ON U.id = M.member_id
          WHERE M.list_id = $1)) AS feeds
  ORDER BY creation DESC;
  RETURN cursor;
END; $$
LANGUAGE PLPGSQL;
