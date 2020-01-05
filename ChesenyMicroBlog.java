import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JOptionPane;

public class ChesenyMicroBlog extends JFrame {

  private ChesenyMicroBlogDownloader chesenyMicroBlogDownloader = null;

  public ChesenyMicroBlog() {
    setTitle("ChesenyMicroBlog");
    setSize(500, 400);
    setLayout(new GridLayout(4, 2, 3, 3));
    JLabel accountTypeJLabel = new JLabel("account type:");
    accountTypeJLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    JComboBox<String> accountTypeJComboBox = new JComboBox<String>();
    accountTypeJComboBox.setEditable(false);
    accountTypeJComboBox.setEnabled(true);
    accountTypeJComboBox.addItem("nickname");
    accountTypeJComboBox.addItem("username");
    accountTypeJComboBox.addItem("uid");
    add(accountTypeJLabel);
    add(accountTypeJComboBox);
    final JLabel jLabel = new JLabel("nickname");
    jLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    JTextField jTextField = new JTextField("");
    add(jLabel);
    add(jTextField);
    accountTypeJComboBox.addItemListener​(new ItemListener() {

      public void itemStateChanged​(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          jLabel.setText((String) e.getItem());
        }
      }

    });
    JLabel filePathJLabel = new JLabel("file path:");
    filePathJLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    final JButton filePathJButton = new JButton("select:");
    filePathJButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jFileChooser.showDialog(new JLabel(), "select");
        if (jFileChooser.getSelectedFile() != null) {
          filePathJButton.setText(jFileChooser.getSelectedFile().getAbsolutePath());
        }
      }

    });
    add(filePathJLabel);
    add(filePathJButton);
    JButton startJButton = new JButton("start");
    startJButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        final String type = (String) accountTypeJComboBox.getSelectedItem();
        final String name = jTextField.getText();
        String filePath = filePathJButton.getText();
        if (type == null || "".equals(type.trim()) || name == null || "".equals(name.trim()) || filePath == null || "".equals(filePath.trim()) || "select".equals(filePath)) {
          JOptionPane.showMessageDialog(null, "please select file path to save picture", "error", JOptionPane.ERROR_MESSAGE);
          return;
        }
        new Thread(new Runnable() {

          public void run() {
            try {
              startJButton.setEnabled(false);
              if (chesenyMicroBlogDownloader == null) {
                chesenyMicroBlogDownloader = new ChesenyMicroBlogDownloader();
              }
              chesenyMicroBlogDownloader.start(chesenyMicroBlogDownloader, type, name, filePath);
            } catch (Exception e) {
              e.printStackTrace();
            } finally {
              startJButton.setEnabled(true);
            }
          }

        }).start();
      }

    });
    add(startJButton);
  }

  public static void main(String[] args) {
    ChesenyMicroBlog chesenyMicroBlog = new ChesenyMicroBlog();
    chesenyMicroBlog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    chesenyMicroBlog.setLocationRelativeTo(null);
    chesenyMicroBlog.setVisible(true);
  }

}
