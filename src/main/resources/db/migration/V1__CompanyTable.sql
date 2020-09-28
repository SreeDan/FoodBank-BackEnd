CREATE TABLE company (
    Id SERIAL,
    PersonId DECIMAL(65, 0) PRIMARY KEY NOT NULL,
    CompanyName VARCHAR(100) NOT NULL,
    Address jsonb,
    URL VARCHAR(100),
    Email VARCHAR(100),
    Phone VARCHAR(100),
    NeededFood DECIMAL(65, 0) ARRAY,
    AvailableFood DECIMAL(65, 0) ARRAY,
    Class VARCHAR(10),
    Image BYTEA,
    ImageType VARCHAR(100)
);
CREATE TABLE food (
    FoodId DECIMAL(65, 0) PRIMARY KEY NOT NULL,
    FoodName VARCHAR(100)
);
CREATE TABLE requests (
    RequestId SERIAL,
    RequesterId DECIMAL(65, 0),
    ReceiverId DECIMAL(65, 0),
    Food DECIMAL(65, 0) ARRAY,
    DateTime VARCHAR(100),
    Type VARCHAR(8),
    Status VARCHAR(100),
    Show BOOLEAN
);
CREATE TABLE status (
    StatusId SERIAL,
    StatusName VARCHAR(100)
);
CREATE TABLE credentials (
    UserName VARCHAR(100),
    Password VARCHAR(100),
    Id DECIMAL(65, 0)
);
