-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION create_dm(session VARCHAR, reciever_id INTEGER, dm_text VARCHAR(140),image_url VARCHAR(100) DEFAULT NULL)
RETURNS BOOLEAN AS $$  --Delimiter for functions and strings
DECLARE conv_id   INTEGER;
        userID    INTEGER := get_user_id_from_session($1);
BEGIN

    -- Checks if reciever is following sender.
    PERFORM user_id
    FROM followships 
    WHERE user_id = userID AND follower_of_user_id = $2 AND confirmed = TRUE;

    IF FOUND THEN

        -- Checks if there are previous conversations or not.
        SELECT id
        INTO conv_id
        FROM conversations 
        WHERE (user_id = userID AND user2_id = $2) OR (user_id = $2 AND user2_id = userID);
        
        IF NOT FOUND THEN
            INSERT INTO conversations (user_id, user2_id) 
            VALUES (userID, $2)
            RETURNING id
            INTO conv_id;
        END IF;

        INSERT INTO direct_messages (sender_id, reciever_id, dm_text, image_url, conv_id, created_at)
        VALUES (userID, reciever_id, dm_text, image_url, conv_id, now()::TIMESTAMP);

        RETURN TRUE;
    ELSE
        RETURN FALSE;
    END IF;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION create_dm2(session VARCHAR, conv_id INTEGER, dm_text VARCHAR(140),
 image_url VARCHAR(100) DEFAULT NULL)
RETURNS BOOLEAN AS $$  --Delimiter for functions and strings
DECLARE followers  INTEGER;
        userID     INTEGER := get_user_id_from_session($1);
        receiverID INTEGER;

BEGIN

    -- Checks if userID was considered sender or receiver in previous conversation.
    SELECT 
    INTO receiverID
         CASE WHEN user_id = userID
         THEN user2_id
         ELSE user_id END
    FROM conversations
    WHERE id = $2;

    -- Checks if reciever is following sender.
    PERFORM user_id
    FROM followships 
    WHERE user_id = userID AND follower_of_user_id = receiverID AND confirmed = TRUE;

    IF FOUND THEN
        INSERT INTO direct_messages (sender_id, reciever_id, dm_text, image_url, conv_id, created_at)
        VALUES (userID, receiverID, dm_text, image_url, conv_id, now()::timestamp);
        RETURN TRUE;
    ELSE
        RETURN FALSE;
    END IF;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
    RETURNS VOID AS $$
DECLARE userID INTEGER := get_user_id_from_session($1);
        senderID INTEGER DEFAULT NULL;
CREATE OR REPLACE FUNCTION delete_dm(session VARCHAR, dm_id INTEGER)
BEGIN

    -- Finds the sender's id of the message.
    SELECT sender_id
    INTO senderID
    FROM direct_messages
    WHERE id = $2;

    -- Checks if userID not equal the senderID.
    IF userID = senderID THEN
        DELETE FROM direct_messages D
        WHERE D.id = $2;
    ELSE
        RAISE EXCEPTION 'Only The Message Sender Can Delete The Message';
    END IF;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION get_conversation(conv_id INTEGER)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur'; 
        U.avatar_url,
BEGIN
    OPEN cursor FOR
    SELECT
        U.id,
        U.name,
        U.username,
        X.id,
        X.name,
        X.username,
        D.dm_text,
        D.image_url,
        D.created_at,
        X.avatar_url

    FROM    conversations C 
        INNER JOIN 
            direct_messages D 
        ON 
            C.id = D.conv_id
        INNER JOIN 
            users U 
        ON 
            D.sender_id = U.id
        INNER JOIN
            users X
        ON 
            D.reciever_id = X.id

    WHERE C.id = $1
    ORDER BY D.created_at ASC;
    RETURN cursor;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
-- TODO add latest tweet
CREATE OR REPLACE FUNCTION get_conversations(session VARCHAR)
    RETURNS REFCURSOR AS $$
DECLARE cursor REFCURSOR := 'cur';
        userID INTEGER := get_user_id_from_session($1);
BEGIN

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

-- No need for it can use create_dm directly instead.
CREATE OR REPLACE FUNCTION create_conversation(session VARCHAR, username2 VARCHAR, dm_text VARCHAR(140))
    RETURNS BOOLEAN AS $$
DECLARE userID  INTEGER := get_user_id_from_session($1);
        userID2 INTEGER := get_user_id_from_username($2);
        conv_id INTEGER;
BEGIN

    SELECT id
    INTO conv_id
    FROM conversations
    WHERE user_id = userID AND user2_id = userID2;

    IF FOUND THEN
        RETURN FALSE;
    ELSE
        INSERT INTO conversations VALUES (DEFAULT, userID, userID2)
        ON CONFLICT (user_id, user2_id)
        DO NOTHING;

        PERFORM create_dm($1, userID2, $3);
    END IF;
    RETURN TRUE;

END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION delete_conversation(session VARCHAR, conv_id INTEGER)
    RETURNS VOID AS $$
DECLARE userID INTEGER := get_user_id_from_session($1);
        senderID INTEGER;
        receiverID INTEGER;
BEGIN

    -- Finds the sender and receiver ids of the conversation.
    SELECT user_id, user2_id
    INTO senderID, receiverID
    FROM conversations
    WHERE id = conv_id;

    IF userID <> senderID AND userID <> receiverID THEN
        RAISE EXCEPTION 'Cannot Delete A Conversation You Aren''t A Part Of';
    ELSE
        DELETE FROM conversations
        WHERE id = $2;
    END IF;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION mark_all_read(session VARCHAR)
    RETURNS VOID AS $$
DECLARE userID INTEGER := get_user_id_from_session($1);
BEGIN

    UPDATE direct_messages
    SET read = TRUE
    WHERE reciever_id = userID;

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
