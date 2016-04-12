-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION create_dm(session VARCHAR, conversation INTEGER,
                                     dm_text VARCHAR(140), image_url VARCHAR(100) DEFAULT NULL)
  RETURNS BOOLEAN AS $$  --Delimiter for functions and strings
DECLARE followers INTEGER;
        conv      INTEGER;
        conv_id   INTEGER;
        userID    INTEGER;
BEGIN
  SELECT user_id
  INTO userID
  FROM sessions
  WHERE id = $1;

  SELECT count(*)
  INTO followers
  FROM followships F
  WHERE F.user_id = userID AND F.follower_of_user_id = $2 AND F.confirmed = TRUE;

  SELECT count(*)
  INTO conv
  FROM conversations C
  WHERE (C.user_id = userID AND C.user2_id = $2) OR (C.user_id = $2 AND C.user2_id = $1);

  IF followers > 0
  THEN
    IF conv = 0
    THEN
      INSERT INTO conversations (user_id, user2_id) VALUES (userID, $2);

      SELECT C.id
      INTO conv_id
      FROM conversations C
      WHERE C.user_id = userID AND C.user2_id = $2
      LIMIT 1;
    ELSE
      SELECT C.id
      INTO conv_id
      FROM conversations C
      WHERE (C.user_id = userID AND C.user2_id = $2) OR (C.user_id = $2 AND C.user2_id = userID)
      LIMIT 1;
    END IF;

    INSERT INTO direct_messages (sender_id, reciever_id, dm_text, image_url, conv_id, created_at)
    VALUES (sender_id, reciever_id, dm_text, image_url, conv_id, now()::timestamp);

    RETURN TRUE;
  ELSE
    RETURN FALSE;
  END IF;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION delete_dm(dm_id INTEGER)
  RETURNS VOID AS $$
BEGIN
  DELETE FROM direct_messages D
  WHERE D.id = $1;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_conversation(conv_id INTEGER)
  RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur'; --Points to query rows
BEGIN
  OPEN cursor FOR
  SELECT
    U.id,
    U.name,
    X.id,
    X.name,
    D.dm_text,
    D.image_url,
    D.created_at,
    U.avatar_url,
    X.avatar_url
  FROM conversations C INNER JOIN direct_messages D ON C.id = D.conv_id
    INNER JOIN users U ON D.sender_id = U.id
    INNER JOIN users X ON D.reciever_id = X.id
  WHERE C.id = $1;
  RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
-- TODO add latest tweet
CREATE OR REPLACE FUNCTION get_conversations(session VARCHAR)
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
    temp.id,
    temp.sender_id,
    U.name,
    temp.reciever_id,
    X.name,
    temp.dm_text,
    temp.created_at,
    U.username,
    X.username,
    U.avatar_url,
    X.avatar_url
  FROM (SELECT DISTINCT ON (C.id)
          C.id,
          D.dm_text,
          D.sender_id,
          D.reciever_id,
          (SELECT max(created_at)
           FROM direct_messages
           WHERE conv_id = C.id) AS created_at
        FROM conversations C INNER JOIN direct_messages D ON C.id = D.conv_id
        ORDER BY C.id, D.created_at DESC) temp INNER JOIN users U ON U.id = temp.sender_id
    INNER JOIN users X ON X.id = temp.reciever_id
  WHERE sender_id = userID OR reciever_id = userID;
  RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION delete_conversation(conv_id INTEGER)
  RETURNS VOID AS $$
BEGIN
  DELETE FROM conversations
  WHERE id = $1;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION mark_all_read()
  RETURNS VOID AS $$
BEGIN
  UPDATE direct_messages
  SET read = TRUE; --All users??
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION mark_read(conversation_id INTEGER)
  RETURNS VOID AS $$
BEGIN
  UPDATE direct_messages
  SET read = TRUE
  WHERE conv_id = $1;
END; $$
LANGUAGE PLPGSQL;
