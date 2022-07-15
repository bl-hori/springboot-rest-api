DROP TABLE IF EXISTS tutorials;

CREATE TABLE tutorials
(
   id INT NOT NULL AUTO_INCREMENT,
   description VARCHAR(255) NOT NULL,
   published bit(1) NOT NULL,
   title VARCHAR(255) NOT NULL,
   PRIMARY KEY(id)
);
