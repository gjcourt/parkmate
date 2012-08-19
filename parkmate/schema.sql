CREATE TABLE user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(20) NOT NULL,
    email VARCHAR(40) NOT NULL,
    pass CHAR(40) NOT NULL,
    first VARCHAR(15) NOT NULL,
    last VARCHAR(20) NOT NULL,
    flags INTEGER,
    admin BOOLEAN,
    unique(email)
);

CREATE TABLE reminder (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    target DATE, -- date that the reminder is for
    trigger DATE, -- date to trigger the the reminder
    flags INTEGER, -- has it been triggered, etc
    media INTEGER, -- email, phone, sms, etc. (default is email)
    user_id INTEGER,
    FOREIGN KEY(user_id) REFERENCES user(id)
);
