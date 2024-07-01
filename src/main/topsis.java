/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package main;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 *
 * @author LENOVO
 */
public class topsis extends javax.swing.JFrame {

    private double[] weights;
    private double[][] decisionMatrix;
    private double[][] normalizedMatrix;
    private double[][] weightedNormalizedMatrix;
    private double[] positiveIdealSolution;
    private double[] negativeIdealSolution;
    private double[] distanceToPositiveIdeal;
    private double[] distanceToNegativeIdeal;
    private double[] preferenceValues;
    private String[] criteriaTypes;

    private String[] alternatives;
    private String[] criteria;

    public topsis() {
        initComponents();
        loadData();
        setLocationRelativeTo(null);
    }

    private void loadData() {
        try (Connection conn = Koneksi.connect(); Statement stmt = conn.createStatement()) {

            // Load criteria
            ResultSet rs = stmt.executeQuery("SELECT * FROM kriteria");
            List<String> criteriaList = new ArrayList<>();
            List<Double> weightsList = new ArrayList<>();
            List<String> criteriaTypesList = new ArrayList<>();  // New list for criteria types
            while (rs.next()) {
                criteriaList.add(rs.getString("nama_kriteria"));
                weightsList.add(rs.getDouble("bobot"));
                criteriaTypesList.add(rs.getString("tipe"));  // Add type
            }
            criteria = criteriaList.toArray(new String[0]);
            weights = weightsList.stream().mapToDouble(Double::doubleValue).toArray();
            criteriaTypes = criteriaTypesList.toArray(new String[0]);  // Convert list to array

            // Load alternatives
            rs = stmt.executeQuery("SELECT * FROM alternatif");
            List<String> alternativesList = new ArrayList<>();
            while (rs.next()) {
                alternativesList.add(rs.getString("nama_alternatif"));
            }
            alternatives = alternativesList.toArray(new String[0]);

            // Load decision matrix
            rs = stmt.executeQuery("SELECT * FROM nilai ORDER BY id_alternatif, id_kriteria");
            decisionMatrix = new double[alternatives.length][criteria.length];
            int currentAltIndex = -1;
            int currentCritIndex = 0;
            int lastAltId = -1;
            while (rs.next()) {
                int altId = rs.getInt("id_alternatif");
                if (altId != lastAltId) {
                    currentAltIndex++;
                    currentCritIndex = 0;
                    lastAltId = altId;
                }
                decisionMatrix[currentAltIndex][currentCritIndex++] = rs.getDouble("nilai");
            }

            displayMatrix(decisionMatrix, tblPembagi, criteria, alternatives);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void performTOPSIS() {
        normalizeDecisionMatrix();
        calculateWeightedNormalizedMatrix();
        determineIdealSolutions();
        calculateDistances();
        calculatePreferenceValues();
        displayRanking();
    }

    private void normalizeDecisionMatrix() {
        normalizedMatrix = new double[decisionMatrix.length][decisionMatrix[0].length];
        double[] sumOfSquares = new double[decisionMatrix[0].length];

        for (int j = 0; j < decisionMatrix[0].length; j++) {
            double sum = 0;
            for (double[] row : decisionMatrix) {
                sum += Math.pow(row[j], 2);
            }
            sumOfSquares[j] = Math.sqrt(sum);
        }

        for (int i = 0; i < decisionMatrix.length; i++) {
            for (int j = 0; j < decisionMatrix[i].length; j++) {
                normalizedMatrix[i][j] = decisionMatrix[i][j] / sumOfSquares[j];
            }
        }

        displayMatrix(normalizedMatrix, TblNormalisasi, criteria, alternatives);
    }

    private void calculateWeightedNormalizedMatrix() {
        weightedNormalizedMatrix = new double[normalizedMatrix.length][normalizedMatrix[0].length];

        for (int i = 0; i < normalizedMatrix.length; i++) {
            for (int j = 0; j < normalizedMatrix[i].length; j++) {
                weightedNormalizedMatrix[i][j] = normalizedMatrix[i][j] * weights[j];
            }
        }

        displayMatrix(weightedNormalizedMatrix, TblNormalisasiterbobot, criteria, alternatives);
    }

    private void determineIdealSolutions() {
        positiveIdealSolution = new double[weightedNormalizedMatrix[0].length];
        negativeIdealSolution = new double[weightedNormalizedMatrix[0].length];

        for (int j = 0; j < weightedNormalizedMatrix[0].length; j++) {
            double max = Double.MIN_VALUE;
            double min = Double.MAX_VALUE;
            for (double[] row : weightedNormalizedMatrix) {
                if (criteriaTypes[j].equals("benefit")) {
                    if (row[j] > max) {
                        max = row[j];
                    }
                    if (row[j] < min) {
                        min = row[j];
                    }
                } else {  // cost criteria
                    if (row[j] < min) {
                        min = row[j];
                    }
                    if (row[j] > max) {
                        max = row[j];
                    }
                }
            }
            if (criteriaTypes[j].equals("benefit")) {
                positiveIdealSolution[j] = max;
                negativeIdealSolution[j] = min;
            } else {  // cost criteria
                positiveIdealSolution[j] = min;
                negativeIdealSolution[j] = max;
            }
        }

        displayIdealSolutions();
    }

    private void calculateDistances() {
        distanceToPositiveIdeal = new double[weightedNormalizedMatrix.length];
        distanceToNegativeIdeal = new double[weightedNormalizedMatrix.length];

        for (int i = 0; i < weightedNormalizedMatrix.length; i++) {
            double sumPos = 0;
            double sumNeg = 0;
            for (int j = 0; j < weightedNormalizedMatrix[i].length; j++) {
                sumPos += Math.pow(weightedNormalizedMatrix[i][j] - positiveIdealSolution[j], 2);
                sumNeg += Math.pow(weightedNormalizedMatrix[i][j] - negativeIdealSolution[j], 2);
            }
            distanceToPositiveIdeal[i] = Math.sqrt(sumPos);
            distanceToNegativeIdeal[i] = Math.sqrt(sumNeg);
        }

        displayDistances();
    }

    private void calculatePreferenceValues() {
        preferenceValues = new double[weightedNormalizedMatrix.length];

        for (int i = 0; i < weightedNormalizedMatrix.length; i++) {
            preferenceValues[i] = distanceToNegativeIdeal[i] / (distanceToPositiveIdeal[i] + distanceToNegativeIdeal[i]);
        }

        displayPreferenceValues();
    }

    private void displayMatrix(double[][] matrix, JTable table, String[] columnNames, String[] rowNames) {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Alternatif");
        for (String columnName : columnNames) {
            model.addColumn(columnName);
        }

        for (int i = 0; i < matrix.length; i++) {
            Object[] row = new Object[matrix[i].length + 1];
            row[0] = rowNames[i];
            for (int j = 0; j < matrix[i].length; j++) {
                row[j + 1] = matrix[i][j];
            }
            model.addRow(row);
        }

        table.setModel(model);
    }


    private void displayIdealSolutions() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Alternatif");
        for (String criterion : criteria) {
            model.addColumn(criterion);
        }

        Object[] positiveRow = new Object[positiveIdealSolution.length + 1];
        positiveRow[0] = "Ideal Positif";
        for (int i = 0; i < positiveIdealSolution.length; i++) {
            positiveRow[i + 1] = positiveIdealSolution[i];
        }

        Object[] negativeRow = new Object[negativeIdealSolution.length + 1];
        negativeRow[0] = "Ideal Negatif";
        for (int i = 0; i < negativeIdealSolution.length; i++) {
            negativeRow[i + 1] = negativeIdealSolution[i];
        }

        model.addRow(positiveRow);
        model.addRow(negativeRow);

        TblSolusiideal.setModel(model);
    }

    private void displayDistances() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Alternatif");
        model.addColumn("D+");
        model.addColumn("D-");

        for (int i = 0; i < alternatives.length; i++) {
            Object[] row = new Object[3];
            row[0] = alternatives[i];
            row[1] = distanceToPositiveIdeal[i];
            row[2] = distanceToNegativeIdeal[i];
            model.addRow(row);
        }

        tblJarakalternatif.setModel(model);
    }

    private void displayPreferenceValues() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Alternatif");
        model.addColumn("Nilai Preferensi");

        for (int i = 0; i < alternatives.length; i++) {
            Object[] row = new Object[2];
            row[0] = alternatives[i];
            row[1] = preferenceValues[i];
            model.addRow(row);
        }

        TblNilaiV.setModel(model);
    }

    private void displayRanking() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Alternatif");
        model.addColumn("Nilai Preferensi");
        model.addColumn("Ranking");

        Double[] preferenceValuesCopy = Arrays.stream(preferenceValues).boxed().toArray(Double[]::new);
        Arrays.sort(preferenceValuesCopy, (a, b) -> Double.compare(b, a)); // Sort in descending order

        for (int i = 0; i < alternatives.length; i++) {
            for (int j = 0; j < alternatives.length; j++) {
                if (preferenceValuesCopy[i].equals(preferenceValues[j])) {
                    Object[] row = new Object[3];
                    row[0] = alternatives[j];
                    row[1] = preferenceValues[j];
                    row[2] = i + 1; // Ranking starts from 1
                    model.addRow(row);
                    break;
                }
            }
        }
        tblranking.setModel(model);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane8 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        BtnHitung = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblPembagi = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        TblNormalisasi = new javax.swing.JTable();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        TblNormalisasiterbobot = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        TblSolusiideal = new javax.swing.JTable();
        jScrollPane6 = new javax.swing.JScrollPane();
        tblJarakalternatif = new javax.swing.JTable();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        TblNilaiV = new javax.swing.JTable();
        btnBack = new javax.swing.JToggleButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblranking = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(153, 255, 255));
        jPanel1.setPreferredSize(new java.awt.Dimension(1558, 876));

        BtnHitung.setText("Hitung Topsis");
        BtnHitung.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnHitungActionPerformed(evt);
            }
        });

        tblPembagi.setModel(new javax.swing.table.DefaultTableModel(
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
        tblPembagi.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblPembagiMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tblPembagi);

        jLabel1.setText("Decision Matriks");

        jLabel3.setText("Normalisasi");

        TblNormalisasi.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane3.setViewportView(TblNormalisasi);

        jLabel4.setText("Normalisasi Terbobot");

        TblNormalisasiterbobot.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane4.setViewportView(TblNormalisasiterbobot);

        jLabel5.setText("A- dan A+");

        TblSolusiideal.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane5.setViewportView(TblSolusiideal);

        tblJarakalternatif.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane6.setViewportView(tblJarakalternatif);

        jLabel6.setText("D- dan D+");

        jLabel7.setText("V (Nilai Preferensi)");

        TblNilaiV.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane7.setViewportView(TblNilaiV);

        btnBack.setText("Back");
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });

        tblranking.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane2.setViewportView(tblranking);

        jLabel2.setText("Hasil Perangkingan");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 572, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 427, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 427, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6))
                        .addGap(83, 83, 83)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addGap(357, 357, 357))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(485, 485, 485)
                                .addComponent(jLabel5))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(BtnHitung)
                                .addGap(18, 18, 18)
                                .addComponent(btnBack))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel4)
                                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
                                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                                .addGap(137, 137, 137)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel7)
                                    .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 427, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addContainerGap(541, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(BtnHitung)
                    .addComponent(btnBack))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
                            .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap(90, Short.MAX_VALUE))
        );

        jScrollPane8.setViewportView(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 1558, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 876, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void BtnHitungActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnHitungActionPerformed
        // TODO add your handling code here:
        performTOPSIS();

    }//GEN-LAST:event_BtnHitungActionPerformed

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        // TODO add your handling code here:
        this.setVisible(false);
        new main().setVisible(true);
    }//GEN-LAST:event_btnBackActionPerformed

    private void tblPembagiMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblPembagiMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_tblPembagiMouseClicked

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
            java.util.logging.Logger.getLogger(topsis.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(topsis.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(topsis.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(topsis.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new topsis().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BtnHitung;
    private javax.swing.JTable TblNilaiV;
    private javax.swing.JTable TblNormalisasi;
    private javax.swing.JTable TblNormalisasiterbobot;
    private javax.swing.JTable TblSolusiideal;
    private javax.swing.JToggleButton btnBack;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JTable tblJarakalternatif;
    private javax.swing.JTable tblPembagi;
    private javax.swing.JTable tblranking;
    // End of variables declaration//GEN-END:variables
}
