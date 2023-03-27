
CREATE TABLE Users(
    username varchar(20) PRIMARY KEY,
    password varbinary(20),
    salt varbinary(20),
    balance int)
  

CREATE TABLE Reservations(
    rid int PRIMARY KEY,
    fid1 int,
    fid2 int,
    userid varchar(20),
    paid int,
    cancelled int,
    FOREIGN KEY(fid1) REFERENCES FLIGHTS(fid),
    FOREIGN KEY(fid2) REFERENCES FLIGHTS(fid),
    FOREIGN KEY(userid) REFERENCES Users(username));