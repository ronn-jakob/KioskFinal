/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Admin;


import java.sql.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import javax.swing.JOptionPane;

public class CreateOrder {

    private Connection conn;
    private AdminMain am;

    // CART STORAGE
    private final List<CartItem> cartItems = new ArrayList<>();

    public CreateOrder(Connection conn, AdminMain am) {
        this.conn = conn;
        this.am = am;
    }

    // ==============================
    // 🛒 ADD ITEM TO CART
    // ==============================
    public void addToCart(int itemId, String itemName, double price) {
        for (CartItem item : cartItems) {
            if (item.getItemId() == itemId) {
                item.setQuantity(item.getQuantity() + 1);
                refreshTable();
                return;
            }
        }

        cartItems.add(new CartItem(itemId, itemName, price, 1));
        refreshTable();
    }


    // ==============================
    // ❌ REMOVE ITEM
    // ==============================
    public void removeCartItem() {
        int row = am.tableCO.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(am, "Select item first.");
            return;
        }

        cartItems.remove(row);
        refreshTable();
    }

    // ==============================
    // 🔄 REFRESH TABLE
    // ==============================
    public void refreshTable() {
        DefaultTableModel model = (DefaultTableModel) am.tableCO.getModel();
        model.setRowCount(0);

        for (CartItem item : cartItems) {
            model.addRow(new Object[]{
                item.getItemName(),
                item.getQuantity(),
                item.getPrice(),
                item.getTotal()
            });
        }

        updateTotalDisplay();
    }

    public double getDiscountRate() {
        String discount = (String) am.discountCOComboBox.getSelectedItem();
        if ("Senior Discount".equals(discount) || "PWD".equals(discount)) {
            return 0.20;
        } else {
            return 0.0;
        }
    }

    public void updateTotalDisplay() {
        double total = 0;

        for (CartItem item : cartItems) {
            total += item.getTotal();
        }

        double discount = total * getDiscountRate();
        double finalTotal = total - discount;

        am.totalAmountLabel.setText(String.format("₱%.2f", finalTotal));
    }

    public void confirmOrder() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(am, "Cart is empty.");
            return;
        }

        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotal();
        }

        double discountRate = getDiscountRate();
        double discountAmount = total * discountRate;
        double finalTotal = total - discountAmount;

        // PAYMENT
        String paymentStr = am.paymentCOTextField.getText().trim();
        if (paymentStr.isEmpty()) {
            JOptionPane.showMessageDialog(am, "Enter payment.");
            return;
        }

        double payment;
        try {
            payment = Double.parseDouble(paymentStr);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(am, "Invalid payment.");
            return;
        }

        if (payment < finalTotal) {
            JOptionPane.showMessageDialog(am, "Insufficient payment.");
            return;
        }

        double change = payment - finalTotal;

        int confirm = JOptionPane.showConfirmDialog(am,
                String.format("Total: ₱%.2f\nPayment: ₱%.2f\nChange: ₱%.2f\n\nConfirm?",
                        finalTotal, payment, change));

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            conn.setAutoCommit(false);

            // INSERT ORDER
            String orderSql = "INSERT INTO orders(orderNumber, total, discountRate, discountAmount, finalTotal, orderStatus) VALUES(?,?,?,?,?,?)";
            PreparedStatement psOrder = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);

            int orderNumber = generateOrderNumber();

            psOrder.setInt(1, orderNumber);
            psOrder.setDouble(2, total);
            psOrder.setDouble(3, discountRate);
            psOrder.setDouble(4, discountAmount);
            psOrder.setDouble(5, finalTotal);
            psOrder.setString(6, "completed");

            psOrder.executeUpdate();

            ResultSet rs = psOrder.getGeneratedKeys();
            int orderId = 0;
            if (rs.next()) orderId = rs.getInt(1);

            // INSERT ORDER ITEMS
            String itemSql = "INSERT INTO order_items(orderId, menuItemId, quantity, pricePerItem, itemTotal) VALUES(?,?,?,?,?)";
            PreparedStatement psItem = conn.prepareStatement(itemSql);

            for (CartItem item : cartItems) {
                psItem.setInt(1, orderId);
                psItem.setInt(2, item.getItemId());
                psItem.setInt(3, item.getQuantity());
                psItem.setDouble(4, item.getPrice());
                psItem.setDouble(5, item.getTotal());
                psItem.addBatch();
            }

            psItem.executeBatch();

            // INSERT PAYMENT
            String paySql = "INSERT INTO payments(orderId, amountReceived, changeAmount) VALUES(?,?,?)";
            PreparedStatement psPay = conn.prepareStatement(paySql);

            psPay.setInt(1, orderId);
            psPay.setDouble(2, payment);
            psPay.setDouble(3, change);
            psPay.executeUpdate();

            conn.commit();

            JOptionPane.showMessageDialog(am, "Order Completed!");

            clearCart();

        } catch (SQLException ex) {
            try { conn.rollback(); } catch (Exception e) {}
            JOptionPane.showMessageDialog(am, "Error: " + ex.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch (Exception e) {}
        }
    }

    private int generateOrderNumber() {
        return (int) (System.currentTimeMillis() % 1000000);
    }


    public void clearCart() {
        cartItems.clear();
        refreshTable();
        am.paymentCOTextField.setText("");
    }

    class CartItem {
        private int itemId;
        private String itemName;
        private double price;
        private int quantity;

        public CartItem(int itemId, String itemName, double price, int quantity) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.price = price;
            this.quantity = quantity;
        }

        public int getItemId() { return itemId; }
        public String getItemName() { return itemName; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public double getTotal() {
            return price * quantity;
        }
    }
}
