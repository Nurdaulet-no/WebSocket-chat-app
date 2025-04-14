create table users(
    id bigserial primary key,
    username varchar(255) not null unique,
    email varchar(255) not null unique,
    userFirstName varchar(255) not null,
    password varchar(255) not null
);

create index idx_users_username on users(username);
create index idx_users_email on users(email);

create table chat_rooms(
    id bigserial primary key,
    name varchar(255),
    type varchar(50) not null,
    created_at timestamp with time zone default current_timestamp,
    last_activity_at timestamp with time zone
);

create table chatroom_participants(
    chatroom_id BIGINT not null,
    user_id BIGINT not null,
    primary key (chatroom_id, user_id),
    foreign key (chatroom_id) references chat_rooms(id) on delete cascade,
    foreign key (user_id) references users(id) on delete cascade
);

create index idx_chatroom_participants_user_id on chatroom_participants(user_id);

create table messages(
    id bigserial primary key,
    content varchar(1000) not null,
    created_at timestamp with time zone default current_timestamp not null,
    sender_id BIGINT not null,
    chatroom_id BIGINT not null,
    foreign key (sender_id) references users(id) on delete set null, -- delete all if user deletes account
    foreign key (chatroom_id) references chat_rooms(id) on delete cascade -- delete message if room closed
);

create index idx_messages_chatroom_id_createdAt on messages(chatroom_id, created_at DESC); -- for fast finding rooms