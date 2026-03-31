/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change am license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit am template
 */
package Admin;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import java.sql.*;

/**
 *
 * @author jakob
 */
public class PayOrder {
    
    Connection conn;
    Statement stmt;
    PreparedStatement ps;
    ResultSet rs;
    
    AdminMain am;
    
    public int currentOrderId = 0;
    public double currentTotal = 0;
    public double currentFinalTotal = 0;
    public boolean currentOrderPaid = false;
    public final List<Integer> orderItemIds = new ArrayList<>();
    
    public PayOrder(Connection conn, AdminMain am) {
        this.conn = conn;
        this.am = am;
    }

    
    public double getDiscountRate() {
        String discount = (String) am.discountComboBox.getSelectedItem();
        return ("Senior Discount".equals(discount) || "PWD".equals(discount)) ? 0.20 : 0.0;
    }

    public void loadOrder() {
        String orderNumStr = am.orderNumTextField.getText().trim();
        if (orderNumStr.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(am, "Please enter an order number.");
            return;
        }
        int orderNum;
        try {
            orderNum = Integer.parseInt(orderNumStr);
        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(am, "Order number must be a whole number.");
            return;
        }

        String sql = "SELECT orderId, total, discountRate, finalTotal, orderStatus FROM orders WHERE orderNumber = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderNum);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    javax.swing.JOptionPane.showMessageDialog(am, "Order #" + orderNum + " not found.");
                    return;
                }
                currentOrderId   = rs.getInt("orderId");
                currentTotal     = rs.getDouble("total");
                String status    = rs.getString("orderStatus");
                currentOrderPaid = "completed".equalsIgnoreCase(status);

                if (currentOrderPaid) {
                    javax.swing.JOptionPane.showMessageDialog(am,
                        "Order #" + orderNum + " has already been paid.");
                    return;
                }
            }
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(am, "Database error: " + ex.getMessage());
            return;
        }

        loadOrderItems();
        applyDiscount();
    }

    public void loadOrderItems() {
        String sql =
            "SELECT oi.orderItemId, mi.itemName, d.drinkName, " +
            "GROUP_CONCAT(a.addonName ORDER BY a.addonId SEPARATOR ', ') AS addons, " +
            "oi.quantity, oi.itemTotal " +
            "FROM order_items oi " +
            "JOIN menu_items mi ON mi.itemID = oi.menuItemId " +
            "LEFT JOIN order_item_drinks oid ON oid.orderItemId = oi.orderItemId " +
            "LEFT JOIN drinks d ON d.drinkID = oid.drinkId " +
            "LEFT JOIN order_item_addons oia ON oia.orderItemId = oi.orderItemId " +
            "LEFT JOIN add_ons a ON a.addonId = oia.addonId " +
            "WHERE oi.orderId = ? " +
            "GROUP BY oi.orderItemId, mi.itemName, d.drinkName, oi.quantity, oi.itemTotal";

        DefaultTableModel model = (DefaultTableModel) am.orderTable.getModel();
        model.setRowCount(0);
        orderItemIds.clear();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orderItemIds.add(rs.getInt("orderItemId"));
                    String drinks = rs.getString("drinkName");
                    String addons = rs.getString("addons");
                    model.addRow(new Object[]{
                        rs.getString("itemName"),
                        drinks != null ? drinks : "None",
                        addons != null ? addons : "None",
                        rs.getInt("quantity"),
                        rs.getDouble("itemTotal")
                    });
                }
            }
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(am, "Error loading order items: " + ex.getMessage());
        }
    }

    public void applyDiscount() {
        double discountRate   = getDiscountRate();
        double discountAmount = currentTotal * discountRate;
        currentFinalTotal     = currentTotal - discountAmount;
        am.totalPriceLabel.setText(String.format("\u20b1%.2f", currentFinalTotal));
    }

    public void calculateChange() {
        if (currentOrderId == 0) {
            javax.swing.JOptionPane.showMessageDialog(am, "Please load an order first.");
            return;
        }
        String paymentStr = am.paymentTextField.getText().trim();
        if (paymentStr.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(am, "Please enter a payment amount.");
            return;
        }
        double payment;
        try {
            payment = Double.parseDouble(paymentStr);
        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(am, "Invalid payment amount.");
            return;
        }
        if (payment < currentFinalTotal) {
            javax.swing.JOptionPane.showMessageDialog(am,
                String.format("Payment is insufficient.\nTotal due: \u20b1%.2f", currentFinalTotal));
            return;
        }
        double change = payment - currentFinalTotal;
        int confirm = javax.swing.JOptionPane.showConfirmDialog(am,
            String.format("Total:    \u20b1%.2f%nPayment:  \u20b1%.2f%nChange:   \u20b1%.2f%n%nConfirm payment?",
                currentFinalTotal, payment, change),
            "Confirm Payment", javax.swing.JOptionPane.YES_NO_OPTION);
        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            processPayment(payment, change);
        }
    }

    public void processPayment(double payment, double change) {
        double discountRate   = getDiscountRate();
        double discountAmount = currentTotal * discountRate;
        try {
            conn.setAutoCommit(false);

            String updateOrder = "UPDATE orders SET discountRate=?, discountAmount=?, finalTotal=?, orderStatus='completed' WHERE orderId=?";
            try (PreparedStatement ps = conn.prepareStatement(updateOrder)) {
                ps.setDouble(1, discountRate);
                ps.setDouble(2, discountAmount);
                ps.setDouble(3, currentFinalTotal);
                ps.setInt(4, currentOrderId);
                ps.executeUpdate();
            }

            String upsertPayment =
                "INSERT INTO payments(orderId, amountReceived, changeAmount) VALUES(?,?,?) " +
                "ON DUPLICATE KEY UPDATE amountReceived=VALUES(amountReceived), changeAmount=VALUES(changeAmount)";
            try (PreparedStatement ps = conn.prepareStatement(upsertPayment)) {
                ps.setInt(1, currentOrderId);
                ps.setDouble(2, payment);
                ps.setDouble(3, change);
                ps.executeUpdate();
            }

            conn.commit();
            javax.swing.JOptionPane.showMessageDialog(am,
                String.format("Payment processed!\nChange: \u20b1%.2f", change));
            clearPayOrderForm();
        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException rollbackEx) {

            }
            javax.swing.JOptionPane.showMessageDialog(am, "Error processing payment: " + ex.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException autoCommitEx) {

            }
        }
    }

    public void addOrderItem() {
        if (currentOrderId == 0) {
            javax.swing.JOptionPane.showMessageDialog(am, "Please load an order first.");
            return;
        }

        List<String> names  = new ArrayList<>();
        List<Integer> ids   = new ArrayList<>();
        List<Double> prices = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT itemID, itemName, price FROM menu_items ORDER BY itemName");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt("itemID"));
                names.add(rs.getString("itemName"));
                prices.add(rs.getDouble("price"));
            }
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(am, "Error loading menu: " + ex.getMessage());
            return;
        }

        if (names.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(am, "No menu items available.");
            return;
        }

        javax.swing.JComboBox<String> itemCombo = new javax.swing.JComboBox<>(names.toArray(new String[0]));
        javax.swing.JTextField qtyField = new javax.swing.JTextField("1", 6);
        Object[] message = {"Select Menu Item:", itemCombo, "Quantity:", qtyField};

        int result = javax.swing.JOptionPane.showConfirmDialog(am, message,
            "Add Item to Order", javax.swing.JOptionPane.OK_CANCEL_OPTION);
        if (result != javax.swing.JOptionPane.OK_OPTION) return;

        int selectedIdx = itemCombo.getSelectedIndex();
        int menuItemId  = ids.get(selectedIdx);
        double price    = prices.get(selectedIdx);
        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(am, "Please enter a valid quantity (positive integer).");
            return;
        }
        double itemTotal = price * qty;

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO order_items(orderId, menuItemId, quantity, pricePerItem, itemTotal) VALUES(?,?,?,?,?)")) {
                ps.setInt(1, currentOrderId);
                ps.setInt(2, menuItemId);
                ps.setInt(3, qty);
                ps.setDouble(4, price);
                ps.setDouble(5, itemTotal);
                ps.executeUpdate();
            }

            currentTotal = recalcOrderTotal();
            conn.commit();

            loadOrderItems();
            applyDiscount();
        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException rollbackEx) {

            }
            javax.swing.JOptionPane.showMessageDialog(am, "Error adding item: " + ex.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException autoCommitEx) {

            }
        }
    }

    public void editOrderItem() {
        int selectedRow = am.orderTable.getSelectedRow();
        if (selectedRow == -1) {
            javax.swing.JOptionPane.showMessageDialog(am, "Please select an item to edit.");
            return;
        }

        int orderItemId  = orderItemIds.get(selectedRow);
        int currentQty   = (Integer) am.orderTable.getValueAt(selectedRow, 3);
        double rowTotal  = (Double) am.orderTable.getValueAt(selectedRow, 4);
        double unitPrice = (currentQty > 0) ? rowTotal / currentQty : rowTotal;

        String input = javax.swing.JOptionPane.showInputDialog(am,
            "Enter new quantity:", String.valueOf(currentQty));
        if (input == null || input.trim().isEmpty()) return;

        int newQty;
        try {
            newQty = Integer.parseInt(input.trim());
            if (newQty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(am, "Please enter a valid quantity (positive integer).");
            return;
        }

        double newItemTotal = unitPrice * newQty;

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE order_items SET quantity=?, itemTotal=? WHERE orderItemId=?")) {
                ps.setInt(1, newQty);
                ps.setDouble(2, newItemTotal);
                ps.setInt(3, orderItemId);
                ps.executeUpdate();
            }

            currentTotal = recalcOrderTotal();
            conn.commit();

            loadOrderItems();
            applyDiscount();
        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException rollbackEx) {
//                logger.log(java.util.logging.Level.SEVERE, "Rollback failed", rollbackEx);
            }
            javax.swing.JOptionPane.showMessageDialog(am, "Error editing item: " + ex.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException autoCommitEx) {
//                logger.log(java.util.logging.Level.SEVERE, "Failed to restore auto-commit", autoCommitEx);
            }
        }
    }

    public void deleteOrderItem() {
        int selectedRow = am.orderTable.getSelectedRow();
        if (selectedRow == -1) {
            javax.swing.JOptionPane.showMessageDialog(am, "Please select an item to delete.");
            return;
        }

        int confirm = javax.swing.JOptionPane.showConfirmDialog(am,
            "Are you sure you want to delete am item?",
            "Confirm Delete", javax.swing.JOptionPane.YES_NO_OPTION);
        if (confirm != javax.swing.JOptionPane.YES_OPTION) return;

        int orderItemId = orderItemIds.get(selectedRow);

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM order_item_addons WHERE orderItemId=?")) {
                ps.setInt(1, orderItemId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM order_item_drinks WHERE orderItemId=?")) {
                ps.setInt(1, orderItemId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM order_items WHERE orderItemId=?")) {
                ps.setInt(1, orderItemId);
                ps.executeUpdate();
            }

            currentTotal = recalcOrderTotal();
            conn.commit();

            loadOrderItems();
            applyDiscount();
        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException rollbackEx) {
//                logger.log(java.util.logging.Level.SEVERE, "Rollback failed", rollbackEx);
            }
            javax.swing.JOptionPane.showMessageDialog(am, "Error deleting item: " + ex.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException autoCommitEx) {
//                logger.log(java.util.logging.Level.SEVERE, "Failed to restore auto-commit", autoCommitEx);
            }
        }
    }


    public double recalcOrderTotal() throws SQLException {
        double newTotal = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(itemTotal), 0) AS total FROM order_items WHERE orderId=?")) {
            ps.setInt(1, currentOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) newTotal = rs.getDouble("total");
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE orders SET total=? WHERE orderId=?")) {
            ps.setDouble(1, newTotal);
            ps.setInt(2, currentOrderId);
            ps.executeUpdate();
        }
        return newTotal;
    }

    public void clearPayOrderForm() {
        currentOrderId    = 0;
        currentTotal      = 0;
        currentFinalTotal = 0;
        currentOrderPaid  = false;
        orderItemIds.clear();
        am.orderNumTextField.setText("");
        am.paymentTextField.setText("");
        am.totalPriceLabel.setText(" ");
        am.discountComboBox.setSelectedIndex(0);
        ((DefaultTableModel) am.orderTable.getModel()).setRowCount(0);
    }

    // ---- end Pay Order panel logic ----
    
    
}
