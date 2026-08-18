ALTER TABLE traveler_vpn.vpn_users DROP up;
ALTER TABLE traveler_vpn.vpn_users DROP down;
CREATE TABLE servers (
    id BIGINT PRIMARY KEY,
    name TEXT,
    location TEXT
);
DROP TABLE traveler_vpn.servers_metrics;
