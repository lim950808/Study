package ExcelToSQLConverter;

import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

public class ExcelToSQLConverter {

    // Main conversion function
    public static String convertToSQL(String excelFormula) {
        // Remove whitespace for simplicity
        String formula = excelFormula.replaceAll("\\s+", "");
        return parseFormula(formula);
    }

    // Function to parse the Excel formula recursively
    private static String parseFormula(String formula) {
        if (formula.startsWith("IF(")) {
            return parseIfFunction(formula);
        } else if (formula.startsWith("MAX(")) {
            return parseMaxFunction(formula);
        } else if (formula.startsWith("MIN(")) {
            return parseMinFunction(formula);
        } else {
            return formula;
        }
    }

    // Function to parse IF function specifically
    private static String parseIfFunction(String formula) {
        Stack<Character> stack = new Stack<>();
        StringBuilder condition = new StringBuilder();
        StringBuilder truePart = new StringBuilder();
        StringBuilder falsePart = new StringBuilder();

        int part = 0; // 0: condition, 1: truePart, 2: falsePart
        for (int i = 3; i < formula.length() - 1; i++) { // Start after "IF("
            char ch = formula.charAt(i);

            if (ch == '(') {
                stack.push(ch);
            } else if (ch == ')') {
                stack.pop();
            }

            if (ch == ',' && stack.isEmpty()) {
                part++;
                continue;
            }

            if (part == 0) {
                condition.append(ch);
            } else if (part == 1) {
                truePart.append(ch);
            } else {
                falsePart.append(ch);
            }
        }

        String sqlCondition = parseCondition(condition.toString());
        String sqlTruePart = parseFormula(truePart.toString());
        String sqlFalsePart = parseFormula(falsePart.toString());

        return "(CASE WHEN " + sqlCondition + " THEN " + sqlTruePart + " ELSE " + sqlFalsePart + " END)";
    }

    // Function to parse MAX function specifically
    private static String parseMaxFunction(String formula) {
        String innerArgs = formula.substring(4, formula.length() - 1); // Extract arguments inside MAX()
        List<String> args = splitArguments(innerArgs);
        String joinedArgs = "TO_DECIMAL(" + String.join("), TO_DECIMAL(", args) + ")";
        return "GREATEST(" + joinedArgs + ")";
    }

    // Function to parse MIN function specifically
    private static String parseMinFunction(String formula) {
        String innerArgs = formula.substring(4, formula.length() - 1); // Extract arguments inside MIN()
        List<String> args = splitArguments(innerArgs);
        String joinedArgs = "TO_DECIMAL(" + String.join("), TO_DECIMAL(", args) + ")";
        return "LEAST(" + joinedArgs + ")";
    }

    // Function to parse simple conditions (extendable for more complex ones)
    private static String parseCondition(String condition) {
        if (condition.contains("=")) {
            String[] parts = condition.split("=");
            return parts[0] + " IS NULL";
        } else {
            return condition;
        }
    }

    // Helper function to split arguments considering nested functions
    private static List<String> splitArguments(String args) {
        List<String> result = new ArrayList<>();
        Stack<Character> stack = new Stack<>();
        StringBuilder currentArg = new StringBuilder();

        for (int i = 0; i < args.length(); i++) {
            char ch = args.charAt(i);

            if (ch == '(') {
                stack.push(ch);
            } else if (ch == ')') {
                stack.pop();
            }

            if (ch == ',' && stack.isEmpty()) {
                result.add(currentArg.toString().trim());
                currentArg = new StringBuilder();
            } else {
                currentArg.append(ch);
            }
        }
        result.add(currentArg.toString().trim());

        return result;
    }

    public static void main(String[] args) {
        String excelFormulaMAX = "MAX(C2,C1)";
        String sqlMAX = convertToSQL(excelFormulaMAX);
        System.out.println("MAX 함수 : " + sqlMAX); // Output: (GREATEST(TO_DECIMAL(C2), TO_DECIMAL(C1)))

        String excelFormula = "IF(C1=\"\",C2,C1)";
        String sql = convertToSQL(excelFormula);
        System.out.println("IF 단일 함수 : " + sql); // Output: (CASE WHEN C1 IS NULL THEN C2 ELSE C1 END)

        // For nested IF example with MAX function
        String nestedExcelFormula = "IF(C1=\"\",IF(C2=\"\",C3,C2),C2)";
        String nestedSql = convertToSQL(nestedExcelFormula);
        System.out.println("IF 다중 함수 : " + nestedSql);

        String nestedExcelFormula2 = "IF(C1=\"\",IF(C2=\"\",MAX(C3,C4),C2),C2)";
        String nestedSql2 = convertToSQL(nestedExcelFormula2);
        System.out.println("IF 다중 함수 및 기타 함수 : " + nestedSql2); // Output: (CASE WHEN C1 IS NULL THEN (CASE WHEN C2 IS NULL THEN GREATEST(TO_DECIMAL(C3), TO_DECIMAL(C4)) ELSE C2 END) ELSE C2 END)

        String nestedExcelFormula3 = "IF(C1=\"\",IF(C2=\"\",MAX(C3,C4,C5,C6),C2),MIN(C7,C8))";
        String nestedSql3 = convertToSQL(nestedExcelFormula3);
        System.out.println("IF 다중 함수 및 기타 다중 함수 : " + nestedSql3); // Output: (CASE WHEN C1 IS NULL THEN (CASE WHEN C2 IS NULL THEN GREATEST(TO_DECIMAL(C3), TO_DECIMAL(C4), TO_DECIMAL(C5), TO_DECIMAL(C6)) ELSE C2 END) ELSE LEAST(TO_DECIMAL(C7), TO_DECIMAL(C8)) END)
    }
}
