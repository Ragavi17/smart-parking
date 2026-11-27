CREATE TABLE slot_types (
  id             BIGINT NOT NULL AUTO_INCREMENT,
  name           VARCHAR(30) NOT NULL,
  price_per_hour DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_slot_types_name (name)
) ENGINE = InnoDB;

CREATE TABLE users (
  id            BIGINT NOT NULL AUTO_INCREMENT,
  name          VARCHAR(100) NOT NULL,
  email         VARCHAR(150) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role          VARCHAR(20) NOT NULL,
  created_at    DATETIME NOT NULL,
  updated_at    DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB;

CREATE TABLE slots (
  id           BIGINT NOT NULL AUTO_INCREMENT,
  slot_number  VARCHAR(20) NOT NULL,
  active       TINYINT(1) NOT NULL DEFAULT 1,
  created_at   DATETIME NOT NULL,
  updated_at   DATETIME NOT NULL,
  slot_type_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_slots_slot_number (slot_number),
  CONSTRAINT fk_slots_slot_types
    FOREIGN KEY (slot_type_id) REFERENCES slot_types (id)
) ENGINE = InnoDB;

CREATE TABLE bookings (
  id             BIGINT NOT NULL AUTO_INCREMENT,
  start_time     DATETIME NOT NULL,
  end_time       DATETIME NOT NULL,
  price_per_hour DECIMAL(10,2) NOT NULL,
  total_amount   DECIMAL(10,2) NOT NULL,
  status         VARCHAR(20) NOT NULL,
  created_at     DATETIME NOT NULL,
  updated_at     DATETIME NOT NULL,
  user_id        BIGINT NOT NULL,
  slot_id        BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_bookings_users FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_bookings_slots FOREIGN KEY (slot_id) REFERENCES slots (id),
  CONSTRAINT chk_bookings_time CHECK (end_time > start_time),
  INDEX idx_booking_user (user_id),
  INDEX idx_booking_slot_time (slot_id, start_time, end_time),
  INDEX idx_booking_status_created (status, created_at)
) ENGINE = InnoDB;

CREATE TABLE booking_extensions (
  id           BIGINT NOT NULL AUTO_INCREMENT,
  old_end_time DATETIME NOT NULL,
  new_end_time DATETIME NOT NULL,
  extra_amount DECIMAL(10,2) NOT NULL,
  status       VARCHAR(20) NOT NULL,
  created_at   DATETIME NOT NULL,
  booking_id   BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_booking_extensions_bookings
    FOREIGN KEY (booking_id) REFERENCES bookings (id),
  CONSTRAINT chk_extension_time CHECK (new_end_time > old_end_time),
  INDEX idx_extension_booking (booking_id)
) ENGINE = InnoDB;