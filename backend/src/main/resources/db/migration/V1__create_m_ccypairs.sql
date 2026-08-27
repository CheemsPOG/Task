-- Currency-pair master (docs 123, 124, 127).
-- ccypair_cd is 6 letters (USDJPY). priority is also GET /curpairs.curpairCd.
-- is_deleted=0 means tradable. rate_unit → pricescale = 10^rate_unit.

CREATE TABLE m_ccypairs (
	ccypair_cd VARCHAR(6) PRIMARY KEY,
	ccypair_jp VARCHAR(64) NOT NULL,
	rate_unit INTEGER NOT NULL,
	is_deleted INTEGER NOT NULL DEFAULT 0,
	priority INTEGER NOT NULL
);

INSERT INTO m_ccypairs (ccypair_cd, ccypair_jp, rate_unit, is_deleted, priority) VALUES
	('USDJPY', '米ドル/円', 3, 0, 1),
	('EURJPY', 'ユーロ/円', 3, 0, 2),
	('EURUSD', 'ユーロ/米ドル', 5, 0, 3),
	('GBPUSD', '英ポンド/米ドル', 5, 0, 4),
	('AUDUSD', '豪ドル/米ドル', 5, 0, 5);
