/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package main;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
/**
 *
 * @author LENOVO
 */
public class alternatif extends javax.swing.JFrame {

    private ArrayList<JTextField> nilaiFields;
    private ArrayList<String> kriteriaList;
    private DefaultTableModel tableModel;
    private int selectedAlternatifId = -1;

    public alternatif() {
        kriteriaList = getKriteriaList();
        nilaiFields = new ArrayList<>();
        
        // Initialize components
        initComponents();
        
        createDynamicFields();
        setupTable();
        loadAlternatif();
        setLocationRelativeTo(null);
    }

    private void createDynamicFields() {
        panelFields.setLayout(new GridLayout(0, 2));
        panelFields.add(jLabel2);
        panelFields.add(txtNama);

        for (String kriteria : kriteriaList) {
            JLabel label = new JLabel(kriteria + ":");
            JTextField textField = new JTextField(20);
            nilaiFields.add(textField);

            panelFields.add(label);
            panelFields.add(textField);
        }}
        private void setupTable() {
            tableModel = new DefaultTableModel(new Object[]{"ID", "Nama Alternatif"}, 0);
            for (String kriteria : kriteriaList) {
                tableModel.addColumn(kriteria);
            }
            jTable1.setModel(tableModel);
            jScrollPane1.setViewportView(jTable1);
        }

        private ArrayList<String> getKriteriaList() {
            ArrayList<String> kriteria = new ArrayList<>();
            try (Connection conn = Koneksi.connect()) {
                String query = "SELECT nama_kriteria FROM kriteria";
                try (PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        kriteria.add(rs.getString("nama_kriteria"));
                    }
                }
            } catch (SQLException e) {
                showError("Koneksi error: " + e.getMessage());
            }
            return kriteria;
        }

    private void addAlternatif() {
        String namaAlternatif = txtNama.getText();
        ArrayList<String> nilaiKriteria = new ArrayList<>();

        for (JTextField field : nilaiFields) {
            nilaiKriteria.add(field.getText());
        }

        if (namaAlternatif.isEmpty() || nilaiKriteria.contains("")) {
            showError("Please fill all fields.");
            return;
        }

        try (Connection conn = Koneksi.connect()) {
            String query = "INSERT INTO alternatif (nama_alternatif) VALUES (?)";
            try (PreparedStatement stmt = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, namaAlternatif);
                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int idAlternatif = generatedKeys.getInt(1);
                        System.out.println("ID Alternatif yang baru ditambahkan: " + idAlternatif); // Log ID Alternatif
                        insertNilaiKriteria(conn, idAlternatif, nilaiKriteria);
                    }
                }
            }

            showSuccess("Data alternatif berhasil ditambahkan.");
            loadAlternatif();
        } catch (SQLException e) {
            showError("Koneksi error: " + e.getMessage());
            e.printStackTrace(); // Menampilkan detail kesalahan
        }
    }

    private void insertNilaiKriteria(Connection conn, int idAlternatif, ArrayList<String> nilaiKriteria) throws SQLException {
        String query = "INSERT INTO nilai (id_alternatif, id_kriteria, nilai) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < kriteriaList.size(); i++) {
                stmt.setInt(1, idAlternatif);
                stmt.setInt(2, i + 1);
                stmt.setString(3, nilaiKriteria.get(i));
                stmt.executeUpdate();
            }
        }
    }

    private void deleteAlternatif() {
        if (selectedAlternatifId == -1) {
            showError("Silakan pilih baris untuk dihapus.");
            return;
        }

        try (Connection conn = Koneksi.connect()) {
            // Hapus entri dari tabel nilai terlebih dahulu
            String query = "DELETE FROM nilai WHERE id_alternatif = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, selectedAlternatifId);
                stmt.executeUpdate();
            }

            // Kemudian hapus entri dari tabel alternatif
            query = "DELETE FROM alternatif WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, selectedAlternatifId);
                stmt.executeUpdate();
            }

            showSuccess("Data alternatif berhasil dihapus.");
            loadAlternatif();
        } catch (SQLException e) {
            showError("Koneksi error: " + e.getMessage());
        }
    }


    private void editAlternatif() {
        if (selectedAlternatifId == -1) {
            showError("Please select a row to edit.");
            return;
        }

        String namaAlternatif = txtNama.getText();
        ArrayList<String> nilaiKriteria = new ArrayList<>();

        for (JTextField field : nilaiFields) {
            nilaiKriteria.add(field.getText());
        }

        if (namaAlternatif.isEmpty() || nilaiKriteria.contains("")) {
            showError("Please fill all fields.");
            return;
        }

        try (Connection conn = Koneksi.connect()) {
            String query = "UPDATE alternatif SET nama_alternatif = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, namaAlternatif);
                stmt.setInt(2, selectedAlternatifId);
                stmt.executeUpdate();
            }

            updateNilaiKriteria(conn, selectedAlternatifId, nilaiKriteria);

            showSuccess("Data alternatif berhasil diubah.");
            loadAlternatif();
        } catch (SQLException e) {
            showError("Koneksi error: " + e.getMessage());
        }
}

    private void updateNilaiKriteria(Connection conn, int idAlternatif, ArrayList<String> nilaiKriteria) throws SQLException {
        String query = "UPDATE nilai SET nilai = ? WHERE id_alternatif = ? AND id_kriteria = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < kriteriaList.size(); i++) {
                stmt.setString(1, nilaiKriteria.get(i));
                stmt.setInt(2, idAlternatif);
                stmt.setInt(3, i + 1);
                stmt.executeUpdate();
            }
        }
    }


    private void loadAlternatif() {
        try (Connection conn = Koneksi.connect()) {
            String query = "SELECT a.id, a.nama_alternatif, k.nama_kriteria, n.nilai " +
                    "FROM alternatif a " +
                    "LEFT JOIN nilai n ON a.id = n.id_alternatif " +
                    "LEFT JOIN kriteria k ON n.id_kriteria = k.id " +
                    "ORDER BY a.id, k.id";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                tableModel.setRowCount(0);
                int currentId = -1;
                Object[] row = null;
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String namaAlternatif = rs.getString("nama_alternatif");
                    String kriteria = rs.getString("nama_kriteria");
                    String nilai = rs.getString("nilai");

                    if (id != currentId) {
                        if (row != null) {
                            tableModel.addRow(row);
                        }
                        row = new Object[kriteriaList.size() + 2];
                        row[0] = id;
                        row[1] = namaAlternatif;
                        currentId = id;
                    }
                    if (kriteria != null) {
                        int kriteriaIndex = kriteriaList.indexOf(kriteria);
                        row[kriteriaIndex + 2] = nilai;
                    }
                }
                if (row != null) {
                    tableModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }




    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtNama = new javax.swing.JTextField();
        btnTambah = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        panelFields = new javax.swing.JPanel();
        btnBack = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(153, 255, 255));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jLabel1.setText("Alternatif");

        jLabel2.setText("Nama Alternatif");

        btnTambah.setText("Tambah");
        btnTambah.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTambahActionPerformed(evt);
            }
        });

        btnHapus.setText("Hapus");
        btnHapus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHapusActionPerformed(evt);
            }
        });

        btnEdit.setText("Edit");
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditActionPerformed(evt);
            }
        });

        btnRefresh.setText("Refresh");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        panelFields.setBackground(new java.awt.Color(153, 255, 255));

        javax.swing.GroupLayout panelFieldsLayout = new javax.swing.GroupLayout(panelFields);
        panelFields.setLayout(panelFieldsLayout);
        panelFieldsLayout.setHorizontalGroup(
            panelFieldsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        panelFieldsLayout.setVerticalGroup(
            panelFieldsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 33, Short.MAX_VALUE)
        );

        btnBack.setText("Back");
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(39, 39, 39)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(314, 314, 314)
                        .addComponent(jLabel1))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(panelFields, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                            .addGap(8, 8, 8)
                            .addComponent(jLabel2)
                            .addGap(18, 18, 18)
                            .addComponent(txtNama, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                            .addComponent(btnTambah)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnHapus)
                            .addGap(18, 18, 18)
                            .addComponent(btnEdit)
                            .addGap(18, 18, 18)
                            .addComponent(btnRefresh)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnBack))
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 730, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(31, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtNama, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelFields, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnTambah)
                    .addComponent(btnHapus)
                    .addComponent(btnEdit)
                    .addComponent(btnRefresh)
                    .addComponent(btnBack))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked
        // TODO add your handling code here:
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow != -1) {
            selectedAlternatifId = (int) tableModel.getValueAt(selectedRow, 0);
            txtNama.setText((String) tableModel.getValueAt(selectedRow, 1));
            for (int i = 0; i < nilaiFields.size(); i++) {
                Object nilai = tableModel.getValueAt(selectedRow, i + 2);
                if (nilai != null) {
                    nilaiFields.get(i).setText(nilai.toString());
                } else {
                    nilaiFields.get(i).setText("");
                }
            }
        }
    }//GEN-LAST:event_jTable1MouseClicked

    private void btnTambahActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTambahActionPerformed
        // TODO add your handling code here:
        addAlternatif();
    }//GEN-LAST:event_btnTambahActionPerformed

    private void btnHapusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusActionPerformed
        // TODO add your handling code here:
        deleteAlternatif();
        
    }//GEN-LAST:event_btnHapusActionPerformed

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
        // TODO add your handling code here:
        editAlternatif();
    }//GEN-LAST:event_btnEditActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        // TODO add your handling code here:
        this.setVisible(false);
        new main().setVisible(true);
    }//GEN-LAST:event_btnBackActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(alternatif.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(alternatif.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(alternatif.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(alternatif.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new alternatif().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnTambah;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JPanel panelFields;
    private javax.swing.JTextField txtNama;
    // End of variables declaration//GEN-END:variables
}
