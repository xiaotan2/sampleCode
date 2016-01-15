import java.awt.Component;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/*
 * LogCellRenderer renders cells in LogTable to highlight search text
 */
public class LogCellRenderer implements TableCellRenderer {

    private MarkFindTableModel mMarkFindModel;
    private LogTableCellRenderer mTableRenderer;

    /*
     * Constructor
     */
    public LogCellRenderer(MarkFindTableModel markFindModel) {
        mMarkFindModel = markFindModel;
        mTableRenderer = new LogTableCellRenderer();
    }

    /*
     * rendering the table column cell based on RuleList
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        mTableRenderer = (LogTableCellRenderer) mTableRenderer.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);
        if (value != null && column != LogTableModel.COLUMN_TIME) {
            boolean[] allTextFound = new boolean[mMarkFindModel.getRowCount()];
            // construct allTextfound array, allTextFound is true if all texts
            // in the same row matches search text
            for (int i = 0; i < mMarkFindModel.getRowCount(); i++) {
                MarkFindRule rule = mMarkFindModel.getRule(i);
                boolean threadIDMatch =
                        isMatch(row, LogTableModel.COLUMN_THREADID, rule, table.getModel(),
                                MarkFindRule.SEARCH_TYPE_EQUALS);
                boolean processIDMatch =
                        isMatch(row, LogTableModel.COLUMN_PROCESSID, rule, table.getModel(),
                                MarkFindRule.SEARCH_TYPE_EQUALS);
                boolean priorityMatch =
                        isMatch(row, LogTableModel.COLUMN_PRIORITY, rule, table.getModel(),
                                MarkFindRule.SEARCH_TYPE_EQUALS);
                boolean tagMatch =
                        isMatch(row, LogTableModel.COLUMN_TAG, rule, table.getModel(),
                                MarkFindRule.SEARCH_TYPE_EQUALS);
                boolean messageMatch =
                        isMatch(row, LogTableModel.COLUMN_MESSAGE, rule, table.getModel(),
                                rule.searchType);

                allTextFound[i] = threadIDMatch && processIDMatch && priorityMatch && tagMatch
                        && messageMatch && !allRuleTextEmpty(rule);
            }
            // After constructing allTextFound array, we know what rule applies
            // to this cell.
            // Then we highlight them
            Vector<LogHighlighter> highlighterList = new Vector<LogHighlighter>();
            for (int i = 0; i < mMarkFindModel.getRowCount(); i++) {
                MarkFindRule markRule = mMarkFindModel.getRule(i);
                if (!allTextFound[i] || !markRule.apply) {
                    continue;
                }
                // construct start and end index
                String stringCompare = value.toString();
                String searchText = getTextFromColumn(markRule, column);
                int searchType = 0;
                if (column == LogTableModel.COLUMN_MESSAGE) {
                    searchType = markRule.searchType;
                } else {
                    searchType = MarkFindRule.SEARCH_TYPE_EQUALS;
                }
                if (searchText.isEmpty()) {
                    continue;
                }
                switch (searchType) {
                case MarkFindRule.SEARCH_TYPE_EQUALS:
                    if (stringCompare.equals(searchText)) {
                        int start = stringCompare.indexOf(searchText);
                        highlighterList.add(new LogHighlighter(start, start + searchText.length(),
                                markRule));
                    }
                    break;
                case MarkFindRule.SEARCH_TYPE_CONTAINS:
                    int start = stringCompare.indexOf(searchText);
                    while (start != -1) {
                        highlighterList.add(new LogHighlighter(start, start + searchText.length(),
                                markRule));
                        start = stringCompare.indexOf(searchText, start + 1);
                    }
                    break;
                case MarkFindRule.SEARCH_TYPE_STARTSWITH:
                    int startIndex = stringCompare.indexOf(searchText);
                    if (startIndex == 0) {
                        highlighterList.add(new LogHighlighter(startIndex, startIndex
                                + searchText.length(), markRule));
                    }
                    break;
                case MarkFindRule.SEARCH_TYPE_REGEX:
                    Pattern pat = Pattern.compile(searchText);
                    Matcher mat = pat.matcher(stringCompare);
                    while (mat.find()) {
                        highlighterList.add(new LogHighlighter(mat.start(), mat.end(), markRule));
                    }
                }
            }
            // set up renderer start, end index and colors using highlighterList
            mTableRenderer.highlightRegions(highlighterList);
        } else {
            mTableRenderer.highlightRegions(new Vector<LogHighlighter>());
        }
        return mTableRenderer;
    }

    /*
     * check if two text match by their compare type
     */
    public static boolean isMatch(int row, int column, MarkFindRule rule,
            TableModel model, int compareType) {

        String compareText = getTextFromColumn(rule, column);
        if (compareText.isEmpty()) {
            return true;
        }
        String searchText = (String) model.getValueAt(row, column);
        if (compareType == MarkFindRule.SEARCH_TYPE_EQUALS && searchText.equals(compareText)) {
            return true;
        } else if (compareType == MarkFindRule.SEARCH_TYPE_CONTAINS
                && searchText.contains(compareText)) {
            return true;
        } else if (compareType == MarkFindRule.SEARCH_TYPE_STARTSWITH
                && searchText.indexOf(compareText) == 0) {
            return true;
        } else if (compareType == MarkFindRule.SEARCH_TYPE_REGEX) {
            Pattern pat = Pattern.compile(compareText);
            Matcher mat = pat.matcher(searchText);
            return mat.find();
        } else {
            return false;
        }
    }

    private static String getTextFromColumn(MarkFindRule markRule, int column) {
        switch (column) {
        case LogTableModel.COLUMN_THREADID:
            return markRule.threadIDFindText;
        case LogTableModel.COLUMN_PROCESSID:
            return markRule.processIDFindText;
        case LogTableModel.COLUMN_PRIORITY:
            return markRule.priorityFindText;
        case LogTableModel.COLUMN_TAG:
            return markRule.tagFindText;
        case LogTableModel.COLUMN_MESSAGE:
            return markRule.messageFindText;
        }
        return null;
    }

    public static boolean allRuleTextEmpty(MarkFindRule markRule) {
        return markRule.threadIDFindText.isEmpty() && markRule.priorityFindText.isEmpty()
                && markRule.processIDFindText.isEmpty() && markRule.tagFindText.isEmpty()
                && markRule.messageFindText.isEmpty();
    }
}
