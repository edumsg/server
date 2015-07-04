-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION create_dm(sender_id integer, reciever_id integer,
  dm_text varchar(140), created_at timestamp, image_url varchar(100) DEFAULT null)
RETURNS boolean AS $$
DECLARE followers integer;
DECLARE conv integer;
DECLARE conv_id integer;
  BEGIN
    SELECT count(*) INTO followers FROM followships F
    WHERE F.user_id = $1 AND F.follower_id = $2 AND F.confirmed = TRUE;

    SELECT count(*) INTO conv FROM conversations C
    WHERE (C.user_id = $1 AND C.user2_id = $2) OR (C.user_id = $2 AND C.user2_id = $1);

    IF followers > 0 THEN
      IF conv = 0 THEN
        INSERT INTO conversations(user_id, user2_id) VALUES ($1, $2);
        
        SELECT C.id INTO conv_id FROM conversations C
        WHERE C.user_id = $1 AND C.user2_id = $2 LIMIT 1;
      ELSE
        SELECT C.id INTO conv_id FROM conversations C
        WHERE (C.user_id = $1 AND C.user2_id = $2) OR (C.user_id = $2 AND C.user2_id = $1) LIMIT 1;
      END IF;

      INSERT INTO direct_messages(sender_id, reciever_id, dm_text, image_url, conv_id, created_at)
      VALUES (sender_id, reciever_id, dm_text, image_url, conv_id, created_at);
      
      RETURN TRUE;
    ELSE
      RETURN FALSE;
    END IF;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION delete_dm(dm_id integer)
RETURNS void AS $$
  BEGIN
    DELETE FROM direct_messages D WHERE D.id = $1;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_conversation(conv_id integer)
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT U.id, U.name, X.id, X.name, D.dm_text, D.image_url, D.created_at
    FROM conversations C INNER JOIN direct_messages D ON C.id = D.conv_id
      INNER JOIN users U ON D.sender_id = U.id INNER JOIN users X ON D.reciever_id = X.id
    WHERE C.id = $1;
    RETURN cursor;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
-- TODO add latest tweet
CREATE OR REPLACE FUNCTION get_conversations(user_id integer)
RETURNS refcursor AS $$
DECLARE cursor refcursor := 'cur';
  BEGIN
    OPEN cursor FOR
    SELECT temp.id, temp.sender_id, U.name, temp.reciever_id, X.name, temp.dm_text, temp.created_at
    FROM (SELECT DISTINCT ON (C.id) C.id, D.dm_text, D.sender_id, D.reciever_id,
      (SELECT max(created_at) FROM direct_messages WHERE conv_id = C.id) AS created_at
      FROM conversations C INNER JOIN direct_messages D ON c.id = D.conv_id
      ORDER BY C.id, D.created_at DESC) temp INNER JOIN users U on U.id = temp.sender_id
      INNER JOIN users X on X.id = temp.reciever_id
    WHERE sender_id = $1 OR reciever_id = $1;
    RETURN cursor;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION delete_conversation(conv_id integer)
RETURNS void AS $$
  BEGIN
    DELETE FROM conversations WHERE id = $1;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION mark_all_read()
RETURNS void AS $$
  BEGIN
    UPDATE direct_messages SET read = TRUE;
  END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION mark_read(conversation_id integer)
RETURNS void AS $$
  BEGIN
    UPDATE direct_messages SET read = TRUE WHERE conv_id = $1;
  END; $$
LANGUAGE PLPGSQL;