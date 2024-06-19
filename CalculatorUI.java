import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalculatorUI extends Frame implements ActionListener {
    private static ArrayList<String> history = new ArrayList<>();
    private static boolean startNewCalculation = true;
    private static TextField display;
    private static boolean isRunning = false; //
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final Object lock = new Object();

    public CalculatorUI() {
        setTitle("Simple Calculator");
        setLayout(new BorderLayout());

        display = new TextField();
        display.setEditable(false);

        add(display, BorderLayout.NORTH);

        Panel buttonPanel = new Panel(new GridLayout(5, 5));

        String[] buttonLabels = {
                "7", "8", "9", "/",
                "4", "5", "6", "*",
                "1", "2", "3", "-",
                "C", "0", "Hs", "+",
                "X", "Del", ".", "="
        };

        for (String label : buttonLabels) {
            Button button = new Button(label);
            button.addActionListener(this);
            buttonPanel.add(button);
        }
        add(buttonPanel, BorderLayout.CENTER);
        setSize(300, 200);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if ("C".equals(command)) {
            display.setText("");
        } else if ("=".equals(command)) {
            String input = display.getText();
            if(!isRunning) {
                isRunning = true;
                executorService.submit(() -> {
                    try {
                        double result = calculator(input);
                        display.setText(String.valueOf(result));
                        startNewCalculation = true;
                    } catch (Exception ex) {
                        display.setText("Error");
                    }finally {
                        isRunning = false;
                    }
                });
            }
        } else {
            if (startNewCalculation) {
                display.setText(command);
                startNewCalculation = false;
            } else {
                display.setText(display.getText() + command);
            }
        }
        if ("Hs".equals(command)) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        display.setText(displayHistory());
                        startNewCalculation = true;
                        lock.notifyAll();
                    }
                }
            });
        } else if ("Del".equals(command)) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        clearHistory();
                        startNewCalculation = true;
                        lock.notifyAll();
                    }
                }
            });
        }
    }

    public static double calculator(String input) {
        String[] tokens = input.split("(?<=[-+*/])|(?=[-+*/])");
        double result = 0.0;
        double operand = 0.0;
        String operator = "+";

        for (String token : tokens) {
            if (isNumeric(token)) {
                operand = Double.parseDouble(token);
                switch (operator) {
                    case "+":
                        result += operand;
                        break;
                    case "-":
                        result -= operand;
                        break;
                    case "*":
                        result *= operand;
                        break;
                    case "/":
                        if (operand != 0) {
                            result /= operand;
                        } else if(operand == 0) {
                            display.setText("Error");
                            System.out.println("Lỗi không chia cho 0!");
                            return Double.parseDouble("");
                        }
                        break;
                }
            } else {
                operator = token;
            }
        }

        startNewCalculation = true;
        System.out.println("Kết quả: " + input + " = " + result);
        String resultString = Double.toString(result);
        history.add(input + " = " + resultString);
        isRunning = false;
        synchronized (lock) {
            lock.notifyAll();
        }
        return result;
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String displayHistory() {
        System.out.println("Lịch sử phép tính");
        StringBuilder historyString = new StringBuilder();
        if (history.isEmpty()) {
            display.setText("EMPTY");
            System.out.println("EMPTY");

        } else {
            for (String entry : history) {
                System.out.println(entry);
                display.setText(entry);
                historyString.append(entry).append("\n");
            }
        }
        return historyString.toString();
    }

    public static void clearHistory() {
        if (history.isEmpty()) {
            display.setText("NO DATA");
            System.out.println("không có dữ liệu");
        } else {
            history.clear();
            display.setText("delete success");
            System.out.println("Xóa dữ liệu thành công");
        }
    }

    public static void main(String[] args) {
        new CalculatorUI();
    }
}
