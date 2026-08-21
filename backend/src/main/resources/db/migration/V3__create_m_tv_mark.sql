CREATE TABLE m_tv_mark (
	id VARCHAR(32) PRIMARY KEY,
	ccypair_cd VARCHAR(6) NOT NULL,
	resolution VARCHAR(8) NOT NULL,
	mark_at BIGINT NOT NULL,
	color VARCHAR(32) NOT NULL,
	label VARCHAR(8) NOT NULL,
	mark_text VARCHAR(256) NOT NULL
);

-- Deterministic seed window (unix seconds UTC) near current chart range:
-- from 1787011200 (2026-08-18) through 1787270400 (2026-08-21)
INSERT INTO m_tv_mark (id, ccypair_cd, resolution, mark_at, color, label, mark_text) VALUES
	('m1', 'USDJPY', '1D', 1787011200, 'green', 'B', 'Buy signal'),
	('m2', 'USDJPY', '1D', 1787097600, 'red', 'S', 'Sell signal'),
	('m3', 'USDJPY', '1D', 1787184000, 'green', 'B', 'Buy signal follow-up');
