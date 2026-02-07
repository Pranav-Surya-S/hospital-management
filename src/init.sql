DROP DATABASE IF EXISTS hospital;
CREATE DATABASE hospital;
USE hospital;

-- Patients (ID like 'PT-001')
CREATE TABLE patients (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(10) NOT NULL
);

-- Doctors (ID like 'DR-001')
CREATE TABLE doctors (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    specialization VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- Appointments (ID like 'APT-001')
CREATE TABLE appointments (
    id VARCHAR(10) PRIMARY KEY,
    patient_id VARCHAR(10) NOT NULL,
    doctor_id VARCHAR(10) NOT NULL,
    appointment_date DATE NOT NULL,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

CREATE TABLE treatments (
    id VARCHAR(10) PRIMARY KEY,
    patient_id VARCHAR(10) NOT NULL,
    doctor_id VARCHAR(10) NOT NULL,
    diagnosis VARCHAR(255),
    prescription TEXT, -- Holds medicines and advice
    visit_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- Users (Admins)
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL
);

-- Default Users
INSERT INTO users (username, password, role) VALUES ('root', 'root123', 'ROOT');
INSERT INTO users (username, password, role) VALUES ('admin', 'admin123', 'ADMIN');

-- Default Data
INSERT INTO doctors (id, name, specialization, password) VALUES ('DR-001', 'Dr. Smith', 'Cardiology', 'doc123');
INSERT INTO doctors (id, name, specialization, password) VALUES ('DR-002', 'Dr. Jones', 'Neurology', 'doc123');
INSERT INTO patients (id, name, age, gender) VALUES ('PT-001', 'John Doe', 30, 'Male');

-- TRIGGER: Prevent deletion of ROOT account at Database Level
DELIMITER //
CREATE TRIGGER prevent_root_deletion
BEFORE DELETE ON users
FOR EACH ROW
BEGIN
    IF OLD.username = 'root' OR OLD.role = 'ROOT' THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'CRITICAL ERROR: The ROOT account cannot be deleted.';
    END IF;
END;
//
DELIMITER ;