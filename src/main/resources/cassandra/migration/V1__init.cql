CREATE TABLE traveler_vpn.vpn_users (
    user_id uuid PRIMARY KEY,
    username text,
    connection_links set<text>,
    plan text,
    status text,
    last_payment_at bigint,
    expiry_at bigint,
    traffic_left bigint,
    up bigint,
    down bigint,
);

CREATE TABLE traveler_vpn.servers_metrics (
    id bigint PRIMARY KEY,
    name text,
    latency_ms bigint,
    location text
);