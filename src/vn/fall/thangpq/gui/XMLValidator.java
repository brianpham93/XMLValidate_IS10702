package vn.fall.thangpq.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class XMLValidator extends JFrame {

    static XMLValidator frame;
    private JPanel contentPane;
    private JTextField txtFieldFilePath;
    File xmlFile;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    frame = new XMLValidator();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public XMLValidator() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 450, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);
        

        JPanel controlPanel = new JPanel();
        contentPane.add(controlPanel, BorderLayout.NORTH);

        txtFieldFilePath = new JTextField();
        txtFieldFilePath.setText("Please select a XML file");
        controlPanel.add(txtFieldFilePath);
        txtFieldFilePath.setColumns(20);
        
        JPanel resultPanel = new JPanel();
        contentPane.add(resultPanel, BorderLayout.CENTER);
        resultPanel.setLayout(null);
        
        final JTextArea txtrXmlValidationResult = new JTextArea();
        txtrXmlValidationResult.setRows(10);
        txtrXmlValidationResult.setText("XML Validation Result");
        txtrXmlValidationResult.setBounds(0, 0, 420, 200);
        resultPanel.add(txtrXmlValidationResult);


        JButton btnOpenFile = new JButton("Open File");
        btnOpenFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                final JFileChooser fileDialog = new JFileChooser();
                int returnVal = fileDialog.showOpenDialog(frame);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fileDialog.getSelectedFile();
                    txtFieldFilePath.setText(file.getPath());
                    xmlFile = file;
                }
            }
        });
        controlPanel.add(btnOpenFile);

        JButton btnValidate = new JButton("Validate");
        btnValidate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                LineNumberReader lr = null;
                try {
                    lr = new LineNumberReader(new FileReader(xmlFile));
                    Stack stack = new Stack();
                    String result = "Stack is empty";
                    String line = lr.readLine();
                    int flag = 0;

                    readLineLoop:
                    while (line != null) {
                        if (line.trim().isEmpty() || isProlog(line)) {
                            line = lr.readLine();
                            continue;
                        }
                        while (true) { // Extract each "<...>" substring
                            String extractedTag = null;
                            if ( flag != 0){
                                result = "Extra content out of root element. Error line: " + lr.getLineNumber();
                                break readLineLoop;
                            }
                            if (containsComment(line)) {
                                String comment = line.substring(line.indexOf("<!--"), line.indexOf("-->") + 3);
                                String commentContent = comment.replaceFirst("<!--", "").replaceFirst("-->", "");
                                if (commentContent.contains("<!--")
                                        || commentContent.contains("-->")) {
                                    result = "Comment is not valid. Error line: " + lr.getLineNumber() + "\n" +  line;
                                    break readLineLoop;
                                }
                                line = line.replace(comment, "");
                                continue;

                            }
                            if (tagHasNoCloseBracket(line)) {
                                result = "There's no close bracket. Error line: " + lr.getLineNumber() + "\n" + line;
                                break readLineLoop;
                            } else if (tagHasFullBracket(line)) {
                                extractedTag = line.substring(line.indexOf("<"), line.indexOf(">") + 1);

                                if (isOpenTag(extractedTag)) {
                                    if (!isValidTagName(getTagName(extractedTag))) {
                                        result = "Invalid open tag name. Error line: " + lr.getLineNumber() + "\n" + line;
                                        break readLineLoop;
                                    }
                                    if(flag != 0){
                                    	result = "XML file has no root element. Error line: " + lr.getLineNumber();
                                    	break readLineLoop;
                                    }
                                    stack.add(extractedTag);
                                    line = line.replace(extractedTag, "");
                                    if (line.indexOf("<") != -1) {
                                        continue;
                                    } else {
                                        line = lr.readLine();
                                        continue readLineLoop;
                                    }

                                }
                                // Check if extracted tag is end tag
                                else if (isEndTag(extractedTag)) {
                                    if (!isValidTagName(getTagName(extractedTag))) {
                                        result = "Invalid end tag name. Error line: " + lr.getLineNumber() + "\n" + line;
                                        break readLineLoop;
                                    }
                                    if (stack.isEmpty()) {
                                        result = "There's no open tag for this close tag. Error line: " + lr.getLineNumber() + "\n" + line;
                                        break readLineLoop;
                                    }
                                    //End tag match open tag
                                    if (getTagName(extractedTag).equals(getTagName(stack.lastElement().toString()))) {

                                        stack.remove(stack.lastElement());
                                        if (stack.isEmpty()) flag = 1;
                                        line = line.replace(extractedTag, "");
                                        if (line.indexOf("<") != -1) {
                                            //Find next tag available on this line
                                            continue;
                                        } else {
                                            //Move to next line
                                            line = lr.readLine();
                                            continue readLineLoop;
                                        }

                                    } else { // end tag doesn't match open tag
                                        result = "End tag doesn't match open tag. Error line: " + lr.getLineNumber() + "\n" + line;
                                        break readLineLoop;
                                    }

                                }
                                // Check if extracted tag is empty tag
                                else if (isEmptyTag(extractedTag)) {
                                    if (!isValidTagName(getTagName(extractedTag))) {
                                        result = "Invalid empty tag name. Error line: " + lr.getLineNumber() + "\n" + line;
                                        break readLineLoop;
                                    }

                                    line = line.replace(extractedTag, "");
                                    if (line.indexOf("<") != -1) {
                                        continue;
                                    } else {
                                        line = lr.readLine();
                                        continue readLineLoop;
                                    }

                                }
                            } else {
                                line = lr.readLine();
                                continue readLineLoop;
                            }

                        }

                    }
                    txtrXmlValidationResult.setText(result);
                } catch (FileNotFoundException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        lr.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }

        });
        controlPanel.add(btnValidate);

        
    }

    private boolean containsComment(String line) {
        return line.contains("<!--") && line.contains("-->") && line.indexOf("-->") > line.indexOf("<!---");
    }

    private boolean tagHasFullBracket(String line) {
        return (line.indexOf("<") != -1) && (line.indexOf(">") != -1) && line.indexOf("<") < line.indexOf(">");
    }

    private boolean tagHasNoCloseBracket(String line) {
        return (line.indexOf("<") != -1) && (line.indexOf(">") == -1);
    }

    //Get the name of the tag
    private static String getTagName(String tag) {
        String tagName = "";
        if (isOpenTag(tag)) {
            tagName = tag.replace("<", "").replace(">", "");
        } else if (isEndTag(tag)) {
            tagName = tag.replace("</", "").replace(">", "");
        } else if (isEmptyTag(tag)) {
            tagName = tag.replace("<", "").replace("/>", "");
        }
        return tagName;
    }

    public static boolean isOpenTag(String tag) {
        Pattern openTagPattern = Pattern.compile("<(.*?)>");//match any char zero or more times
        Matcher m = openTagPattern.matcher(tag);
        if (m.find() && !tag.startsWith("</") && !tag.endsWith("/>")) {
            return true;
        }
        return false;
    }

    public static boolean isEndTag(String tag) {
        Pattern endTagPattern = Pattern.compile("</(.*?)>");
        Matcher m = endTagPattern.matcher(tag);
        if (m.find() && !tag.endsWith("/>")) {
            return true;
        }
        return false;
    }


    public static boolean isEmptyTag(String tag) {
        Pattern emptyTagPattern = Pattern.compile("<(.*?)/>");
        Matcher m = emptyTagPattern.matcher(tag);
        if (m.find() && !tag.startsWith("</")) {
            return true;
        }
        return false;
    }


    //Check if the name of tag is valid
    private static boolean isValidTagName(String tagName) {

        String REGEX = "^[a-z0-9_-]{1,}$";
        Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(tagName);
        if (matcher.matches()){
            return true;
        }
        return false;

//		return !tagName.toLowerCase().startsWith("xml") && Character.isLetter(tagName.charAt(0)) && (!tagName.startsWith(" ") &&
//				!tagName.endsWith(" ")) && !tagName.trim().contains(" ")
//				&& !tagName.contains("<") && !tagName.contains(">") && !tagName.contains("/");
    }

    private boolean isProlog(String line) {

        return line.startsWith("<?xml") && line.endsWith("?>");
    }
}
