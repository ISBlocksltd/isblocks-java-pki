-- New columns in CertificateData are added by the JPA provider if there are sufficient privileges
-- if not added automatically the following SQL statements can be run to add the new columns 
-- ALTER TABLE CertificateData ADD notBefore NUMBER(19);
-- ALTER TABLE CertificateData ADD endEntityProfileId NUMBER(10);
-- ALTER TABLE CertificateData ADD subjectAltName VARCHAR2(2000 byte);
--
-- Table ProfileData is new and is added by the JPA provider if there are sufficient privileges. 
-- See create-tables-database.sql
--
-- subjectDN and subjectAltName columns in UserData has been extended to accommodate longer names
-- subjectDN from 250 to 400 characters and subjectAltName from 250 to 2000 characters
-- ALTER TABLE UserData MODIFY subjectAltName VARCHAR2(2000 byte);
-- ALTER TABLE UserData MODIFY subjectDN VARCHAR2(400 byte);
-- ALTER TABLE CertificateData MODIFY subjectDN VARCHAR2(400 byte);
