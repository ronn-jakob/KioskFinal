-- KioskEDP Database Schema
-- Run this script to create or recreate the database schema.

CREATE DATABASE IF NOT EXISTS kioskEDP;
USE kioskEDP;

CREATE TABLE IF NOT EXISTS menu_items (
    itemID   INT          NOT NULL AUTO_INCREMENT,
    itemName VARCHAR(100) NOT NULL,
    price    DECIMAL(10,2) NOT NULL,
    category VARCHAR(50),
    image    VARCHAR(255),
    PRIMARY KEY (itemID)
);

CREATE TABLE IF NOT EXISTS drinks (
    drinkID   INT          NOT NULL AUTO_INCREMENT,
    drinkName VARCHAR(100) NOT NULL,
    price     DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (drinkID)
);

CREATE TABLE IF NOT EXISTS add_ons (
    addonId  INT          NOT NULL AUTO_INCREMENT,
    addonName VARCHAR(100) NOT NULL,
    price    DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (addonId)
);

CREATE TABLE IF NOT EXISTS orders (
    orderId        INT            NOT NULL AUTO_INCREMENT,
    orderNumber    INT            NOT NULL,
    total          DECIMAL(10,2)  NOT NULL DEFAULT 0,
    discountRate   DECIMAL(5,4)   NOT NULL DEFAULT 0,
    discountAmount DECIMAL(10,2)  NOT NULL DEFAULT 0,
    finalTotal     DECIMAL(10,2)  NOT NULL DEFAULT 0,
    orderStatus    ENUM('Pending','Paid') NOT NULL DEFAULT 'Pending',
    PRIMARY KEY (orderId)
);

CREATE TABLE IF NOT EXISTS order_items (
    orderItemId  INT           NOT NULL AUTO_INCREMENT,
    orderId      INT           NOT NULL,
    menuItemId   INT           NOT NULL,
    quantity     INT           NOT NULL DEFAULT 1,
    pricePerItem DECIMAL(10,2) NOT NULL DEFAULT 0,
    itemTotal    DECIMAL(10,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (orderItemId),
    FOREIGN KEY (orderId)    REFERENCES orders(orderId)     ON DELETE CASCADE,
    FOREIGN KEY (menuItemId) REFERENCES menu_items(itemID)  ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS order_item_drinks (
    orderItemId INT NOT NULL,
    drinkId     INT NOT NULL,
    PRIMARY KEY (orderItemId, drinkId),
    FOREIGN KEY (orderItemId) REFERENCES order_items(orderItemId) ON DELETE CASCADE,
    FOREIGN KEY (drinkId)     REFERENCES drinks(drinkID)          ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS order_item_addons (
    orderItemId INT NOT NULL,
    addonId     INT NOT NULL,
    PRIMARY KEY (orderItemId, addonId),
    FOREIGN KEY (orderItemId) REFERENCES order_items(orderItemId) ON DELETE CASCADE,
    FOREIGN KEY (addonId)     REFERENCES add_ons(addonId)         ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payments (
    paymentId      INT           NOT NULL AUTO_INCREMENT,
    orderId        INT           NOT NULL UNIQUE,
    amountReceived DECIMAL(10,2) NOT NULL DEFAULT 0,
    changeAmount   DECIMAL(10,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (paymentId),
    FOREIGN KEY (orderId) REFERENCES orders(orderId) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS users (
    userId   INT          NOT NULL AUTO_INCREMENT,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    PRIMARY KEY (userId)
);
