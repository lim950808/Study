package ExcelToSQLConverter;

import java.util.Stack;

public class ExcelToSQLConverterWithDFS {

    public static void main(String[] args) {
        String excelFormula = "IF(IF(C1=C2,\"Y\",\"N\")=\"N\",C3,C4)";
        String excelFormula2 = "IF(IF(C1=C2,\"Y\",\"N\")=\"N\",IF(C5<>C9,C6,C5),C4)";
        String excelFormula3 = "IF(C4=\"\",C6,C7)";
        String sqlQuery = convertToSQL(excelFormula2);
        System.out.println(sqlQuery);
    }

    public static String convertToSQL(String formula) {
        formula = formula.replace("=\"\"", " IS NULL").replace("\"", "\'");
        return DFS(formula);
    }

    private static String DFS(String formula) {
        Stack<Integer> stack = new Stack<>();
        for (int i = 0; i < formula.length(); i++) {
            if (formula.startsWith("IF(", i)) {
                stack.push(i);
            } else if (formula.charAt(i) == ')') {
                if (!stack.isEmpty()) {
                    int start = stack.pop();
                    if (stack.isEmpty()) {
                        String innerIF = formula.substring(start, i + 1);
                        String converted = convertIFToCaseWhen(innerIF);
                        formula = formula.substring(0, start) + converted + formula.substring(i + 1);
                        return DFS(formula);
                    }
                }
            }
        }
        return formula;
    }

    private static String convertIFToCaseWhen(String ifExpression) {
        String innerExpression = ifExpression.substring(3, ifExpression.length() - 1);

        int firstComma = findComma(innerExpression, 0);
        int secondComma = findComma(innerExpression, firstComma + 1);

        String condition = innerExpression.substring(0, firstComma);
        String truePart = innerExpression.substring(firstComma + 1, secondComma);
        String falsePart = innerExpression.substring(secondComma + 1);

        return "(CASE WHEN " + condition + " THEN " + truePart + " ELSE " + falsePart + " END)";
    }

    private static int findComma(String s, int start) {
        int parenthesis = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                parenthesis++;
            } else if (c == ')') {
                parenthesis--;
            } else if (c == ',' && parenthesis == 0) {
                return i;
            }
        }
        return -1;
    }
}
