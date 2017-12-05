DROP TABLE IF EXISTS id_repository;

DROP TABLE IF EXISTS simpleinfo;

DROP TABLE IF EXISTS execreports;

DROP TABLE IF EXISTS reports;

DROP TABLE IF EXISTS db_users;

CREATE TABLE db_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lastUpdated DATETIME,
    updateCount INTEGER NOT NULL,
    description VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    active INTEGER NOT NULL,
    hashedPassword VARCHAR(255) NOT NULL,
    superuser INTEGER NOT NULL,
    userdata longtext,
    PRIMARY KEY (id),
    UNIQUE (name)
);

CREATE TABLE reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lastUpdated DATETIME,
    updateCount INTEGER NOT NULL,
    report_id BIGINT NOT NULL,
    fixMessage longtext NOT NULL,
    originator INTEGER,
    reportType INTEGER NOT NULL,
    sendingTime DATETIME NOT NULL,
    orderID VARCHAR(255) NOT NULL,
    actor_id BIGINT,
	brokerID VARCHAR(255),
    PRIMARY KEY (id),
    INDEX idx_sendingTime (sendingTime),
    INDEX idx_orderID (orderID),
    INDEX idx_actor_id (actor_id),
    CONSTRAINT fk_reports_actor_id FOREIGN KEY (actor_id)
     REFERENCES db_users(id)
);

CREATE TABLE execreports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lastUpdated DATETIME,
    updateCount INTEGER NOT NULL,
    avgPrice NUMERIC(17,7) NOT NULL,
    cumQuantity NUMERIC(17,7) NOT NULL,
    lastPrice NUMERIC(17,7),
    lastQuantity NUMERIC(17,7),
    orderID VARCHAR(255) NOT NULL,
    actor_id BIGINT,
    orderStatus INTEGER NOT NULL,
    origOrderID VARCHAR(255),
    rootID VARCHAR(255) NOT NULL,
    sendingTime DATETIME NOT NULL,
    side INTEGER NOT NULL,
	brokerID VARCHAR(255),
	fullSymbol VARCHAR(255) NOT NULL,
    securityType VARCHAR(255) NOT NULL,
    isOpen INTEGER,
    account VARCHAR(255),
    report_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_report_id (report_id),
	INDEX idx_brokerID (brokerID),
    INDEX idx_fullSymbol (fullSymbol),
	INDEX idx_securityType (securityType),
    INDEX idx_sendingTime (sendingTime),
    INDEX idx_orderID (orderID),
    INDEX idx_actor_id (actor_id),
    INDEX idx_rootID (rootID),
    CONSTRAINT fk_execreports_actor_id FOREIGN KEY (actor_id)
     REFERENCES db_users(id),
    CONSTRAINT fk_execreports_report_id FOREIGN KEY (report_id)
     REFERENCES reports(id)
);

CREATE TABLE simpleinfo (
	id BIGINT NOT NULL AUTO_INCREMENT,
    lastUpdated DATETIME,
    updateCount INTEGER NOT NULL,
	securityType VARCHAR(255) NOT NULL,
    fullSymbol VARCHAR(255) NOT NULL,
    brokerID VARCHAR(255),
    fullInfo longtext NOT NULL,	
    PRIMARY KEY (id),
	INDEX idx_securityType (securityType),
    INDEX idx_fullSymbol (fullSymbol),
	INDEX idx_brokerID (brokerID),
    UNIQUE (securityType,fullSymbol,brokerID)
);

create table id_repository (
    id bigint not null AUTO_INCREMENT,
    lastUpdated timestamp,
    updateCount integer not null,
    nextID bigint NOT NULL DEFAULT 0,
    primary key (id)
);