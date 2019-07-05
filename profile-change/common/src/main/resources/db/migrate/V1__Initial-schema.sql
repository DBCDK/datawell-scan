
CREATE TABLE profiles (
    agencyId INT NOT NULL,
    classifier TEXT NOT NULL,
    collectionIdentifier TEXT NOT NULL,
    CONSTRAINT profiles_pk PRIMARY KEY (agencyId, classifier, collectionIdentifier)
);
