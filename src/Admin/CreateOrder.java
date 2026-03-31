/*
 * Handles the Create Order panel logic in the admin panel.
 * Manages an in-memory cart, calculates totals/discounts, and persists the
 * confirmed order to the database.
 */
package Admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class CreateOrder {

    Connection conn;
    AdminMain am;

    // Column indices for tableCreateOrder
    private static final int COL_QUANTITY = 1;
    private static final int COL_PRICE    = 2;
    private static final int COL_TOTAL    = 3;

    /** Order numbers start at this value when the orders table is empty. */
    private static final int STARTING_ORDER_NUMBER = 20000;

    // One entry per row in tableCreateOrder
    private final List<Integer> cartItemIds   = new ArrayList<>();
    private final List<String>  cartItemTypes = new ArrayList<>(); // "menu" | "drink" | "addon"

    private double cartTotal      = 0;
    private double cartFinalTotal = 0;

    public CreateOrder(Connection conn, AdminMain am) {
        this.conn = conn;
        this.am   = am;
    }

    // -----------------------------------------------------------------------
    // Discount helpers
    // -----------------------------------------------------------------------

    public double getDiscountRate() {
        String discount = (String) am.discountCreateOrderComboBox.getSelectedItem();
        return ("Senior Discount".equals(discount) || "PWD".equals(discount)) ? 0.20 : 0.0;
    }

    // -----------------------------------------------------------------------
    // Cart operations
    // -----------------------------------------------------------------------

    /**
     * Adds an item to the cart table. If the same item (same id + type) already
     * exists, its quantity is incremented instead of adding a duplicate row.
     *
     * <p>Drinks and add-ons are accepted only when at least one menu item is
     * already in the cart, because the database requires them to be linked to
     * an {@code order_item}.  If no food item is present yet, the user is
     * informed and the item is not added.
     */
    public void addItemToTable(int itemId, String name, double price, String itemType) {
        // Guard: drinks and add-ons need at least one food item
        if (!"menu".equals(itemType) && !cartItemTypes.contains("menu")) {
            JOptionPane.showMessageDialog(am,
                "Please add a food item first before adding drinks or add-ons.");
            return;
        }

        DefaultTableModel model = (DefaultTableModel) am.tableCreateOrder.getModel();

        // Look for an existing row with the same item
        for (int i = 0; i < model.getRowCount(); i++) {
            if (cartItemIds.get(i) == itemId && cartItemTypes.get(i).equals(itemType)) {
                int qty = (Integer) model.getValueAt(i, COL_QUANTITY);
                qty++;
                model.setValueAt(qty,         i, COL_QUANTITY);
                model.setValueAt(price * qty, i, COL_TOTAL);
                updateTotal();
                return;
            }
        }

        // New item — append row
        model.addRow(new Object[]{name, 1, price, price});
        cartItemIds.add(itemId);
        cartItemTypes.add(itemType);
        updateTotal();
    }

    /** Increases the quantity of the currently selected row by 1. */
    public void increaseQuantity() {
        int selectedRow = am.tableCreateOrder.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(am, "Please select an item.");
            return;
        }
        DefaultTableModel model = (DefaultTableModel) am.tableCreateOrder.getModel();
        int    qty   = (Integer) model.getValueAt(selectedRow, COL_QUANTITY);
        double price = (Double)  model.getValueAt(selectedRow, COL_PRICE);
        qty++;
        model.setValueAt(qty,         selectedRow, COL_QUANTITY);
        model.setValueAt(price * qty, selectedRow, COL_TOTAL);
        updateTotal();
    }

    /** Decreases the quantity of the currently selected row by 1 (minimum 1). */
    public void decreaseQuantity() {
        int selectedRow = am.tableCreateOrder.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(am, "Please select an item.");
            return;
        }
        DefaultTableModel model = (DefaultTableModel) am.tableCreateOrder.getModel();
        int qty = (Integer) model.getValueAt(selectedRow, COL_QUANTITY);
        if (qty <= 1) {
            JOptionPane.showMessageDialog(am,
                "Quantity cannot be less than 1. Use Remove to delete the item.");
            return;
        }
        double price = (Double) model.getValueAt(selectedRow, COL_PRICE);
        qty--;
        model.setValueAt(qty,         selectedRow, COL_QUANTITY);
        model.setValueAt(price * qty, selectedRow, COL_TOTAL);
        updateTotal();
    }

    /** Removes the currently selected row from the cart. */
    public void removeItem() {
        int selectedRow = am.tableCreateOrder.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(am, "Please select an item to remove.");
            return;
        }
        DefaultTableModel model = (DefaultTableModel) am.tableCreateOrder.getModel();
        model.removeRow(selectedRow);
        cartItemIds.remove(selectedRow);
        cartItemTypes.remove(selectedRow);
        updateTotal();
    }

    /** Clears all items from the cart and resets the form. */
    public void clearOrder() {
        DefaultTableModel model = (DefaultTableModel) am.tableCreateOrder.getModel();
        model.setRowCount(0);
        cartItemIds.clear();
        cartItemTypes.clear();
        cartTotal      = 0;
        cartFinalTotal = 0;
        am.totalAmountLabel.setText("");
        am.discountCreateOrderComboBox.setSelectedIndex(0);
        am.paymentCOTextField.setText("");
    }

    /** Recalculates and displays the discounted total. */
    public void applyDiscount() {
        double discountRate   = getDiscountRate();
        double discountAmount = cartTotal * discountRate;
        cartFinalTotal = cartTotal - discountAmount;
        am.totalAmountLabel.setText(String.format("\u20b1%.2f", cartFinalTotal));
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private void updateTotal() {
        DefaultTableModel model = (DefaultTableModel) am.tableCreateOrder.getModel();
        cartTotal = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            cartTotal += (Double) model.getValueAt(i, COL_TOTAL);
        }
        applyDiscount();
    }

    // -----------------------------------------------------------------------
    // Order confirmation
    // -----------------------------------------------------------------------

    /**
     * Validates payment and prompts the user to confirm before persisting the
     * order to the database.
     */
    public void confirmOrder() {
        DefaultTableModel model = (DefaultTableModel) am.tableCreateOrder.getModel();
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(am, "Please add items to the order first.");
            return;
        }

        String paymentStr = am.paymentCOTextField.getText().trim();
        if (paymentStr.isEmpty()) {
            JOptionPane.showMessageDialog(am, "Please enter a payment amount.");
            return;
        }
        double payment;
        try {
            payment = Double.parseDouble(paymentStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(am, "Invalid payment amount.");
            return;
        }
        if (payment < cartFinalTotal) {
            JOptionPane.showMessageDialog(am,
                String.format("Payment is insufficient.\nTotal due: \u20b1%.2f", cartFinalTotal));
            return;
        }

        double change = payment - cartFinalTotal;
        int confirm = JOptionPane.showConfirmDialog(am,
            String.format("Total:    \u20b1%.2f%nPayment:  \u20b1%.2f%nChange:   \u20b1%.2f%n%nConfirm order?",
                cartFinalTotal, payment, change),
            "Confirm Order", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        saveOrderToDatabase(payment, change);
    }

    /**
     * Persists the current cart as a completed order.
     *
     * <ul>
     *   <li>Menu items  → inserted into {@code order_items}</li>
     *   <li>Drinks      → inserted into {@code order_item_drinks}, linked to the
     *                      most recently saved menu order-item (if any)</li>
     *   <li>Add-ons     → inserted into {@code order_item_addons}, linked to the
     *                      most recently saved menu order-item (if any)</li>
     * </ul>
     * The {@code orders.total} always reflects every cart item (including drinks
     * and add-ons) so that the payment calculation is accurate.
     */
    private void saveOrderToDatabase(double payment, double change) {
        double discountRate   = getDiscountRate();
        double discountAmount = cartTotal * discountRate;
        DefaultTableModel model = (DefaultTableModel) am.tableCreateOrder.getModel();

        try {
            conn.setAutoCommit(false);

            // --- Determine next order number ---
            int orderNumber;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(MAX(orderNumber), ?) + 1 AS nextNum FROM orders");
                 ) {
                ps.setInt(1, STARTING_ORDER_NUMBER);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    orderNumber = rs.getInt("nextNum");
                }
            }

            // --- Insert order header ---
            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO orders(orderNumber, total, discountRate, discountAmount, finalTotal, orderStatus) "
                    + "VALUES(?,?,?,?,?,'completed')",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, orderNumber);
                ps.setDouble(2, cartTotal);
                ps.setDouble(3, discountRate);
                ps.setDouble(4, discountAmount);
                ps.setDouble(5, cartFinalTotal);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    orderId = rs.getInt(1);
                }
            }

            // --- Insert order items ---
            int lastMenuOrderItemId = -1;

            for (int i = 0; i < model.getRowCount(); i++) {
                String itemType  = cartItemTypes.get(i);
                int    itemId    = cartItemIds.get(i);
                int    qty       = (Integer) model.getValueAt(i, COL_QUANTITY);
                double price     = (Double)  model.getValueAt(i, COL_PRICE);
                double itemTotal = (Double)  model.getValueAt(i, COL_TOTAL);

                if ("menu".equals(itemType)) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO order_items(orderId, menuItemId, quantity, pricePerItem, itemTotal) "
                            + "VALUES(?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1, orderId);
                        ps.setInt(2, itemId);
                        ps.setInt(3, qty);
                        ps.setDouble(4, price);
                        ps.setDouble(5, itemTotal);
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            rs.next();
                            lastMenuOrderItemId = rs.getInt(1);
                        }
                    }
                } else if ("drink".equals(itemType) && lastMenuOrderItemId != -1) {
                    // Associate drink with the most recent menu order-item
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO order_item_drinks(orderItemId, drinkId) VALUES(?,?)")) {
                        ps.setInt(1, lastMenuOrderItemId);
                        ps.setInt(2, itemId);
                        ps.executeUpdate();
                    }
                } else if ("addon".equals(itemType) && lastMenuOrderItemId != -1) {
                    // Associate add-on with the most recent menu order-item
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO order_item_addons(orderItemId, addonId) VALUES(?,?)")) {
                        ps.setInt(1, lastMenuOrderItemId);
                        ps.setInt(2, itemId);
                        ps.executeUpdate();
                    }
                }
                // Drinks/add-ons without a preceding menu item are tracked in the
                // order total but not persisted to order_item_* tables.
            }

            // --- Insert payment ---
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO payments(orderId, amountReceived, changeAmount) VALUES(?,?,?)")) {
                ps.setInt(1, orderId);
                ps.setDouble(2, payment);
                ps.setDouble(3, change);
                ps.executeUpdate();
            }

            conn.commit();
            JOptionPane.showMessageDialog(am,
                String.format("Order #%d created!\nChange: \u20b1%.2f", orderNumber, change));
            clearOrder();

        } catch (SQLException ex) {
            try { conn.rollback(); } catch (SQLException ignored) { }
            JOptionPane.showMessageDialog(am, "Error creating order: " + ex.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) { }
        }
    }
}
