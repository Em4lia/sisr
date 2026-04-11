package org.labs;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.gui.TreeViewer;
import java.io.File;
import java.util.Arrays;
import javax.swing.*;
import java.util.List;
import java.awt.*;

public class Main extends JFrame {
    private final JTextArea inputTextBox;
    private final JTextPane outputTextPane;

    public Main() {
        setTitle("Java IDE - Lab 4");
        setSize(800, 600);
        setMinimumSize(new Dimension(600, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        inputTextBox = new JTextArea(
                """
                class Test {
                    int main() {
                        int x = 10;
                        int y = 5;
                        int z = x + y * 2;
                        if (z > 15) {
                            z = z - 10;
                        } else {
                            z = z + 10;
                        }
                        int count = 0;
                        while (count < 3) {
                            z = z + 1;
                            count = count + 1;
                        }
                        return z;
                    }
                }
                """
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
        outputScroll.setBorder(BorderFactory.createTitledBorder("Результат виконання"));

        JButton analyzeButton = new JButton("Скомпілювати");
        analyzeButton.addActionListener(e -> {
            new Thread(() -> startAnalysis()).start();
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputScroll, outputScroll);
        splitPane.setDividerLocation(300);

        add(splitPane, BorderLayout.CENTER);
        add(analyzeButton, BorderLayout.SOUTH);
    }

    private void appendColoredText(JTextPane pane, String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            javax.swing.text.StyledDocument doc = pane.getStyledDocument();
            javax.swing.text.Style style = pane.addStyle("ColorStyle", null);
            javax.swing.text.StyleConstants.setForeground(style, color);
            try {
                doc.insertString(doc.getLength(), text, style);
            } catch (javax.swing.text.BadLocationException e) {
                System.err.println("Помилка додавання тексту: " + e.getMessage());
            }
        });
    }

    private void startAnalysis() {
        String code = inputTextBox.getText();
        SwingUtilities.invokeLater(() -> outputTextPane.setText(""));
        try {
            var charStream = CharStreams.fromString(code);
            javaLParser parser = getParser(charStream);

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

            showTreeWindow(parser, tree);

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
                    return;
                }

                if (!warnings.isEmpty()) {
                    appendColoredText(outputTextPane, "\n⚠️ ПОПЕРЕДЖЕННЯ ("  + warnings.size() +  "):\n", Color.WHITE);
                    for (String warn : warnings) appendColoredText(outputTextPane, warn + "\n", Color.ORANGE);
                }

                appendColoredText(outputTextPane, "\nПочаток генерації коду...\n", Color.YELLOW);
                CodeGenerator codeGen = new CodeGenerator();
                codeGen.visit(tree);
                appendColoredText(outputTextPane, "Код згенеровано у файл output.s\n", Color.GREEN);

                try {
                    appendColoredText(outputTextPane, "Запуск GCC для створення виконуваного файлу...\n", Color.YELLOW);

                    String gccCmd = "gcc";
                    File gccFile = new File("D:\\EDC\\8sem\\Створення інтегрованих середовищ розробки\\gcc\\mingw64\\bin\\gcc.exe");
                    if (gccFile.exists()) {
                        gccCmd = gccFile.getAbsolutePath();
                    }

                    ProcessBuilder pb = new ProcessBuilder(gccCmd, "output.s", "-o", "program.exe");

                    String workingDir = System.getProperty("user.dir");
                    pb.directory(new File(workingDir)); 
                    Process process = pb.start();

                    java.util.Scanner s = new java.util.Scanner(process.getErrorStream()).useDelimiter("\\A");
                    String gccOutput = s.hasNext() ? s.next() : "";

                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        appendColoredText(outputTextPane, "✅ Успішно створено program.exe!\n", Color.GREEN);

                        appendColoredText(outputTextPane, "Запуск program.exe...\n", Color.YELLOW);
                        ProcessBuilder runPb = new ProcessBuilder(workingDir + File.separator + "program.exe");
                        runPb.directory(new File(workingDir));
                        Process runProcess = runPb.start();
                        int runExitCode = runProcess.waitFor();
                        appendColoredText(outputTextPane, "Програма завершилась з кодом: " + runExitCode + "\n", Color.CYAN);

                    } else {
                        appendColoredText(outputTextPane, "❌ Помилка компіляції GCC (код " + exitCode + ").\n", Color.RED);
                        if (!gccOutput.isEmpty()) {
                            appendColoredText(outputTextPane, gccOutput + "\n", Color.RED);
                        }
                    }
                } catch (Exception ex) {
                    appendColoredText(outputTextPane, "Помилка виклику GCC: " + ex.getMessage() + "\n", Color.RED);
                }
            }

        } catch (Exception ex) {
            appendColoredText(outputTextPane, "Критична помилка: " + ex.getMessage() + "\n", Color.RED);
        }
    }

    private static javaLParser getParser(org.antlr.v4.runtime.CodePointCharStream charStream) {
        javaLLexer lexer = new javaLLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new javaLParser(tokens);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
    private void showTreeWindow(javaLParser parser, ParseTree tree) {
        JFrame treeFrame = new JFrame("Abstract Syntax Tree");
        TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()),
                tree);
        viewer.setScale(1.5);
        JScrollPane scrollPane = new JScrollPane(viewer);
        treeFrame.add(scrollPane);
        treeFrame.setSize(600, 400);
        treeFrame.setVisible(true);
    }
}