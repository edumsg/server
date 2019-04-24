-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION create_list(name VARCHAR(50), description VARCHAR(140),
                                       session varchar, private BOOLEAN)
  RETURNS RECORD AS $$
DECLARE list_id INTEGER;
        new_list lists%ROWTYPE;
        userID INTEGER := get_user_id_from_session($3);
BEGIN

  INSERT 
  INTO lists
  VALUES (DEFAULT, name, description, userID, private, now()::TIMESTAMP)
  RETURNING * 
  INTO new_list;

  PERFORM subscribe(session, list_id);
  
  RETURN new_list;

END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION create_list_with_members(name VARCHAR(50), description VARCHAR(140),
                                       session varchar, private BOOLEAN, members VARCHAR[])
RETURNS VOID AS $$
DECLARE list_id INTEGER;
        userID INTEGER := get_user_id_from_session($3);
BEGIN

  INSERT 
  INTO lists
  VALUES (DEFAULT, name, description, userID, private, now()::TIMESTAMP) 
  RETURNING id INTO list_id;

  PERFORM subscribe2(userID, list_id);

  IF members <> ARRAY[]::VARCHAR[] THEN
    FOR i IN array_lower(members,1)..array_upper(members,1) LOOP
        PERFORM add_member_with_username(quote_ident(members [i]),list_id);
    END LOOP;
  END IF;

END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION delete_list(session VARCHAR,list_id INTEGER)
  RETURNS VOID AS $$
  DECLARE userID INTEGER := get_user_id_from_session($1);
          creatorID INTEGER;
BEGIN

  SELECT creator_id
  INTO creatorID
  FROM lists
  WHERE id = $2;

  IF creatorID <> userID THEN
    RAISE EXCEPTION 'cannot delete a list you didn''t create';
  ELSE
    DELETE 
    FROM lists L
    WHERE L.id = $2 AND L.creator_id = userID;
  END IF;

END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION update_list(list_id INTEGER, params TEXT [])
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
DECLARE userID INTEGER := get_user_id_from_session($1);
BEGIN

  PERFORM id
  FROM lists
  WHERE id = $2
  LIMIT 1;

  IF FOUND THEN
    INSERT 
    INTO subscriptions
    VALUES (DEFAULT, userID, list_id, now()::timestamp);
  ELSE  
    RAISE EXCEPTION 'no such list exists';
  END IF;

END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION subscribe2(userID INTEGER, list_id INTEGER)
  RETURNS VOID AS $$
BEGIN

  PERFORM id
  FROM lists
  WHERE id = $2
  LIMIT 1;

  IF FOUND THEN
    INSERT 
    INTO subscriptions
    VALUES (DEFAULT, userID, list_id, now()::timestamp);
  ELSE  
    RAISE EXCEPTION 'no such list exists';
  END IF;

END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION unsubscribe(session VARCHAR, list_id INTEGER)
  RETURNS VOID AS $$
DECLARE userID INTEGER := get_user_id_from_session($1);
        creatorID INTEGER;
BEGIN

  SELECT creator_id
  INTO creatorID
  FROM lists
  WHERE id = $2;

  IF FOUND THEN 
    IF userID = creatorID THEN
      RAISE EXCEPTION 'cannot unsubscribe from your own list';
    ELSE
      DELETE FROM subscriptions S
      WHERE S.subscriber_id = userID AND S.list_id = $2;
    END IF;
  ELSE
    RAISE EXCEPTION 'no such list exists';
  END IF;

END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION add_member(user_id INTEGER, list_id INTEGER)
  RETURNS VOID AS $$
BEGIN

  PERFORM id
  FROM lists
  WHERE id = $2
  LIMIT 1;

  IF FOUND THEN
    INSERT 
    INTO memberships
    VALUES(DEFAULT, $1, $2, now()::timestamp);
  ELSE 
    RAISE EXCEPTION 'no such list exists';
  END IF;

END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION add_member_with_username(user_name VARCHAR, list_id INTEGER)
  RETURNS VOID AS $$
  DECLARE userID INTEGER := get_user_id_from_username($1);
BEGIN

  PERFORM id
  FROM lists
  WHERE id = $2
  LIMIT 1;

  IF FOUND THEN
    INSERT 
    INTO memberships
    VALUES(DEFAULT, $1, $2, now()::timestamp);
  ELSE 
    RAISE EXCEPTION 'no such list exists';
  END IF;

END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION delete_member(session INTEGER, member_id INTEGER, list_id INTEGER)
  RETURNS VOID AS $$
DECLARE userID INTEGER := get_user_id_from_session($1);
        creatorID INTEGER;
BEGIN

  SELECT creator_id
  INTO creatorID
  FROM lists
  WHERE id = $3;

  IF userID = creatorID THEN 
    RAISE EXCEPTION 'cannot delete the owner of the list';
  ELSE 
    DELETE 
    FROM memberships M
    WHERE M.member_id = userID AND M.list_id = $2;
  END IF;
  
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
      
    FROM  lists L
      INNER JOIN 
          subscriptions S 
      ON 
          L.id = S.list_id
      INNER JOIN
          users U 
      ON 
          U.id = S.subscriber_id

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

    FROM  lists L 
      INNER JOIN 
          memberships M 
      ON 
          L.id = M.list_id
      INNER JOIN
          users U 
      ON
          U.id = M.member_id

    WHERE L.id = $1
    LIMIT 1;
  RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_list_feeds(list_id INTEGER, type VARCHAR)
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
    retweeter_name,
    retweeter_username,
    creation
  FROM (
        -- Gets tweets originally made by members.
         (  SELECT
              T.id,
              T.tweet_text,
              T.image_url,
              T.created_at,
              C.name,
              C.username,
              C.avatar_url,
              NULL          AS "retweeter_name",
              NULL          AS "retweeter_username",
              NULL          AS "creation"

            FROM    tweets T 
                INNER JOIN 
                    users C 
                ON 
                    T.creator_id = C.id
                INNER JOIN 
                    memberships M 
                ON 
                    M.member_id = C.id

            WHERE M.list_id = $1 AND T.type = $2 )
        UNION
        -- Gets tweets retweeted by members.
         (  SELECT
              T.id,
              T.tweet_text,
              T.image_url,
              T.created_at,
              C.name,
              C.username,
              C.avatar_url,
              U.name        AS "retweeter_name",
              U.username    AS "retweeter_username",
              R.created_at  AS "creation"

            FROM    tweets T 
                INNER JOIN 
                    retweets R 
                ON 
                    T.id = R.tweet_id
                INNER JOIN 
                    users C 
                ON 
                    T.creator_id = C.id
                INNER JOIN 
                    memberships M 
                ON 
                    R.retweeter_id = M.member_id
                INNER JOIN 
                    users U 
                ON 
                    U.id = M.member_id
                    
            WHERE M.list_id = $1 AND T.type = $2 ) ) AS feeds
  ORDER BY creation DESC;
  RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION get_list(list_id INTEGER)
  RETURNS SETOF lists AS $$
BEGIN
  RETURN QUERY
  SELECT * 
  FROM lists 
  WHERE id = $1
  LIMIT 1;
END; $$
LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION is_owner_of_list(session VARCHAR, list_id INTEGER)
  RETURNS BOOLEAN AS $$
DECLARE userID INTEGER := get_user_id_from_session($1);
        isOwner BOOLEAN;
BEGIN
    -- Finds a list with the given id and its creator is userID.
    PERFORM id
    FROM lists
    WHERE id = $2 AND creator_id = userID;

    IF FOUND THEN
      RETURN TRUE;
    ELSE 
      RETURN FALSE;
    END IF;

END;  $$
LANGUAGE PLPGSQL;