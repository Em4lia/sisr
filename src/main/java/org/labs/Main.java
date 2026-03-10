package org.labs;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.gui.TreeViewer;
import java.util.Arrays;

import javax.swing.*;
import java.util.List;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main extends JFrame {
    private JTextArea inputTextBox;
    private JTextPane outputTextPane;

    public Main() {
        setTitle("Java IDE - Lab 3");
        setSize(800, 600);
        setMinimumSize(new Dimension(600, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        inputTextBox = new JTextArea(
                "class SemanticTest {\n" +
                        "    int globalVar;\n\n" +
                        "    int calculate(int a, double b) {\n" +
                        "        return a + 10;\n" +
                        "    }\n\n" +
                        "    void process() {\n" +
                        "        // повторне оголошення (помилка)\n" +
                        "        int x = 5;\n" +
                        "        double x = 10.5;\n\n" +
                        "        // невикористана змінна (попередження)\n" +
                        "        int unusedVar = 100;\n\n" +
                        "        // використання неоголошеної змінної (помилка)\n" +
                        "        y = 20;\n\n" +
                        "        // несумісні типи при присвоєнні (помилка)\n" +
                        "        int z = 5.5;\n\n" +
                        "        // індекс масиву має бути int (помилка)\n" +
                        "        int[] arr;\n" +
                        "        arr[5.5] = 10;\n\n" +
                        "        // умова if не є boolean (помилка)\n" +
                        "        if (z + 5) {\n" +
                        "            z = 1;\n" +
                        "        }\n\n" +
                        "        // недосяжний код (попередження)\n" +
                        "        if (false) {\n" +
                        "            z = 2;\n" +
                        "        }\n\n" +
                        "        // break поза циклом (помилка)\n" +
                        "        break;\n\n" +
                        "        // нескінченний цикл (попередження)\n" +
                        "        while (true) {\n" +
                        "            z = z + 1;\n" +
                        "        }\n\n" +
                        "        // помилки виклику функцій (помилка)\n" +
                        "        int res1 = calculate(10);           // бракує аргументу\n" +
                        "        int res2 = calculate(10, true);     // неправильний тип (boolean замість double)\n\n" +
                        "        // операції з несумісними типами (помилка)\n" +
                        "        boolean flag = true;\n" +
                        "        int badMath = 10 + flag;\n" +
                        "    }\n\n" +
                        "    // невірний тип повернення (помилка)\n" +
                        "    double getNumber() {\n" +
                        "        return true;\n" +
                        "    }\n" +
                        "}"
        );

        inputTextBox.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane inputScroll = new JScrollPane(inputTextBox);
        inputScroll.setBorder(BorderFactory.createTitledBorder("Вхідний код"));

        TextLineNumber tln = new TextLineNumber(inputTextBox);
        inputScroll.setRowHeaderView(tln);

        outputTextPane = new JTextPane();
        outputTextPane.setEditable(false);
        outputTextPane.setBackground(new Color(0, 0, 0));
        outputTextPane.setFont(new Font("Monospace", Font.PLAIN, 14));
        JScrollPane outputScroll = new JScrollPane(outputTextPane);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Результат лексичного аналізу"));

        JButton analyzeButton = new JButton("Виконати аналіз");
        analyzeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startAnalysis();
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputScroll, outputScroll);
        splitPane.setDividerLocation(300);

        add(splitPane, BorderLayout.CENTER);
        add(analyzeButton, BorderLayout.SOUTH);
    }

    private void appendColoredText(JTextPane pane, String text, Color color) {
        javax.swing.text.StyledDocument doc = pane.getStyledDocument();
        javax.swing.text.Style style = pane.addStyle("ColorStyle", null);
        javax.swing.text.StyleConstants.setForeground(style, color);
        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (javax.swing.text.BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void startAnalysis() {
        String code = inputTextBox.getText();
        outputTextPane.setText("");
        try {
            var charStream = CharStreams.fromString(code);
            javaLLexer lexer = new javaLLexer(charStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            javaLParser parser = new javaLParser(tokens);

            parser.removeErrorListeners();
            parser.addErrorListener(new org.antlr.v4.runtime.BaseErrorListener() {
                @Override
                public void syntaxError(org.antlr.v4.runtime.Recognizer<?, ?> recognizer,
                                        Object offendingSymbol, int line, int charPositionInLine,
                                        String msg, org.antlr.v4.runtime.RecognitionException e) {
                    appendColoredText(outputTextPane, String.format(">>> СИНТАКСИЧНА ПОМИЛКА (Рядок %d:%d): %s\n", line, charPositionInLine, msg), Color.RED);
                }
            });

            ParseTree tree = parser.program();

            if (parser.getNumberOfSyntaxErrors() == 0) {
                appendColoredText(outputTextPane, "Синтаксичний аналіз успішно завершено!\n", Color.WHITE);

                SemanticAnalyzer analyzer = new SemanticAnalyzer();
                analyzer.visit(tree);
                analyzer.getSymbolTable().exitScope();

                List<String> errors = analyzer.getErrors();
                List<String> warnings = analyzer.getWarnings();

                if (errors.isEmpty()) {
                    appendColoredText(outputTextPane, "\n✅ Семантичних помилок не виявлено.\n", Color.GREEN);
                } else {
                    appendColoredText(outputTextPane, "\n❌ ЗНАЙДЕНО СЕМАНТИЧНІ ПОМИЛКИ (" + errors.size() + "):\n", Color.WHITE);
                    for (String err : errors) appendColoredText(outputTextPane, err + "\n", Color.RED);
                }

                if (!warnings.isEmpty()) {
                    appendColoredText(outputTextPane, "\n⚠️ ПОПЕРЕДЖЕННЯ ("  + warnings.size() +  "):\n", Color.WHITE);
                    for (String warn : warnings) appendColoredText(outputTextPane, warn + "\n", Color.ORANGE);
                }
                appendColoredText(outputTextPane, "\nДерево побудовано. Відкриття вікна візуалізації...\n", Color.WHITE);
                showTreeWindow(parser, tree);
            }

        } catch (Exception ex) {
            appendColoredText(outputTextPane, "Критична помилка: " + ex.getMessage(), Color.RED);
        }
    }

    private void showTreeWindow(javaLParser parser, ParseTree tree) {
        JFrame treeFrame = new JFrame("Abstract Syntax Tree");
        TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
        viewer.setScale(1.5); // Масштаб
        JScrollPane scrollPane = new JScrollPane(viewer);
        treeFrame.add(scrollPane);
        treeFrame.setSize(600, 400);
        treeFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}