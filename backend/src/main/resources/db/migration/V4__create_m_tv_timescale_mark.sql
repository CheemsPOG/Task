CREATE TABLE m_tv_timescale_mark (
	id VARCHAR(32) PRIMARY KEY,
	ccypair_cd VARCHAR(6) NOT NULL,
	resolution VARCHAR(8) NOT NULL,
	timescale_mark_at BIGINT NOT NULL,
	color VARCHAR(64) NOT NULL,
	label VARCHAR(8) NOT NULL,
	tooltip VARCHAR(256) NOT NULL
);

-- Same deterministic window as m_tv_mark (MarkSeedWindow):
-- from 1787011200 (2026-08-18) through 1787270400 (2026-08-21)
INSERT INTO m_tv_timescale_mark (id, ccypair_cd, resolution, timescale_mark_at, color, label, tooltip) VALUES
	('tm1', 'USDJPY', '1D', 1787011200, 'rgba(255, 99, 71, 0.2)', 'B', 'Buy event'),
	('tm2', 'USDJPY', '1D', 1787097600, 'rgba(70, 130, 180, 0.3)', 'S', 'Sell event'),
	('tm3', 'USDJPY', '1D', 1787184000, 'green', 'N', 'News note');
