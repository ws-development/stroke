/*
 * Copyright (c) 2010-2012, Isode Limited, London, England.
 * All rights reserved.
 */

/*
 * MainFrame.java
 *
 * Created on Jul 7, 2010, 10:03:01 AM
 */

package com.isode.stroke.examples.gui;

import com.isode.stroke.base.SafeByteArray;
import com.isode.stroke.client.ClientError;
import com.isode.stroke.client.ClientOptions;
import com.isode.stroke.client.CoreClient;
import com.isode.stroke.elements.Message;
import com.isode.stroke.eventloop.Event;
import com.isode.stroke.eventloop.EventLoop;
import com.isode.stroke.jid.JID;
import com.isode.stroke.network.JavaNetworkFactories;
import com.isode.stroke.signals.Slot1;
import java.awt.EventQueue;

public class StrokeGUI extends javax.swing.JFrame {

    private CoreClient client_;

    /** Creates new form MainFrame */
    public StrokeGUI() {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        loginJID_ = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        loginPassword_ = new javax.swing.JTextField();
        loginButton_ = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        sendTo_ = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        sendButton_ = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        sendText_ = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        receiveText_ = new javax.swing.JTextArea();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        xmlText_ = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new java.awt.FlowLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Login Stuff"));

        jLabel1.setText("JID");
        jPanel1.add(jLabel1);

        loginJID_.setBounds(new java.awt.Rectangle(0, 0, 150, 0));
        loginJID_.setMinimumSize(new java.awt.Dimension(150, 28));
        loginJID_.setPreferredSize(new java.awt.Dimension(250, 28));
        jPanel1.add(loginJID_);

        jLabel2.setText("Password");
        jPanel1.add(jLabel2);

        loginPassword_.setMinimumSize(new java.awt.Dimension(100, 28));
        loginPassword_.setPreferredSize(new java.awt.Dimension(150, 28));
        jPanel1.add(loginPassword_);

        loginButton_.setText("Login");
        loginButton_.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginButton_ActionPerformed(evt);
            }
        });
        jPanel1.add(loginButton_);

        getContentPane().add(jPanel1);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Send Stuff"));

        sendTo_.setMinimumSize(new java.awt.Dimension(150, 28));
        sendTo_.setPreferredSize(new java.awt.Dimension(250, 28));
        jPanel2.add(sendTo_);

        jLabel3.setText("To");
        jPanel2.add(jLabel3);

        sendButton_.setText("Send");
        sendButton_.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButton_ActionPerformed(evt);
            }
        });
        jPanel2.add(sendButton_);

        sendText_.setColumns(20);
        sendText_.setRows(5);
        sendText_.setMinimumSize(new java.awt.Dimension(150, 16));
        sendText_.setPreferredSize(new java.awt.Dimension(150, 150));
        sendText_.setSize(new java.awt.Dimension(450, 150));
        jScrollPane1.setViewportView(sendText_);

        jPanel2.add(jScrollPane1);

        getContentPane().add(jPanel2);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Receive Stuff"));

        receiveText_.setColumns(20);
        receiveText_.setEditable(false);
        receiveText_.setRows(5);
        jScrollPane2.setViewportView(receiveText_);

        jPanel3.add(jScrollPane2);

        getContentPane().add(jPanel3);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("XML"));

        xmlText_.setColumns(20);
        xmlText_.setEditable(false);
        xmlText_.setRows(5);
        xmlText_.setSize(new java.awt.Dimension(500, 300));
        jScrollPane3.setViewportView(xmlText_);

        jPanel4.add(jScrollPane3);

        getContentPane().add(jPanel4);
        jPanel4.getAccessibleContext().setAccessibleName("XML");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void loginButton_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginButton_ActionPerformed
        System.out.println("Client created from JID " + loginJID_.getText());
        EventLoop eventLoop = new EventLoop() {
            @Override
            protected void post(final Event event) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        handleEvent(event);
                    }
                });
            }
           
        };
        client_ = new CoreClient(JID.fromString(loginJID_.getText()), new SafeByteArray(loginPassword_.getText()), new JavaNetworkFactories(eventLoop));
        System.out.println("Connecting");
        try {
            client_.connect(new ClientOptions());
        } catch (Exception e) {
            //Something bad happened
            System.out.println("Exception!");
        }
        System.out.println("Connected");
        final StrokeGUI thisObject = this;
        client_.onMessageReceived.connect(new Slot1<Message>() {

            public void call(Message p1) {
                thisObject.handleMessageReceived(p1);
            }
        });
        client_.onDisconnected.connect(new Slot1<ClientError>() {

            public void call(ClientError p1) {
                thisObject.handleClientError(p1);
            }
        });
        client_.onDataRead.connect(new Slot1<SafeByteArray>() {

            public void call(SafeByteArray p1) {
                xmlText_.append(">>> " + p1 + "\n");
            }
        });
        client_.onDataWritten.connect(new Slot1<SafeByteArray>() {

            public void call(SafeByteArray p1) {
                xmlText_.append("<<< " + p1 + "\n");
            }
        });
    }//GEN-LAST:event_loginButton_ActionPerformed

    private void sendButton_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendButton_ActionPerformed
        Message message = new Message();
        message.setTo(JID.fromString(sendTo_.getText()));
        message.setBody(sendText_.getText());
        System.out.println("Message body is " + message.getBody());
        client_.sendMessage(message);

    }//GEN-LAST:event_sendButton_ActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new StrokeGUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JButton loginButton_;
    private javax.swing.JTextField loginJID_;
    private javax.swing.JTextField loginPassword_;
    private javax.swing.JTextArea receiveText_;
    private javax.swing.JButton sendButton_;
    private javax.swing.JTextArea sendText_;
    private javax.swing.JTextField sendTo_;
    private javax.swing.JTextArea xmlText_;
    // End of variables declaration//GEN-END:variables

    private void handleMessageReceived(Message message) {
        String from = message.getFrom().toString();
        String body = message.getBody();
        receiveText_.append("<" + from + "> " + body + "\n");
    }

    private void handleClientError(ClientError error) {
        receiveText_.append("Error connecting to server\n");
    }

}
