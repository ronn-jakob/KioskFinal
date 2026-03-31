/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Admin;

import java.awt.*;
import javax.swing.*;


public class MenuCard extends JPanel {
    

    int itemId;

    /**
     * Creates a MenuCard that adds the item to the given {@link CreateOrder}
     * cart when the "Add" button is clicked.
     *
     * @param itemId   database primary key of the item
     * @param name     display name
     * @param price    unit price
     * @param imagePath path to the item image
     * @param co       the {@link CreateOrder} instance managing the cart
     * @param itemType one of {@code "menu"}, {@code "drink"}, or {@code "addon"}
     */
    public MenuCard(int itemId, String name, double price, String imagePath,
                    CreateOrder co, String itemType) {

        this.itemId = itemId;

        setPreferredSize(new Dimension(150, 180));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        setLayout(new BorderLayout());

        // Image
        ImageIcon icon = new ImageIcon(imagePath);
        Image img = icon.getImage().getScaledInstance(120, 80, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel(new ImageIcon(img));
        imageLabel.setHorizontalAlignment(JLabel.CENTER);

        // Name
        JLabel nameLabel = new JLabel(name, JLabel.CENTER);
        nameLabel.setFont(new Font("Poppins", Font.BOLD, 12));

        // Price
        JLabel priceLabel = new JLabel("₱" + price, JLabel.CENTER);

        // Button
        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            if (co != null) {
                co.addItemToTable(itemId, name, price, itemType);
            }
        });

        // Bottom panel
        JPanel bottomPanel = new JPanel(new GridLayout(3,1));
        bottomPanel.add(nameLabel);
        bottomPanel.add(priceLabel);
        bottomPanel.add(addButton);

        add(imageLabel, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.CENTER);
    }

    /**
     * Backward-compatible constructor (no-op Add button).
     */
    public MenuCard(int itemId, String name, double price, String imagePath) {
        this(itemId, name, price, imagePath, null, null);
    }
}
