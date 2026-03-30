/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package KioskMain;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class MenuPanel extends JPanel {

    private final int itemId;
    private final String itemName;
    private final double price;
    private final String imagePath;
   

    public MenuPanel(int itemId, String itemName, double price, String imagePath) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.price = price;
        this.imagePath = imagePath;

        createMenuCard();
    }

   private void createMenuCard() {
    Color mouseExit = Color.WHITE;
    Color mouseEnter = new Color(255, 240, 240);

    setBackground(mouseExit);
    setPreferredSize(new Dimension(100, 140));
    setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(171, 48, 61), 2),
            new EmptyBorder(4, 4, 4, 4)
    ));
    setLayout(new BorderLayout(4, 4));

    JLabel lblImage = new JLabel();
    lblImage.setHorizontalAlignment(SwingConstants.CENTER);
    lblImage.setPreferredSize(new Dimension(90, 90));
    lblImage.setOpaque(true);
    lblImage.setBackground(mouseExit);
    ImageSize.setFitImage(lblImage, imagePath, 90, 90);

    JLabel lblName = new JLabel(
            "<html><div style='text-align:center; width:80px;'>"
            + escapeHtml(itemName)
            + "</div></html>"
    );
    lblName.setHorizontalAlignment(SwingConstants.CENTER);
    lblName.setAlignmentX(Component.CENTER_ALIGNMENT);
    lblName.setFont(new Font("Arial Narrow", Font.BOLD, 12));
    lblName.setForeground(new Color(50, 50, 50));
    lblName.setOpaque(true);
    lblName.setBackground(mouseExit);

    JLabel lblPrice = new JLabel(String.format("%.2f Php", price));
    lblPrice.setHorizontalAlignment(SwingConstants.CENTER);
    lblPrice.setAlignmentX(Component.CENTER_ALIGNMENT);
    lblPrice.setFont(new Font("Arial Narrow", Font.BOLD, 12));
    lblPrice.setForeground(new Color(171, 48, 61));
    lblPrice.setOpaque(true);
    lblPrice.setBackground(mouseExit);

    JPanel bottomPanel = new JPanel();
    bottomPanel.setBackground(mouseExit);
    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
    bottomPanel.add(lblName);
    bottomPanel.add(Box.createVerticalStrut(3));
    bottomPanel.add(lblPrice);

    MouseAdapter hoverEffect = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            setCardBackground(mouseEnter, lblImage, lblName, lblPrice, bottomPanel);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setCardBackground(mouseExit, lblImage, lblName, lblPrice, bottomPanel);
        }
    };
        addMouseListener(hoverEffect);
        lblImage.addMouseListener(hoverEffect);
        lblName.addMouseListener(hoverEffect);
        lblPrice.addMouseListener(hoverEffect);
        bottomPanel.addMouseListener(hoverEffect);

        add(lblImage, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
   
   private void setCardBackground(Color color, Component... components) {
        setBackground(color);
        for (Component comp : components) {
            comp.setBackground(color);
        }
        repaint();
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public int getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public double getPrice() {
        return price;
    }

    public String getImagePath() {
        return imagePath;
    }
}
