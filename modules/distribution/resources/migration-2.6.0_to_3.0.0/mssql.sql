CREATE TABLE AM_SYSTEM_APPS (
            ID INTEGER IDENTITY,
            NAME VARCHAR(50) NOT NULL,
            CONSUMER_KEY VARCHAR(512) NOT NULL,
            CONSUMER_SECRET VARCHAR(512) NOT NULL,
            CREATED_TIME DATETIME2(6) DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (ID)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[AM_API_CLIENT_CERTIFICATE]') AND TYPE IN (N'U'))
CREATE TABLE AM_API_CLIENT_CERTIFICATE (
    TENANT_ID INTEGER NOT NULL,
    ALIAS VARCHAR(45) NOT NULL,
    API_ID INTEGER NOT NULL,
    CERTIFICATE VARBINARY(MAX) NOT NULL,
    REMOVED BIT NOT NULL DEFAULT 0,
    TIER_NAME VARCHAR(512),
    PRIMARY KEY (ALIAS, TENANT_ID, REMOVED),
    FOREIGN KEY (API_ID) REFERENCES AM_API(API_ID) ON DELETE CASCADE
);

ALTER TABLE AM_POLICY_SUBSCRIPTION ADD
 MONETIZATION_PLAN VARCHAR(25) NULL DEFAULT NULL,
 FIXED_RATE VARCHAR(15) NULL DEFAULT NULL, 
 BILLING_CYCLE VARCHAR(15) NULL DEFAULT NULL, 
 PRICE_PER_REQUEST VARCHAR(15) NULL DEFAULT NULL, 
 CURRENCY VARCHAR(15) NULL DEFAULT NULL
;

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[AM_MONETIZATION_USAGE_PUBLISHER]') AND TYPE IN (N'U'))

CREATE TABLE AM_MONETIZATION_USAGE_PUBLISHER (
	ID VARCHAR(100) NOT NULL,
	STATE VARCHAR(50) NOT NULL,
	STATUS VARCHAR(50) NOT NULL,
	STARTED_TIME VARCHAR(50) NOT NULL,
	PUBLISHED_TIME VARCHAR(50) NOT NULL,
	PRIMARY KEY(ID)
);