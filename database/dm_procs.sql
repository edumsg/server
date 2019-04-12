-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION create_dm(session VARCHAR, reciever_id INTEGER, dm_text VARCHAR(140),image_url VARCHAR(100) DEFAULT NULL)
RETURNS BOOLEAN AS $$  --Delimiter for functions and strings
DECLARE followers INTEGER;
        conv      INTEGER;
        conv_id   INTEGER;
        userID    INTEGER;
BEGIN
    -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    -- Checks if reciever is following sender.
    SELECT count(*)
    INTO followers
    FROM followships F
    WHERE F.user_id = userID AND F.follower_of_user_id = $2 AND F.confirmed = TRUE;

    -- Checks if there are previous conversations or not.
    SELECT count(*)
    INTO conv
    FROM conversations C
    WHERE (C.user_id = userID AND C.user2_id = $2) OR (C.user_id = $2 AND C.user2_id = userID);

    IF followers >= 0
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
        userID     INTEGER;
        receiverID INTEGER;

BEGIN
    -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    -- Checks if userID was considered sender or receiver in previous conversation.
    SELECT INTO receiverID
        CASE WHEN user_id = userID
        THEN user2_id
        ELSE user_id END
    FROM conversations
    WHERE id = $2;

    -- Checks if reciever is following sender.
    SELECT count(*)
    INTO followers
    FROM followships F
    WHERE F.user_id = userID AND F.follower_of_user_id = receiverID AND F.confirmed = TRUE;

    IF followers > 0
    THEN
        INSERT INTO direct_messages (sender_id, reciever_id, dm_text, image_url, conv_id, created_at)
        VALUES (userID, receiverID, dm_text, image_url, conv_id, now()::timestamp);

        RETURN TRUE;
    ELSE
        RETURN FALSE;
    END IF;
END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION delete_dm(session VARCHAR, dm_id INTEGER)
    RETURNS VOID AS $$
DECLARE userID INTEGER;
        senderID INTEGER DEFAULT NULL;
BEGIN
    -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

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
DECLARE cursor REFCURSOR := 'cur'; --Points to query rows
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
        U.avatar_url,
        X.avatar_url
    FROM conversations C INNER JOIN direct_messages D ON C.id = D.conv_id
        INNER JOIN users U ON D.sender_id = U.id
        INNER JOIN users X ON D.reciever_id = X.id
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
        userID INTEGER;
BEGIN

    -- Finds user's id through user's session.
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

CREATE OR REPLACE FUNCTION create_conversation(session VARCHAR, username2 VARCHAR, dm_text VARCHAR(140))
    RETURNS BOOLEAN AS $$
DECLARE userID  INTEGER;
        userID2 INTEGER;
        conv_id INTEGER;
BEGIN

    -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

    -- Finds the receiver's id through its username.
    SELECT id
    INTO userID2
    FROM users
    WHERE username = $2;

    IF NOT FOUND
    THEN
        RETURN FALSE;
    ELSE

        SELECT id
        INTO conv_id
        FROM conversations
        WHERE user_id = userID AND user2_id = userID2;

        IF FOUND
        THEN
            RETURN FALSE;
        ELSE

            INSERT INTO conversations VALUES (DEFAULT, userID, userID2)
            ON CONFLICT (user_id, user2_id)
                DO NOTHING;

        PERFORM create_dm($1, userID2, $3);
        END IF;
        RETURN TRUE;
    END IF;

END; $$
LANGUAGE PLPGSQL;

-- JAVA / JSON DONE
CREATE OR REPLACE FUNCTION delete_conversation(session VARCHAR, conv_id INTEGER)
    RETURNS VOID AS $$
DECLARE userID INTEGER;
        senderID INTEGER;
        receiverID INTEGER;
BEGIN

     -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

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
DECLARE userID INTEGER;
BEGIN

    -- Finds user's id through user's session.
    SELECT user_id
    INTO userID
    FROM sessions
    WHERE id = $1;

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
