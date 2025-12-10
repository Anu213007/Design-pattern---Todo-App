package com.todo;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


public class Todo {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}

/* =========================
 * ===== DOMAIN: TASK ======
 * ========================= */

interface Task {
    String getTitle();
    void setTitle(String title);

    boolean isDone();
    void setDone(boolean done);
}

class BasicTask implements Task {
    private String title;
    private boolean done;

    public BasicTask(String title) {
        this.title = title;
        this.done = false;
    }

    public BasicTask(String title, boolean done) {
        this.title = title;
        this.done = done;
    }

    public BasicTask copy() {
        return new BasicTask(title, done);
    }

    @Override
    public String getTitle() { return title; }

    @Override
    public void setTitle(String title) { this.title = title; }

    @Override
    public boolean isDone() { return done; }

    @Override
    public void setDone(boolean done) { this.done = done; }

    // JList will use a custom renderer, so this is only for debugging
    @Override
    public String toString() {
        return "Task{" + title + ", done=" + done + "}";
    }
}

/* =========================
 * ===== ADAPTER PATTERN ===
 * ========================= */

class LegacyNote {
    private final String text;

    public LegacyNote(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}

class LegacyNoteAdapter implements Task {
    private final LegacyNote legacyNote;
    private boolean done;

    public LegacyNoteAdapter(LegacyNote legacyNote) {
        this.legacyNote = legacyNote;
        this.done = false;
    }

    @Override
    public String getTitle() {
        return legacyNote.getText();
    }

    @Override
    public void setTitle(String title) {
        // kept simple: we don't modify the underlying LegacyNote
    }

    @Override
    public boolean isDone() { return done; }

    @Override
    public void setDone(boolean done) { this.done = done; }
}

/* =========================
 * ===== OBSERVER PATTERN ==
 * ========================= */

interface TaskObserver {
    void onTaskListChanged();
}

interface TaskSubject {
    void addObserver(TaskObserver observer);
    void removeObserver(TaskObserver observer);
    void notifyObservers();
}

/* =========================
 * ===== STRATEGY PATTERN ==
 * ========================= */

interface TaskOrderStrategy {
    List<Task> order(List<Task> tasks);
}

class NormalOrderStrategy implements TaskOrderStrategy {
    @Override
    public List<Task> order(List<Task> tasks) {
        return new ArrayList<>(tasks);
    }
}

/** Strategy: completed tasks at bottom. */
class CompletedLastOrderStrategy implements TaskOrderStrategy {
    @Override
    public List<Task> order(List<Task> tasks) {
        List<Task> unfinished = new ArrayList<>();
        List<Task> finished = new ArrayList<>();
        for (Task t : tasks) {
            if (t.isDone()) finished.add(t);
            else unfinished.add(t);
        }
        List<Task> result = new ArrayList<>(unfinished);
        result.addAll(finished);
        return result;
    }
}

/* =========================
 * ===== ITERATOR PATTERN ==
 * ========================= */

interface TaskIterator {
    boolean hasNext();
    Task next();
}

class SimpleTaskIterator implements TaskIterator {
    private final List<Task> tasks;
    private int index = 0;

    public SimpleTaskIterator(List<Task> tasks) {
        this.tasks = tasks;
    }

    @Override
    public boolean hasNext() {
        return index < tasks.size();
    }

    @Override
    public Task next() {
        return tasks.get(index++);
    }
}

/* =========================
 * ===== COMPOSITE PATTERN =
 * ========================= */

interface JournalComponent {
    String getName();
    void print(String indent);
}

class TaskLeaf implements JournalComponent {
    private final Task task;

    public TaskLeaf(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    @Override
    public String getName() {
        return task.getTitle();
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "- Task: " + task.getTitle());
    }
}

class CategoryComposite implements JournalComponent {
    private final String name;
    private final List<JournalComponent> children = new ArrayList<>();

    public CategoryComposite(String name) {
        this.name = name;
    }

    public void addChild(JournalComponent component) {
        children.add(component);
    }

    public void clearChildren() {
        children.clear();
    }

    public List<JournalComponent> getChildren() {
        return children;
    }

    @Override
    public String getName() { return name; }

    @Override
    public void print(String indent) {
        System.out.println(indent + "* Category: " + name);
        for (JournalComponent child : children) {
            child.print(indent + "  ");
        }
    }
}

/* =========================
 * ===== MEMENTO PATTERN ===
 * ========================= */

class TaskManagerMemento {
    private final List<Task> tasksSnapshot;

    public TaskManagerMemento(List<Task> tasksSnapshot) {
        this.tasksSnapshot = tasksSnapshot;
    }

    public List<Task> getTasksSnapshot() {
        return tasksSnapshot;
    }
}

/* =========================
 * ===== SINGLETON + CORE ==
 * ========================= */

class TaskManager implements TaskSubject {
    private static final TaskManager INSTANCE = new TaskManager();

    public static TaskManager getInstance() {
        return INSTANCE;
    }

    private final List<Task> tasks = new ArrayList<>();
    private final List<TaskObserver> observers = new ArrayList<>();
    private TaskOrderStrategy orderStrategy = new NormalOrderStrategy();
    private CategoryComposite root;

    private TaskManager() {
        rebuildCompositeFromTasks();
    }

    @Override
    public void addObserver(TaskObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(TaskObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (TaskObserver observer : observers) {
            observer.onTaskListChanged();
        }
    }

    public synchronized void addTask(Task task) {
        tasks.add(task);
        rebuildCompositeFromTasks();
        notifyObservers();
    }

    public synchronized void deleteTask(Task task) {
        tasks.remove(task);
        rebuildCompositeFromTasks();
        notifyObservers();
    }

    public synchronized void toggleDone(Task task) {
        task.setDone(!task.isDone());
        rebuildCompositeFromTasks();
        notifyObservers();
    }

    public synchronized void setOrderStrategy(TaskOrderStrategy strategy) {
        this.orderStrategy = strategy;
        notifyObservers();
    }

    public synchronized List<Task> getTasksOrdered() {
        return orderStrategy.order(tasks);
    }

    public synchronized TaskIterator iteratorForAll() {
        return new SimpleTaskIterator(getTasksOrdered());
    }

    public synchronized TaskManagerMemento createMemento() {
        List<Task> copy = new ArrayList<>();
        for (Task t : tasks) {
            if (t instanceof BasicTask) {
                copy.add(((BasicTask) t).copy());
            } else {
                copy.add(new BasicTask(t.getTitle(), t.isDone()));
            }
        }
        return new TaskManagerMemento(copy);
    }

    public synchronized void restore(TaskManagerMemento memento) {
        tasks.clear();
        tasks.addAll(memento.getTasksSnapshot());
        rebuildCompositeFromTasks();
        notifyObservers();
    }

    private void rebuildCompositeFromTasks() {
        root = new CategoryComposite("All Tasks");
        for (Task t : tasks) {
            root.addChild(new TaskLeaf(t));
        }
    }
}

/* =========================
 * ===== FACTORY PATTERN ===
 * ========================= */

class TaskFactory {
    public static Task createBasicTask(String title) {
        return new BasicTask(title);
    }

    public static Task createLegacyTask(LegacyNote note) {
        return new LegacyNoteAdapter(note);
    }
}

/* =========================
 * ===== COMMAND PATTERN ===
 * ========================= */

interface Command {
    void execute();
}

class CommandManager {
    private final Deque<TaskManagerMemento> undoStack = new ArrayDeque<>();
    private final Deque<TaskManagerMemento> redoStack = new ArrayDeque<>();
    private final TaskManager manager;

    public CommandManager(TaskManager manager) {
        this.manager = manager;
    }

    public void executeCommand(Command command) {
        undoStack.push(manager.createMemento());
        redoStack.clear();
        command.execute();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(manager.createMemento());
            TaskManagerMemento m = undoStack.pop();
            manager.restore(m);
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(manager.createMemento());
            TaskManagerMemento m = redoStack.pop();
            manager.restore(m);
        }
    }
}

class AddTaskCommand implements Command {
    private final TaskManager manager;
    private final String title;

    public AddTaskCommand(TaskManager manager, String title) {
        this.manager = manager;
        this.title = title;
    }

    @Override
    public void execute() {
        Task task = TaskFactory.createBasicTask(title);
        manager.addTask(task);
    }
}

class DeleteTaskCommand implements Command {
    private final TaskManager manager;
    private final Task task;

    public DeleteTaskCommand(TaskManager manager, Task task) {
        this.manager = manager;
        this.task = task;
    }

    @Override
    public void execute() {
        manager.deleteTask(task);
    }
}

class ToggleTaskDoneCommand implements Command {
    private final TaskManager manager;
    private final Task task;

    public ToggleTaskDoneCommand(TaskManager manager, Task task) {
        this.manager = manager;
        this.task = task;
    }

    @Override
    public void execute() {
        manager.toggleDone(task);
    }
}

/* =========================
 * ===== FACADE PATTERN ====
 * ========================= */

enum ViewMode {
    NORMAL,
    COMPLETED_LAST
}

class TaskAppFacade {
    private final TaskManager manager;
    private final CommandManager commandManager;

    public TaskAppFacade() {
        this.manager = TaskManager.getInstance();
        this.commandManager = new CommandManager(manager);
    }

    public void addTask(String title) {
        if (title == null || title.isBlank()) return;
        Command c = new AddTaskCommand(manager, title.trim());
        commandManager.executeCommand(c);
    }

    public void deleteTaskAtIndex(int index) {
        List<Task> list = manager.getTasksOrdered();
        if (index < 0 || index >= list.size()) return;
        Task t = list.get(index);
        Command c = new DeleteTaskCommand(manager, t);
        commandManager.executeCommand(c);
    }

    public void toggleTaskDoneAtIndex(int index) {
        List<Task> list = manager.getTasksOrdered();
        if (index < 0 || index >= list.size()) return;
        Task t = list.get(index);
        Command c = new ToggleTaskDoneCommand(manager, t);
        commandManager.executeCommand(c);
    }

    public void undo() {
        commandManager.undo();
    }

    public void redo() {
        commandManager.redo();
    }

    public void setViewMode(ViewMode mode) {
        if (mode == ViewMode.NORMAL) {
            manager.setOrderStrategy(new NormalOrderStrategy());
        } else {
            manager.setOrderStrategy(new CompletedLastOrderStrategy());
        }
    }

    public TaskIterator getIteratorForAll() {
        return manager.iteratorForAll();
    }
}

/* =========================
 * ===== SWING UI LAYER ====
 * ========================= */

/** Custom renderer to show checkboxes in the list. */
class TaskCellRenderer extends JCheckBox implements ListCellRenderer<Task> {

    private final Color paper;
    private final Color ink;
    private final Color selectionBg;

    public TaskCellRenderer(Color paper, Color ink, Color selectionBg) {
        this.paper = paper;
        this.ink = ink;
        this.selectionBg = selectionBg;
        setOpaque(true);
        setFont(new Font("Serif", Font.PLAIN, 14));
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends Task> list, Task value, int index,
            boolean isSelected, boolean cellHasFocus) {

        setText(value.getTitle());
        setSelected(value.isDone());
        setForeground(ink);

        if (isSelected) {
            setBackground(selectionBg);
        } else {
            setBackground(paper);
        }
        return this;
    }
}

class MainFrame extends JFrame implements TaskObserver {

    private final Color PAPER         = new Color(249, 243, 232); // main background
    private final Color PAPER_DARK    = new Color(240, 231, 216); // header / toolbar strip
    private final Color PAPER_LIGHT   = new Color(253, 249, 241); // inner "page"
    private final Color INK           = new Color(76, 63, 56);    // warm brown ink

    private final Color ACCENT_BUTTON    = new Color(234, 221, 205); // subtle beige buttons
    private final Color ACCENT_HIGHLIGHT = new Color(219, 229, 213); // very soft greenish selection

    private final TaskAppFacade facade = new TaskAppFacade();
    private final TaskManager manager = TaskManager.getInstance();

    private DefaultListModel<Task> listModel;
    private JList<Task> taskList;
    private JComboBox<String> viewModeCombo;
    private JLabel viewModeLabel;

    public MainFrame() {
        setTitle("Todo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 480);
        setLocationRelativeTo(null);

        getContentPane().setBackground(PAPER);
        setLayout(new BorderLayout(8, 8));

        add(createTitlePanel(), BorderLayout.NORTH);
        add(createTaskListPanel(), BorderLayout.CENTER);
        add(createToolbar(), BorderLayout.SOUTH);

        manager.addObserver(this);
        refreshTaskList();
    }

    private Border createPanelBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 196, 176)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );
    }

    private JComponent createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAPER_DARK); // gentle band at the top
        panel.setBorder(BorderFactory.createEmptyBorder(10, 16, 8, 16));

        JLabel title = new JLabel("Todo 🌿");
        title.setFont(new Font("Serif", Font.BOLD, 24));
        title.setForeground(INK);

        JLabel subtitle = new JLabel("a tiny todo for your tasks");
        subtitle.setFont(new Font("Serif", Font.ITALIC, 12));
        subtitle.setForeground(new Color(130, 115, 100)); // slightly softer ink

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.add(title, BorderLayout.NORTH);
        textPanel.add(subtitle, BorderLayout.SOUTH);

        panel.add(textPanel, BorderLayout.WEST);
        return panel;
    }

    private JComponent createTaskListPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(PAPER);
        outer.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAPER_LIGHT); // subtle "journal page"
        panel.setBorder(createPanelBorder());

        JLabel label = new JLabel("Today’s Tasks");
        label.setFont(new Font("Serif", Font.BOLD, 18));
        label.setForeground(INK);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        panel.add(label, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        taskList = new JList<>(listModel);
        taskList.setBackground(PAPER_LIGHT);
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.setCellRenderer(new TaskCellRenderer(PAPER_LIGHT, INK, ACCENT_HIGHLIGHT));

        JScrollPane scrollPane = new JScrollPane(taskList);
        scrollPane.getViewport().setBackground(PAPER_LIGHT);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        outer.add(panel, BorderLayout.CENTER);
        return outer;
    }

    private void styleButton(JButton b) {
        b.setFocusPainted(false);
        b.setBackground(ACCENT_BUTTON);
        b.setForeground(INK);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createLineBorder(new Color(188, 174, 156), 1));
        b.setFont(new Font("Serif", Font.PLAIN, 13));
        b.setPreferredSize(new Dimension(130, 34));   // BIG toolbar buttons
    }

    private void styleDialogButton(JButton b) {
        b.setFocusPainted(false);
        b.setBackground(ACCENT_BUTTON);
        b.setForeground(INK);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createLineBorder(new Color(188, 174, 156), 1));
        b.setFont(new Font("Serif", Font.PLAIN, 13));
        b.setPreferredSize(new Dimension(90, 28));    // smaller, for OK/Cancel
    }

    private JComponent createToolbar() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(PAPER);
        outer.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));

        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 4));
        panel.setBackground(PAPER);

        JPanel row1 = new JPanel();
        row1.setBackground(PAPER_DARK); // same calm band as header

        JButton add = new JButton("Add Task");
        JButton del = new JButton("Delete Task");
        JButton toggle = new JButton("Toggle Completed");
        JButton undo = new JButton("Undo");
        JButton redo = new JButton("Redo");

        JButton[] row1Buttons = {add, del, toggle, undo, redo};
        for (JButton b : row1Buttons) {
            styleButton(b);
            row1.add(b);
        }

        add.addActionListener(e -> onAddTask());
        del.addActionListener(e -> onDeleteTask());
        toggle.addActionListener(e -> onToggleTask());
        undo.addActionListener(e -> facade.undo());
        redo.addActionListener(e -> facade.redo());

        JPanel row2 = new JPanel();
        row2.setBackground(PAPER_DARK); // same color → unified, not noisy
        row2.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        viewModeLabel = new JLabel("View: Normal");
        viewModeLabel.setForeground(INK);
        viewModeLabel.setFont(new Font("Serif", Font.PLAIN, 13));

        viewModeCombo = new JComboBox<>(new String[]{"Normal", "Completed Last"});
        viewModeCombo.setBackground(Color.WHITE);
        viewModeCombo.setForeground(INK);
        viewModeCombo.setFont(new Font("Serif", Font.PLAIN, 13));

        viewModeCombo.addActionListener(e -> {
            int idx = viewModeCombo.getSelectedIndex();
            ViewMode mode = (idx == 0 ? ViewMode.NORMAL : ViewMode.COMPLETED_LAST);
            facade.setViewMode(mode);
            viewModeLabel.setText("View: " + (mode == ViewMode.NORMAL ? "Normal" : "Completed Last"));
        });

        row2.add(new JLabel("Order: "));
        row2.add(viewModeCombo);
        row2.add(Box.createHorizontalStrut(12));
        row2.add(viewModeLabel);

        panel.add(row1);
        panel.add(row2);

        outer.add(panel, BorderLayout.CENTER);
        return outer;
    }

    /** Themed "Add Task" dialog. */
    private String showAddTaskDialog() {
        JDialog dialog = new JDialog(this, "Add Task", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(PAPER);
        dialog.setLayout(new BorderLayout(8, 8));

        dialog.setSize(460, 210);
        dialog.setLocationRelativeTo(this);

        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.setBackground(PAPER);
        center.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));

        JLabel label = new JLabel("Add a new task:");
        label.setFont(new Font("Serif", Font.PLAIN, 15));
        label.setForeground(INK);

        JTextArea area = new JTextArea(3, 30);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Serif", Font.PLAIN, 14));
        area.setBackground(Color.WHITE);
        area.setForeground(INK);
        area.setBorder(BorderFactory.createLineBorder(new Color(200, 185, 160)));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(PAPER);

        center.add(label, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        dialog.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setBackground(PAPER);
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 16, 14, 16));

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");

        styleDialogButton(ok);
        styleDialogButton(cancel);

        bottom.add(ok);
        bottom.add(cancel);
        dialog.add(bottom, BorderLayout.SOUTH);

        final String[] result = {null};

        ok.addActionListener(e -> {
            String text = area.getText();
            if (text != null) {
                text = text.trim().replace("\n", " ");
            }
            if (text != null && !text.isBlank()) {
                result[0] = text;
            }
            dialog.dispose();
        });

        cancel.addActionListener(e -> {
            result[0] = null;
            dialog.dispose();
        });

        dialog.getRootPane().setDefaultButton(ok);
        dialog.setVisible(true);

        return result[0];
    }

    private void onAddTask() {
        String title = showAddTaskDialog();
        if (title != null) {
            facade.addTask(title);
        }
    }

    private void onDeleteTask() {
        int index = taskList.getSelectedIndex();
        if (index >= 0) {
            facade.deleteTaskAtIndex(index);
        }
    }

    private void onToggleTask() {
        int index = taskList.getSelectedIndex();
        if (index >= 0) {
            facade.toggleTaskDoneAtIndex(index);
        }
    }

    @Override
    public void onTaskListChanged() {
        SwingUtilities.invokeLater(this::refreshTaskList);
    }

    private void refreshTaskList() {
        listModel.clear();
        TaskIterator iterator = facade.getIteratorForAll();
        while (iterator.hasNext()) {
            listModel.addElement(iterator.next());
        }
    }
}
