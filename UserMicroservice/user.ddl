DROP TABLE IF EXISTS Users;
DROP TABLE IF EXISTS Coupons;

CREATE TABLE User(
	uid SERIAL NOT NULL PRIMARY KEY,
	email varchar NOT NULL UNIQUE,
	password varchar NOT NULL,
	prefer_name varchar,
	rides NUMERIC NOT NULL,
	isDriver bool NOT NULL,
	availableCoupons integer[] NOT NULL,
    redeemedCoupons integer[] NOT NULL
);

CREATE TABLE Coupons(
    cid SERIAL NOT NULL PRIMARY KEY,
    "name" varchar NOT NULL,
    description varchar NOT NULL,
	discount numeric NOT NULL,
    expiry date NOT NULL
);


INSERT INTO TABLE Coupons VALUES(1,"Ride one Get $10 off","This coupon is added after you take your first ride from Zoomer!",10,"2099-12-31");
INSERT INTO TABLE Coupons VALUES(2,"$5 off","This coupon is only avaliable when you ride is greater than $10",5,"2099-12-31");